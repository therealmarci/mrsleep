package at.marcelr.mrsleep.api;

import at.marcelr.mrsleep.AdminActions;
import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.NoSleepManager;
import at.marcelr.mrsleep.Settings;
import at.marcelr.mrsleep.SleepManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Plain data (maps, lists, strings, numbers) for MRadmin's web interface - no MRsleep or Bukkit types leave this
 * class. Called by {@link MRadminModule} on the server thread. MRadmin checks the permissions (MRsleep's own:
 * mrsleep.admin.gui to look, .clear / .bypass / .ban for the actions) before it asks.
 * <pre>
 *  overview                                       worlds with sleep status and blocks, totals, settings
 *  player  {uuid}                                 one player: blocks, cooldown, bypass, ban, offered durations
 *  flags                                          uuid -> marks for MRadmin's player list (sleep-blocking, sleep-banned)
 *  action  {action, admin, uuid|world, duration?, reason?}
 *          clear | release | bypass | removeBypass | resetCooldown | ban | unban  -> null or an error key
 * </pre>
 * Times are epoch ms, durations ms; a permanent ban has "permanent": true.
 */
final class WebData {

    private static final int MAX_REASON = 200;

    private WebData() {
    }

    static Object handle(MRSleep plugin, String request, Map<?, ?> args) {
        return switch (request) {
            case "overview" -> overview(plugin);
            case "player" -> {
                OfflinePlayer target = target(args.get("uuid"));
                yield target == null ? null : playerPart(plugin, target.getUniqueId());
            }
            case "flags" -> flags(plugin);
            case "action" -> action(plugin, args);
            default -> null;
        };
    }

    // --------------------------------------------------------------- overview

    private static Map<String, Object> overview(MRSleep plugin) {
        NoSleepManager noSleep = plugin.noSleep();
        Settings s = plugin.settings();
        List<Map<String, Object>> worlds = new ArrayList<>();
        int blocked = 0;
        for (World world : Bukkit.getWorlds()) {
            boolean managed = plugin.sleep().isManaged(world);
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("name", world.getName());
            w.put("environment", world.getEnvironment().name().toLowerCase(Locale.ROOT));
            w.put("managed", managed);
            if (managed) {
                SleepManager.Status status = plugin.sleep().status(world);
                w.put("night", SleepManager.isNight(world));
                w.put("thunder", world.isThundering());
                w.put("time", world.getTime() % 24_000L);
                w.put("counted", status.counted());
                w.put("exempt", status.exempt());
                w.put("sleeping", status.sleeping());
                w.put("needed", status.needed());
                List<Map<String, Object>> blockers = new ArrayList<>();
                noSleep.activeBlockers(world).forEach((uuid, nights) -> blockers.add(Map.of(
                        "uuid", uuid.toString(), "name", NoSleepManager.name(uuid), "nights", nights,
                        "pending", noSleep.isPending(world, uuid))));
                blocked += blockers.size();
                w.put("blockers", blockers);
            }
            worlds.add(w);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("worlds", worlds);
        result.put("blocked", blocked);
        result.put("bans", noSleep.activeBans().size());
        result.put("bypasses", noSleep.activeBypasses().size());
        result.put("cooldowns", noSleep.onCooldown().size());
        result.put("percentage", s.percentage());
        result.put("minimum", s.minimumSleepers());
        result.put("cooldown", s.cooldownMillis());
        result.put("maxNights", s.maxNights());
        result.put("bypassDurations", s.gui().bypassDurations());
        result.put("banDurations", s.gui().banDurations());
        return result;
    }

    // ---------------------------------------------------------------- players

    /** A player's part of MRadmin's profile page: their state and the durations the menu offers. */
    private static Map<String, Object> playerPart(MRSleep plugin, UUID id) {
        Map<String, Object> p = player(plugin, id);
        p.put("bypassDurations", plugin.settings().gui().bypassDurations());
        p.put("banDurations", plugin.settings().gui().banDurations());
        p.put("cooldownTotal", plugin.settings().cooldownMillis());
        return p;
    }

    /** Who blocks sleeping or is excluded from /nosleep - for the marks in MRadmin's player list. */
    private static Map<String, List<String>> flags(MRSleep plugin) {
        Map<String, List<String>> flags = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (UUID id : plugin.noSleep().activeBlockers(world).keySet()) {
                List<String> f = flags.computeIfAbsent(id.toString(), k -> new ArrayList<>());
                if (!f.contains("sleep-blocking")) {
                    f.add("sleep-blocking");
                }
            }
        }
        for (UUID id : plugin.noSleep().activeBans().keySet()) {
            flags.computeIfAbsent(id.toString(), k -> new ArrayList<>()).add("sleep-banned");
        }
        return flags;
    }

