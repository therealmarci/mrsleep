package at.marcelr.mrsleep;

import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Admin operations shared by {@code /mrsleep}, the admin menu and MRadmin's web interface, so all behave identically
 * (feedback to the admin, notice to the affected player, broadcast and Discord log).
 * <p>
 * Every action exists twice: with the admin as {@link CommandSender} (in game, console) and with the admin's name and
 * an optional receiver for the feedback (null = none, e.g. an admin in the web interface who is not online). The
 * second form returns whether something changed.
 */
public final class AdminActions {

    /** Bypass up to limits.support.bypass, ban up to limits.support.ban (and lift both). */
    public static final String PERM_BYPASS = "mrsleep.admin.bypass";
    public static final String PERM_BAN = "mrsleep.admin.ban";
    /** Up to limits.staff.*. */
    public static final String PERM_BYPASS_STAFF = "mrsleep.admin.bypass.staff";
    public static final String PERM_BAN_STAFF = "mrsleep.admin.ban.staff";
    /** No limit, permanent bans. */
    public static final String PERM_BYPASS_UNLIMITED = "mrsleep.admin.bypass.unlimited";
    public static final String PERM_BAN_UNLIMITED = "mrsleep.admin.ban.unlimited";
    /** Reset a player's /nosleep cooldown. */
    public static final String PERM_RESET = "mrsleep.admin.resetcooldown";

    private final MRSleep plugin;

    public AdminActions(MRSleep plugin) {
        this.plugin = plugin;
    }

    private Messages messages() {
        return plugin.messages();
    }

    public static String name(OfflinePlayer player) {
        String name = player.getName();
        return name == null ? player.getUniqueId().toString().substring(0, 8) : name;
    }

    /** Feedback for the admin - only if there is someone to tell. */
    private void reply(CommandSender feedback, String key, TagResolver... resolvers) {
        if (feedback != null) {
            messages().send(feedback, key, resolvers);
        }
    }

    public void reload(CommandSender admin) {
        plugin.reload();
        messages().send(admin, "reloaded");
        plugin.discord().log("admin-reload", "admin", admin.getName());
    }

    public void clear(CommandSender admin, World world) {
        clear(admin.getName(), admin, world);
    }

    /** Lifts all /nosleep blocks in the world. @return whether a block was active */
    public boolean clear(String admin, CommandSender feedback, World world) {
        String names = NoSleepManager.names(plugin.noSleep().activeBlockers(world).keySet());
        if (!plugin.noSleep().clear(world)) {
            reply(feedback, "clear-none", tag("world", world.getName()));
            return false;
        }
        reply(feedback, "cleared", tag("world", world.getName()));
        messages().broadcast(world, "cleared-broadcast", tag("admin", admin));
        plugin.discord().log("admin-clear", "admin", admin, "world", world.getName(), "players", names);
        return true;
    }

    /** Lifts the target's blocks in all worlds. @return whether a block was active */
    public boolean release(CommandSender admin, OfflinePlayer target) {
        return release(admin.getName(), admin, target);
    }

    public boolean release(String admin, CommandSender feedback, OfflinePlayer target) {
        String name = name(target);
        List<World> worlds = plugin.noSleep().release(target.getUniqueId());
        if (worlds.isEmpty()) {
            reply(feedback, "release-none", tag("player", name));
            return false;
        }
        reply(feedback, "released", tag("player", name));
        for (World world : worlds) {
            messages().broadcast(world, "nosleep-broadcast-released", tag("player", name), tag("admin", admin));
            plugin.discord().log("admin-release", "admin", admin, "player", name, "world", world.getName());
        }
        return true;
    }

    // ------------------------------------------------------------------ team limits

    /** The permission a bypass of this length needs (limits in config.yml). */
    public String bypassPermission(long duration) {
        Settings.LimitSettings limits = plugin.settings().limits();
        return duration <= limits.supportBypass() ? PERM_BYPASS
                : duration <= limits.staffBypass() ? PERM_BYPASS_STAFF : PERM_BYPASS_UNLIMITED;
    }

    /** The permission a ban of this length needs; a permanent one only with .ban.unlimited. */
    public String banPermission(long duration) {
        Settings.LimitSettings limits = plugin.settings().limits();
        return duration == Settings.PERMANENT ? PERM_BAN_UNLIMITED : duration <= limits.supportBan() ? PERM_BAN
                : duration <= limits.staffBan() ? PERM_BAN_STAFF : PERM_BAN_UNLIMITED;
    }

    /** Longest bypass the sender may give, -1 = no limit. */
    private long maxBypass(CommandSender sender) {
        Settings.LimitSettings limits = plugin.settings().limits();
        return sender.hasPermission(PERM_BYPASS_UNLIMITED) ? -1
                : sender.hasPermission(PERM_BYPASS_STAFF) ? limits.staffBypass() : limits.supportBypass();
    }

    private long maxBan(CommandSender sender) {
        Settings.LimitSettings limits = plugin.settings().limits();
        return sender.hasPermission(PERM_BAN_UNLIMITED) ? -1
                : sender.hasPermission(PERM_BAN_STAFF) ? limits.staffBan() : limits.supportBan();
    }

