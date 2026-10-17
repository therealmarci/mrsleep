package at.marcelr.mrsleep;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

public final class SleepListener implements Listener {

    private final MRSleep plugin;
    /** "world:player:night" - so a player clicking the bed repeatedly is logged to Discord only once per night. */
    private final Set<String> loggedBedAttempts = new HashSet<>();

    public SleepListener(MRSleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!plugin.sleep().isManaged(world)) {
            return;
        }
        Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
        if (blockers.isEmpty()) {
            return;
        }

        String names = NoSleepManager.names(blockers.keySet());
        String nights = plugin.messages().nights(NoSleepManager.longest(blockers));
        boolean prevent = plugin.settings().preventBedEnter();
        if (prevent) {
            event.setCancelled(true);
        }
        plugin.messages().send(player, prevent ? "bed-prevented" : "bed-blocked-warning",
                tag("players", names), tag("nights", nights));

        String key = world.getUID() + ":" + player.getUniqueId() + ":" + NoSleepManager.nightKey(world);
        if (loggedBedAttempts.add(key)) {
            plugin.discord().log("bed-blocked", "player", player.getName(), "world", world.getName(),
                    "players", names, "nights", nights);
        }
    }

    /** Tells the world when someone lies down (only if the night is not blocked). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEntered(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!plugin.sleep().isManaged(world) || plugin.noSleep().isBlocked(world)) {
            return;
        }
        String reason = plugin.sleep().exclusionReason(player);
        if (reason != null) {
            // Otherwise an excluded player gets no feedback at all and assumes sleeping is broken.
            plugin.messages().send(player, "bed-excluded", tag("reason", plugin.messages().plain(reason)));
            return;
        }
        // One tick later the player is actually in bed and counted as sleeping.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !player.isSleeping()) {
                return;
            }
            SleepManager.Status status = plugin.sleep().status(world);
            plugin.messages().broadcast(world, "player-sleeping",
                    tag("player", player.getName()),
                    tag("sleeping", status.sleeping()),
                    tag("needed", status.needed()));
            plugin.sleep().showProgress(world);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // A little later, so the notice is not buried under join messages.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.updateChecker().notifyIfOutdated(player);
            }
        }, 60L);

        World world = player.getWorld();
        if (!plugin.sleep().isManaged(world)) {
            return;
        }
        Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
        if (!blockers.isEmpty()) {
            plugin.messages().send(player, "join-blocked-info",
                    tag("players", NoSleepManager.names(blockers.keySet())),
                    tag("nights", plugin.messages().nights(NoSleepManager.longest(blockers))));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.settings().releaseOnQuit()) {
            return;
        }
        Player player = event.getPlayer();
        for (World world : plugin.noSleep().release(player.getUniqueId())) {
            if (plugin.settings().broadcast()) {
                plugin.messages().broadcast(world, "nosleep-broadcast-quit", tag("player", player.getName()));
            }
            plugin.discord().log("nosleep-quit", "player", player.getName(), "world", world.getName());
        }
    }

    /** Safety net: never let vanilla skip the night in worlds MRsleep manages. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP && plugin.sleep().isManaged(event.getWorld())) {
            event.setCancelled(true);
        }
        // New day, new attempts: keep the de-duplication set small.
        loggedBedAttempts.clear();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.sleep().applyGameRule(event.getWorld());
    }
}