    private static Map<String, Object> player(MRSleep plugin, UUID id) {
        NoSleepManager noSleep = plugin.noSleep();
        Player online = Bukkit.getPlayer(id);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("uuid", id.toString());
        p.put("name", NoSleepManager.name(id));
        p.put("online", online != null);
        if (online != null) {
            p.put("world", online.getWorld().getName());
            p.put("sleeping", online.isSleeping());
            String reason = plugin.sleep().exclusionReason(online);
            p.put("excluded", reason == null ? null : reason.substring("reason-".length()));
        }
        List<Map<String, Object>> blocking = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            int nights = noSleep.remainingNights(world, id);
            if (nights > 0) {
                blocking.add(Map.of("world", world.getName(), "nights", nights, "pending", noSleep.isPending(world, id)));
            }
        }
        p.put("blocking", blocking);
        p.put("cooldown", noSleep.remainingCooldown(id));
        p.put("bypass", noSleep.bypassRemaining(id));
        NoSleepManager.Ban ban = noSleep.ban(id);
        if (ban != null) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("permanent", ban.permanent());
            b.put("until", ban.permanent() ? 0 : ban.until());
            b.put("reason", ban.reason());
            b.put("by", ban.by());
            p.put("ban", b);
        }
        return p;
    }

    // ----------------------------------------------------------------- action

    /** Runs an admin action like in the menu (messages, broadcast, Discord log). @return null or an error key */
    private static String action(MRSleep plugin, Map<?, ?> args) {
        String action = String.valueOf(args.get("action"));
        String admin = args.get("admin") instanceof String name && !name.isBlank() ? name : "Web";
        AdminActions actions = plugin.adminActions();
        Settings.GuiSettings gui = plugin.settings().gui();

        if (action.equals("clear")) {
            World world = args.get("world") instanceof String name ? Bukkit.getWorld(name) : null;
            if (world == null || !plugin.sleep().isManaged(world)) {
                return "world-unknown";
            }
            return actions.clear(admin, null, world) ? null : "nothing-to-clear";
        }

        OfflinePlayer target = target(args.get("uuid"));
        if (target == null) {
            return "player-unknown";
        }
        long duration = args.get("duration") instanceof Number n ? n.longValue() : 0;
        return switch (action) {
            case "release" -> actions.release(admin, null, target) ? null : "not-blocking";
            case "bypass" -> {
                // Only the durations the menu offers (sleep.gui in MRsleep's config).
                if (!gui.bypassDurations().contains(duration)) {
                    yield "invalid-duration";
                }
                actions.bypass(admin, null, target, duration);
                yield null;
            }
            case "removeBypass" -> actions.removeBypass(admin, null, target) ? null : "no-bypass";
            case "resetCooldown" -> {
                actions.resetCooldown(admin, null, target);
                yield null;
            }
            case "ban" -> {
                if (!gui.banDurations().contains(duration)) {
                    yield "invalid-duration";
                }
                String reason = args.get("reason") instanceof String r ? r.strip() : "";
                if (reason.length() > MAX_REASON) {
                    reason = reason.substring(0, MAX_REASON);
                }
                actions.ban(admin, null, target, duration, reason);
                yield null;
            }
            case "unban" -> actions.unban(admin, null, target) ? null : "not-banned";
            default -> "unknown-action";
        };
    }

    /** A player who has been on the server (or is online); null for anything else. */
    private static OfflinePlayer target(Object uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(String.valueOf(uuid)));
            return player.isOnline() || player.hasPlayedBefore() ? player : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
