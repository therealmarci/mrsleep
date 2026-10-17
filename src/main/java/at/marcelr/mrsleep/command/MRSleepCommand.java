package at.marcelr.mrsleep.command;

import at.marcelr.mrsleep.AdminActions;
import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.NoSleepManager;
import at.marcelr.mrsleep.Settings;
import at.marcelr.mrsleep.SleepManager;
import at.marcelr.mrsleep.UpdateChecker;
import at.marcelr.mrsleep.gui.MainMenu;
import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

public final class MRSleepCommand implements TabExecutor {

    private static final String PERM_GUI = "mrsleep.admin.gui";
    private static final String PERM_RELOAD = "mrsleep.admin.reload";
    private static final String PERM_STATUS = "mrsleep.admin.status";
    private static final String PERM_CLEAR = "mrsleep.admin.clear";
    private static final String PERM_BYPASS = "mrsleep.admin.bypass";
    private static final String PERM_BAN = "mrsleep.admin.ban";

    private static final List<String> PERMANENT_WORDS = List.of("permanent", "perm", "dauerhaft", "forever");

    private final MRSleep plugin;

    public MRSleepCommand(MRSleep plugin) {
        this.plugin = plugin;
    }

    private Messages messages() {
        return plugin.messages();
    }

    private AdminActions actions() {
        return plugin.adminActions();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages().send(sender, hasAnyAdminPermission(sender) ? "admin-usage" : "no-permission");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version", "ver" -> version(sender);
            case "gui", "menu", "menü" -> {
                if (!(sender instanceof Player player)) {
                    messages().send(sender, "players-only");
                } else if (check(sender, PERM_GUI)) {
                    new MainMenu(plugin, player).open();
                }
            }
            case "reload" -> {
                if (check(sender, PERM_RELOAD)) {
                    actions().reload(sender);
                }
            }
            case "status" -> {
                if (check(sender, PERM_STATUS)) {
                    status(sender, args);
                }
            }
            case "debug" -> {
                if (check(sender, PERM_STATUS)) {
                    debug(sender);
                }
            }
            case "clear" -> {
                if (check(sender, PERM_CLEAR)) {
                    clear(sender, args);
                }
            }
            case "release" -> {
                if (check(sender, PERM_CLEAR)) {
                    withPlayer(sender, args, 1, target -> actions().release(sender, target));
                }
            }
            case "bypass" -> {
                if (check(sender, PERM_BYPASS)) {
                    bypass(sender, args);
                }
            }
            case "resetcooldown" -> {
                if (check(sender, PERM_BYPASS)) {
                    withPlayer(sender, args, 1, target -> actions().resetCooldown(sender, target));
                }
            }
            case "ban" -> {
                if (check(sender, PERM_BAN)) {
                    ban(sender, args);
                }
            }
            case "unban" -> {
                if (check(sender, PERM_BAN)) {
                    withPlayer(sender, args, 1, target -> actions().unban(sender, target));
                }
            }
            case "bans" -> {
                if (check(sender, PERM_BAN)) {
                    bans(sender);
                }
            }
            default -> messages().send(sender, "admin-usage");
        }
        return true;
    }

    // ------------------------------------------------------------- version

    private void version(CommandSender sender) {
        UpdateChecker updates = plugin.updateChecker();
        messages().send(sender, "version",
                tag("version", updates.currentVersion()),
                tag("author", String.join(", ", plugin.getPluginMeta().getAuthors())));
        // Order: version, update check, result, empty line, links.
        Runnable links = () -> {
            sender.sendMessage(Component.empty());
            messages().send(sender, "version-links",
                    tag("modrinth", link("Modrinth", updates.modrinthPage())),
                    tag("github", link("GitHub", updates.githubPage())));
        };
        if (plugin.settings().update().enabled()) {
            updates.checkNow(sender, links);
        } else {
            updates.sendStatus(sender);
            links.run();
        }
    }

    private static Component link(String label, String url) {
        return Component.text(label).color(NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url));
    }

    // -------------------------------------------------------------- status

    private void status(CommandSender sender, String[] args) {
        List<World> worlds = new ArrayList<>();
        if (args.length >= 2) {
            World world = resolveWorld(sender, args[1]);
            if (world == null) {
                return;
            }
            worlds.add(world);
        } else {
            for (World world : Bukkit.getWorlds()) {
                if (plugin.sleep().isManaged(world)) {
                    worlds.add(world);
                }
            }
        }
        for (World world : worlds) {
            SleepManager.Status status = plugin.sleep().status(world);
            Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
            List<String> entries = new ArrayList<>();
            blockers.forEach((uuid, nights) ->
                    entries.add(NoSleepManager.name(uuid) + " (" + messages().nights(nights) + ")"));
            String names = entries.isEmpty() ? messages().plain("status-none") : String.join(", ", entries);
            messages().send(sender, "status",
                    tag("world", world.getName()),
                    tag("sleeping", status.sleeping()),
                    tag("needed", status.needed()),
                    tag("counted", status.counted()),
                    tag("exempt", status.exempt()),
                    tag("players", names));
        }
    }

    /** Shows per world and per player why the night is or is not being skipped. */
    private void debug(CommandSender sender) {
        Messages messages = messages();
        for (World world : Bukkit.getWorlds()) {
            boolean managed = plugin.sleep().isManaged(world);
            Map<UUID, Integer> blockers = managed ? plugin.noSleep().activeBlockers(world) : Map.of();
            messages.send(sender, "debug-world",
                    tag("world", world.getName()),
                    tag("managed", managed ? "ja" : "nein"),
                    tag("gamerule", String.valueOf(world.getGameRuleValue(GameRules.PLAYERS_SLEEPING_PERCENTAGE))),
                    tag("time", world.getTime()),
                    tag("night", SleepManager.isNight(world) ? "ja" : "nein"),
                    tag("players", blockers.isEmpty() ? messages.plain("status-none") : NoSleepManager.names(blockers.keySet())));
            if (!managed) {
                continue;
            }
            SleepManager.Status status = plugin.sleep().status(world);
            messages.send(sender, "debug-quota",
                    tag("counted", status.counted()),
                    tag("exempt", status.exempt()),
                    tag("sleeping", status.sleeping()),
                    tag("deep", status.deepSleeping()),
                    tag("needed", status.needed()));
            for (Player player : world.getPlayers()) {
                String reason = plugin.sleep().exclusionReason(player);
                messages.send(sender, "debug-player",
                        tag("player", player.getName()),
                        tag("counts", reason == null ? messages.plain("debug-counts-yes")
                                : messages.plain("debug-counts-no") + " (" + messages.plain(reason) + ")"),
                        tag("sleeping", player.isSleeping() ? "ja" : "nein"),
                        tag("ticks", player.getSleepTicks()),
                        tag("cooldown", cooldownState(player)));
            }
        }
    }

    private String cooldownState(Player player) {
        Messages messages = messages();
        NoSleepManager noSleep = plugin.noSleep();
        if (player.hasPermission(NoSleepManager.PERM_NO_COOLDOWN)) {
            return messages.plain("debug-cooldown-permission");
        }
        long bypass = noSleep.bypassRemaining(player.getUniqueId());
        if (bypass > 0) {
            return messages.plain("debug-cooldown-bypass").replace("<time>", DurationUtil.format(bypass));
        }
        if (noSleep.isPendingAnywhere(player.getUniqueId())) {
            return messages.plain("debug-cooldown-pending");
        }
        long remaining = noSleep.remainingCooldown(player);
        return remaining > 0
                ? messages.plain("debug-cooldown-remaining").replace("<time>", DurationUtil.format(remaining))
                : messages.plain("debug-cooldown-ready");
    }

    // -------------------------------------------------------------- blocks

    private void clear(CommandSender sender, String[] args) {
        World world;
        if (args.length >= 2) {
            world = resolveWorld(sender, args[1]);
        } else if (sender instanceof Player player) {
            world = resolveWorld(sender, player.getWorld().getName());
        } else {
            messages().send(sender, "admin-usage");
            return;
        }
        if (world != null) {
            actions().clear(sender, world);
        }
    }

    // ------------------------------------------------------------- bypass

    private void bypass(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            Map<UUID, Long> active = plugin.noSleep().activeBypasses();
            if (active.isEmpty()) {
                messages().send(sender, "bypass-list-empty");
                return;
            }
            messages().send(sender, "bypass-list-header");
            active.forEach((uuid, remaining) -> messages().send(sender, "bypass-list-entry",
                    tag("player", NoSleepManager.name(uuid)),
                    tag("time", DurationUtil.format(remaining))));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
            withPlayer(sender, args, 2, target -> actions().removeBypass(sender, target));
            return;
        }
        if (args.length < 3) {
            messages().send(sender, "admin-usage");
            return;
        }
        long duration = DurationUtil.parse(args[2]);
        if (duration <= 0) {
            messages().send(sender, "invalid-duration", tag("input", args[2]));
            return;
        }
        withPlayer(sender, args, 1, target -> actions().bypass(sender, target, duration));
    }

    // ---------------------------------------------------------------- bans

    /** /mrsleep ban <player> [duration|permanent] [reason...] - without a duration the ban is permanent. */
    private void ban(CommandSender sender, String[] args) {
        long duration = Settings.PERMANENT;
        int reasonStart = 2;
        if (args.length >= 3) {
            if (PERMANENT_WORDS.contains(args[2].toLowerCase(Locale.ROOT))) {
                reasonStart = 3;
            } else {
                long parsed = DurationUtil.parse(args[2]);
                if (parsed > 0) {
                    duration = parsed;
                    reasonStart = 3;
                }
            }
        }
        String reason = args.length > reasonStart
                ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length)) : "";
        long finalDuration = duration;
        withPlayer(sender, args, 1, target -> actions().ban(sender, target, finalDuration, reason));
    }

    private void bans(CommandSender sender) {
        Map<UUID, NoSleepManager.Ban> bans = plugin.noSleep().activeBans();
        if (bans.isEmpty()) {
            messages().send(sender, "bans-empty");
            return;
        }
        messages().send(sender, "bans-header");
        bans.forEach((uuid, ban) -> messages().send(sender, "bans-entry",
                tag("player", NoSleepManager.name(uuid)),
                tag("duration", actions().banDuration(ban)),
                tag("reason", ban.reason()),
                tag("by", ban.by())));
    }

    // ------------------------------------------------------------- helpers

    private interface PlayerAction {
        void run(OfflinePlayer target);
    }

    /** Resolves the player name at {@code args[index]} and runs the action, or reports usage / not found. */
    private void withPlayer(CommandSender sender, String[] args, int index, PlayerAction action) {
        if (args.length <= index) {
            messages().send(sender, "admin-usage");
            return;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(args[index]);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(args[index]);
        }
        if (target == null) {
            messages().send(sender, "player-not-found", tag("player", args[index]));
            return;
        }
        action.run(target);
    }

    private boolean check(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages().send(sender, "no-permission");
        return false;
    }

    private static boolean hasAnyAdminPermission(CommandSender sender) {
        for (String permission : List.of(PERM_GUI, PERM_RELOAD, PERM_STATUS, PERM_CLEAR, PERM_BYPASS, PERM_BAN)) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private World resolveWorld(CommandSender sender, String name) {
        World world = Bukkit.getWorld(name);
        if (world == null || !plugin.sleep().isManaged(world)) {
            messages().send(sender, "world-not-found", tag("world", name));
            return null;
        }
        return world;
    }

    // --------------------------------------------------------- tab complete

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("version");
            addIf(sender, PERM_GUI, options, "gui");
            addIf(sender, PERM_STATUS, options, "status", "debug");
            addIf(sender, PERM_CLEAR, options, "clear", "release");
            addIf(sender, PERM_BYPASS, options, "bypass", "resetcooldown");
            addIf(sender, PERM_BAN, options, "ban", "unban", "bans");
            addIf(sender, PERM_RELOAD, options, "reload");
        } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "status", "clear" -> {
                    if (args.length == 2) {
                        for (World world : Bukkit.getWorlds()) {
                            if (plugin.sleep().isManaged(world)) {
                                options.add(world.getName());
                            }
                        }
                    }
                }
                case "release", "resetcooldown" -> {
                    if (args.length == 2) {
                        onlineNames(options);
                    }
                }
                case "bypass" -> {
                    if (args.length == 2) {
                        options.addAll(List.of("list", "remove"));
                        onlineNames(options);
                    } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                        onlineNames(options);
                    } else if (args.length == 3 && !args[1].equalsIgnoreCase("list")) {
                        options.addAll(List.of("30m", "1h", "6h", "1d", "7d"));
                    }
                }
                case "ban" -> {
                    if (args.length == 2) {
                        onlineNames(options);
                    } else if (args.length == 3) {
                        options.addAll(List.of("permanent", "1h", "1d", "7d", "30d"));
                    }
                }
                case "unban" -> {
                    if (args.length == 2) {
                        plugin.noSleep().activeBans().keySet().forEach(uuid -> options.add(NoSleepManager.name(uuid)));
                    }
                }
                default -> {
                }
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }

    private static void addIf(CommandSender sender, String permission, List<String> target, String... values) {
        if (sender.hasPermission(permission)) {
            target.addAll(List.of(values));
        }
    }

    private static void onlineNames(List<String> target) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            target.add(player.getName());
        }
    }
}
