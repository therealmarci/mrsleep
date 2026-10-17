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
 * Admin operations shared by {@code /mrsleep} and the admin menu, so both behave identically
 * (feedback to the admin, notice to the affected player, broadcast and Discord log).
 */
public final class AdminActions {

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

    public void reload(CommandSender admin) {
        plugin.reload();
        messages().send(admin, "reloaded");
        plugin.discord().log("admin-reload", "admin", admin.getName());
    }

    public void clear(CommandSender admin, World world) {
        String names = NoSleepManager.names(plugin.noSleep().activeBlockers(world).keySet());
        if (plugin.noSleep().clear(world)) {
            messages().send(admin, "cleared", tag("world", world.getName()));
            messages().broadcast(world, "cleared-broadcast", tag("admin", admin.getName()));
            plugin.discord().log("admin-clear", "admin", admin.getName(), "world", world.getName(), "players", names);
        } else {
            messages().send(admin, "clear-none", tag("world", world.getName()));
        }
    }

    /** Lifts the target's blocks in all worlds. @return whether a block was active */
    public boolean release(CommandSender admin, OfflinePlayer target) {
        String name = name(target);
        List<World> worlds = plugin.noSleep().release(target.getUniqueId());
        if (worlds.isEmpty()) {
            messages().send(admin, "release-none", tag("player", name));
            return false;
        }
        messages().send(admin, "released", tag("player", name));
        for (World world : worlds) {
            messages().broadcast(world, "nosleep-broadcast-released", tag("player", name), tag("admin", admin.getName()));
            plugin.discord().log("admin-release", "admin", admin.getName(), "player", name, "world", world.getName());
        }
        return true;
    }

    public void bypass(CommandSender admin, OfflinePlayer target, long duration) {
        plugin.noSleep().setBypass(target.getUniqueId(), duration);
        String time = DurationUtil.format(duration);
        String name = name(target);
        messages().send(admin, "bypass-set", tag("player", name), tag("time", time));
        notifyTarget(admin, target, "bypass-granted-notify", tag("time", time));
        plugin.discord().log("admin-bypass", "admin", admin.getName(), "player", name, "time", time);
    }

    public void removeBypass(CommandSender admin, OfflinePlayer target) {
        String name = name(target);
        if (plugin.noSleep().removeBypass(target.getUniqueId())) {
            messages().send(admin, "bypass-removed", tag("player", name));
            notifyTarget(admin, target, "bypass-removed-notify");
            plugin.discord().log("admin-bypass-remove", "admin", admin.getName(), "player", name);
        } else {
            messages().send(admin, "bypass-none", tag("player", name));
        }
    }

    public void resetCooldown(CommandSender admin, OfflinePlayer target) {
        plugin.noSleep().resetCooldown(target.getUniqueId());
        String name = name(target);
        messages().send(admin, "cooldown-reset", tag("player", name));
        notifyTarget(admin, target, "cooldown-reset-notify");
        plugin.discord().log("admin-cooldown-reset", "admin", admin.getName(), "player", name);
    }

    /** Excludes the target from /nosleep and lifts their active blocks. */
    public void ban(CommandSender admin, OfflinePlayer target, long duration, String reason) {
        String cleanReason = reason == null || reason.isBlank() ? messages().plain("ban-no-reason") : reason;
        plugin.noSleep().ban(target.getUniqueId(), duration, cleanReason, admin.getName());
        String name = name(target);
        String durationText = banDuration(plugin.noSleep().ban(target.getUniqueId()));
        messages().send(admin, "ban-set", tag("player", name), tag("duration", durationText), tag("reason", cleanReason));
        notifyTarget(admin, target, "ban-notify", tag("duration", durationText), tag("reason", cleanReason));
        plugin.discord().log("admin-ban", "admin", admin.getName(), "player", name,
                "time", durationText, "reason", cleanReason);

        for (World world : plugin.noSleep().release(target.getUniqueId())) {
            messages().broadcast(world, "nosleep-broadcast-released", tag("player", name), tag("admin", admin.getName()));
        }
    }

    public void unban(CommandSender admin, OfflinePlayer target) {
        String name = name(target);
        if (plugin.noSleep().unban(target.getUniqueId())) {
            messages().send(admin, "unban-set", tag("player", name));
            notifyTarget(admin, target, "unban-notify");
            plugin.discord().log("admin-unban", "admin", admin.getName(), "player", name);
        } else {
            messages().send(admin, "unban-none", tag("player", name));
        }
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