    // ------------------------------------------------------------------ bypass

    public void bypass(CommandSender admin, OfflinePlayer target, long duration) {
        if (!admin.hasPermission(bypassPermission(duration))) {
            messages().send(admin, "limit-bypass", tag("time", DurationUtil.format(maxBypass(admin))));
            return;
        }
        bypass(admin.getName(), admin, target, duration);
    }

    public void bypass(String admin, CommandSender feedback, OfflinePlayer target, long duration) {
        plugin.noSleep().setBypass(target.getUniqueId(), duration);
        String time = DurationUtil.format(duration);
        String name = name(target);
        reply(feedback, "bypass-set", tag("player", name), tag("time", time));
        notifyTarget(feedback, target, "bypass-granted-notify", tag("time", time));
        plugin.discord().log("admin-bypass", "admin", admin, "player", name, "time", time);
    }

    public void removeBypass(CommandSender admin, OfflinePlayer target) {
        removeBypass(admin.getName(), admin, target);
    }

    /** @return whether a bypass was active */
    public boolean removeBypass(String admin, CommandSender feedback, OfflinePlayer target) {
        String name = name(target);
        if (!plugin.noSleep().removeBypass(target.getUniqueId())) {
            reply(feedback, "bypass-none", tag("player", name));
            return false;
        }
        reply(feedback, "bypass-removed", tag("player", name));
        notifyTarget(feedback, target, "bypass-removed-notify");
        plugin.discord().log("admin-bypass-remove", "admin", admin, "player", name);
        return true;
    }

    public void resetCooldown(CommandSender admin, OfflinePlayer target) {
        resetCooldown(admin.getName(), admin, target);
    }

    public void resetCooldown(String admin, CommandSender feedback, OfflinePlayer target) {
        plugin.noSleep().resetCooldown(target.getUniqueId());
        String name = name(target);
        reply(feedback, "cooldown-reset", tag("player", name));
        notifyTarget(feedback, target, "cooldown-reset-notify");
        plugin.discord().log("admin-cooldown-reset", "admin", admin, "player", name);
    }

    /** Excludes the target from /nosleep and lifts their active blocks. */
    public void ban(CommandSender admin, OfflinePlayer target, long duration, String reason) {
        if (!admin.hasPermission(banPermission(duration))) {
            messages().send(admin, "limit-ban", tag("time", DurationUtil.format(maxBan(admin))));
            return;
        }
        ban(admin.getName(), admin, target, duration, reason);
    }

    public void ban(String admin, CommandSender feedback, OfflinePlayer target, long duration, String reason) {
        // The reason is stored, so a missing one is filled in the server language.
        String cleanReason = reason == null || reason.isBlank()
                ? messages().server(() -> messages().plain("ban-no-reason")) : reason;
        plugin.noSleep().ban(target.getUniqueId(), duration, cleanReason, admin);
        String name = name(target);
        NoSleepManager.Ban ban = plugin.noSleep().ban(target.getUniqueId());
        reply(feedback, "ban-set", tag("player", name), tag("duration", banDuration(ban)), tag("reason", cleanReason));
        Player online = target.getPlayer();
        if (online != null && online != feedback) {
            messages().run(online, () -> messages().send(online, "ban-notify",
                    tag("duration", banDuration(ban)), tag("reason", cleanReason)));
        }
        plugin.discord().log("admin-ban", "admin", admin, "player", name,
                "time", messages().server(() -> banDuration(ban)), "reason", cleanReason);

        for (World world : plugin.noSleep().release(target.getUniqueId())) {
            messages().broadcast(world, "nosleep-broadcast-released", tag("player", name), tag("admin", admin));
        }
    }

    public void unban(CommandSender admin, OfflinePlayer target) {
        unban(admin.getName(), admin, target);
    }

    /** @return whether the player was banned */
    public boolean unban(String admin, CommandSender feedback, OfflinePlayer target) {
        String name = name(target);
        if (!plugin.noSleep().unban(target.getUniqueId())) {
            reply(feedback, "unban-none", tag("player", name));
            return false;
        }
        reply(feedback, "unban-set", tag("player", name));
        notifyTarget(feedback, target, "unban-notify");
        plugin.discord().log("admin-unban", "admin", admin, "player", name);
        return true;
    }

    /** "dauerhaft" or "noch 2h 5m". */
    public String banDuration(NoSleepManager.Ban ban) {
        if (ban == null || ban.permanent()) {
            return messages().plain("ban-duration-permanent");
        }
        return messages().plain("ban-duration-temporary").replace("<time>", DurationUtil.format(ban.remaining()));
    }

    /** Informs the affected player, if online and not the admin themself. */
    private void notifyTarget(CommandSender admin, OfflinePlayer target, String key, TagResolver... resolvers) {
        Player online = target.getPlayer();
        if (online != null && online != admin) {
            messages().send(online, key, resolvers);
        }
    }
}
