package at.marcelr.mrsleep;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Replaces the vanilla sleep check. Vanilla can only treat ignored players as "sleeping",
 * which still counts them towards the total; MRsleep removes them from the quota entirely.
 */
public final class SleepManager {

    public static final String PERM_EXEMPT = "mrsleep.exempt";

    /**
     * Gamerule value in managed worlds. 100 % means vanilla only tries to skip when literally everyone sleeps,
     * and that attempt is cancelled in {@link SleepListener#onTimeSkip}. Values above 100 must NOT be used:
     * since 26.x vanilla then tells every player "No amount of rest can pass this night" when lying down.
     */
    private static final int VANILLA_PERCENTAGE = 100;
    /** Ticks a player must lie in bed before counting (same as vanilla). */
    private static final int DEEP_SLEEP_TICKS = 100;
    private static final long CHECK_INTERVAL = 10L;

    private final MRSleep plugin;
    private final Set<UUID> managedWorlds = new HashSet<>();
    /** world -> night key for which the "night begins" notice was already handled */
    private final Map<UUID, Long> nightAnnounced = new HashMap<>();
    private BukkitTask task;

    public record Status(int counted, int exempt, int sleeping, int deepSleeping, int needed) {
    }

    public SleepManager(MRSleep plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getWorlds().forEach(this::applyGameRule);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, CHECK_INTERVAL);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (World world : Bukkit.getWorlds()) {
            if (managedWorlds.contains(world.getUID())) {
                world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, plugin.settings().percentage());
            }
        }
        managedWorlds.clear();
    }

    public boolean isManaged(World world) {
        return world.getEnvironment() == World.Environment.NORMAL
                && !plugin.settings().disabledWorlds().contains(world.getName().toLowerCase(Locale.ROOT));
    }

    /** Disables vanilla night skipping in managed worlds, restores it in worlds no longer managed. */
    public void applyGameRule(World world) {
        if (isManaged(world)) {
            world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, VANILLA_PERCENTAGE);
            managedWorlds.add(world.getUID());
        } else if (managedWorlds.remove(world.getUID())) {
            world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, plugin.settings().percentage());
        }
    }

    public boolean isExcluded(Player player) {
        return exclusionReason(player) != null;
    }

    /** @return message key of the reason the player does not count towards the quota, or null if they count */
    public String exclusionReason(Player player) {
        Settings s = plugin.settings();
        if (player.hasPermission(PERM_EXEMPT)) {
            return "reason-exempt";
        }
        if (s.ignoreSpectators() && player.getGameMode() == GameMode.SPECTATOR) {
            return "reason-spectator";
        }
        if (s.respectSleepingIgnored() && player.isSleepingIgnored()) {
            return "reason-sleeping-ignored";
        }
        return null;
    }

    /** Sends the sleep progress to everyone in the world right away (instead of waiting for the next check). */
    public void showProgress(World world) {
        if (!plugin.settings().actionBar() || plugin.noSleep().isBlocked(world)) {
            return;
        }
        Status status = status(world);
        if (status.sleeping() == 0) {
            return;
        }
        for (Player player : world.getPlayers()) {
            plugin.messages().actionBar(player, "actionbar-sleeping",
                    tag("sleeping", status.sleeping()),
                    tag("needed", status.needed()),
                    tag("counted", status.counted()));
        }
    }

    public static boolean isNight(World world) {
        long time = world.getTime() % 24_000L;
        return time >= NoSleepManager.NIGHT_START && time < NoSleepManager.NIGHT_END;
    }

    public Status status(World world) {
        int counted = 0;
        int exempt = 0;
        int sleeping = 0;
        int deep = 0;
        for (Player player : world.getPlayers()) {
            if (isExcluded(player)) {
                exempt++;
                continue;
            }
            counted++;
            if (player.isSleeping()) {
                sleeping++;
                if (player.getSleepTicks() >= DEEP_SLEEP_TICKS) {
                    deep++;
                }
            }
        }
        int needed = 0;
        if (counted > 0) {
            Settings s = plugin.settings();
            int byPercentage = (int) Math.ceil(counted * s.percentage() / 100.0);
            needed = Math.max(1, Math.min(counted, Math.max(s.minimumSleepers(), byPercentage)));
        }
        return new Status(counted, exempt, sleeping, deep, needed);
    }

    private void tick() {
        for (World world : Bukkit.getWorlds()) {
            if (isManaged(world)) {
                housekeeping(world);
                checkSleep(world);
            }
        }
    }

    /** Announces expired blocks and, once per night, that the beginning night is blocked. */
    private void housekeeping(World world) {
        Messages messages = plugin.messages();
        for (UUID expired : plugin.noSleep().purgeExpired(world)) {
            String name = NoSleepManager.name(expired);
            messages.broadcast(world, "nosleep-expired", tag("player", name));
            plugin.discord().log("nosleep-expired", "player", name, "world", world.getName());
        }

        if (!isNight(world)) {
            return;
        }
        long key = NoSleepManager.nightKey(world);
        if (Objects.equals(nightAnnounced.put(world.getUID(), key), key)) {
            return;
        }
        Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
        if (!blockers.isEmpty()) {
            String names = NoSleepManager.names(blockers.keySet());
            String nights = messages.nights(NoSleepManager.longest(blockers));
            messages.broadcast(world, "night-start-blocked", tag("players", names), tag("nights", nights));
            plugin.discord().log("night-start-blocked", "world", world.getName(), "players", names, "nights", nights);
        }
    }

    private void checkSleep(World world) {
        Status status = status(world);
        if (status.sleeping() == 0) {
            return;
        }
        Messages messages = plugin.messages();
        boolean actionBar = plugin.settings().actionBar();

        Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
        if (!blockers.isEmpty()) {
            if (actionBar) {
                String names = NoSleepManager.names(blockers.keySet());
                String nights = messages.nights(NoSleepManager.longest(blockers));
                for (Player player : world.getPlayers()) {
                    if (player.isSleeping()) {
                        messages.actionBar(player, "actionbar-blocked", tag("players", names), tag("nights", nights));
                    }
                }
            }
            return;
        }

        if (status.deepSleeping() >= status.needed()) {
            skipNight(world, status);
            return;
        }

        if (actionBar) {
            for (Player player : world.getPlayers()) {
                messages.actionBar(player, "actionbar-sleeping",
                        tag("sleeping", status.sleeping()),
                        tag("needed", status.needed()),
                        tag("counted", status.counted()));
            }
        }
    }

    private void skipNight(World world, Status status) {
        long time = world.getTime();
        world.setFullTime(world.getFullTime() + (24_000L - time % 24_000L));

        List<Player> players = world.getPlayers();
        for (Player player : players) {
            if (player.isSleeping()) {
                player.wakeup(false);
            }
        }
        if (plugin.settings().clearWeather() && world.hasStorm()) {
            world.setStorm(false);
            world.setThundering(false);
        }
        if (plugin.settings().announceSkip()) {
            plugin.messages().broadcast(world, "night-skipped");
        }
        plugin.discord().log("night-skipped", "world", world.getName(),
                "sleeping", status.deepSleeping(), "needed", status.needed(), "counted", status.counted());
    }
}
