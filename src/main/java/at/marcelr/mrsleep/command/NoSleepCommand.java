package at.marcelr.mrsleep.command;

import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.NoSleepManager;
import at.marcelr.mrsleep.util.DurationUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * /nosleep           - blocks the current/upcoming night, or lifts the own block if one is active
 * /nosleep <nights>  - blocks the next N nights (replaces an own block, counts as a use)
 * /nosleep off       - lifts the own block
 */
public final class NoSleepCommand implements TabExecutor {

    private static final List<String> OFF_WORDS = List.of("off", "aus", "cancel");

    private final MRSleep plugin;

    public NoSleepCommand(MRSleep plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("mrsleep.nosleep")) {
            messages.send(player, "no-permission");
            return true;
        }
        World world = player.getWorld();
        if (!plugin.sleep().isManaged(world)) {
            messages.send(player, "world-not-managed");
            return true;
        }

        NoSleepManager noSleep = plugin.noSleep();
        boolean active = noSleep.remainingNights(world, player.getUniqueId()) > 0;
        int maxNights = plugin.settings().maxNights();

        if (args.length > 0 && OFF_WORDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            if (active) {
                lift(player, world);
            } else {
                messages.send(player, "nosleep-not-active");
            }
            return true;
        }
        if (args.length == 0 && active) {
            lift(player, world);
            return true;
        }

        int nights = 1;
        if (args.length > 0) {
            try {
                nights = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                nights = -1;
            }
            if (nights < 1 || nights > maxNights) {
                messages.send(player, "nosleep-invalid-nights", tag("max", maxNights));
                return true;
            }
        }

        // Lifting an own block stays possible; only new blocks are refused.
        NoSleepManager.Ban ban = noSleep.ban(player.getUniqueId());
        if (ban != null) {
            messages.send(player, "nosleep-banned",
                    tag("duration", plugin.adminActions().banDuration(ban)), tag("reason", ban.reason()));
            return true;
        }

        long remaining = noSleep.remainingCooldown(player);
        if (remaining > 0) {
            messages.send(player, "nosleep-cooldown", tag("time", DurationUtil.format(remaining)));
            return true;
        }

        // Starts the cooldown right away at night, otherwise once the night begins (SleepManager#housekeeping).
        noSleep.setBlock(world, player.getUniqueId(), nights);

        String nightsText = messages.nights(nights);
        messages.send(player, active ? "nosleep-updated" : "nosleep-activated", tag("nights", nightsText));
        if (noSleep.isPending(world, player.getUniqueId()) && noSleep.remainingCooldown(player) == 0
                && !player.hasPermission(NoSleepManager.PERM_NO_COOLDOWN) && noSleep.bypassRemaining(player.getUniqueId()) <= 0) {
            messages.send(player, "nosleep-pending-info", tag("time", DurationUtil.format(plugin.settings().cooldownMillis())));
        }
        if (plugin.settings().broadcast()) {
            messages.broadcast(world, "nosleep-broadcast-activated",
                    tag("player", player.getName()), tag("nights", nightsText));
        }
        plugin.discord().log("nosleep-activated", "player", player.getName(), "world", world.getName(),
                "nights", nightsText);
        return true;
    }

    private void lift(Player player, World world) {
        NoSleepManager noSleep = plugin.noSleep();
        // Before the night began the cooldown never started, so lifting is free.
        boolean free = noSleep.isPending(world, player.getUniqueId());
        noSleep.removeBlock(world, player.getUniqueId());
        long remaining = noSleep.remainingCooldown(player);
        if (free || remaining == 0) {
            plugin.messages().send(player, free ? "nosleep-deactivated-free" : "nosleep-deactivated");
        } else {
            plugin.messages().send(player, "nosleep-deactivated-cooldown", tag("time", DurationUtil.format(remaining)));
        }
        if (plugin.settings().broadcast()) {
            plugin.messages().broadcast(world, "nosleep-broadcast-deactivated", tag("player", player.getName()));
        }
        plugin.discord().log("nosleep-deactivated", "player", player.getName(), "world", world.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (int i = 1; i <= plugin.settings().maxNights(); i++) {
                options.add(String.valueOf(i));
            }
            options.add("off");
            options.removeIf(option -> !option.startsWith(args[0].toLowerCase(Locale.ROOT)));
        }
        return options;
    }
}
