package at.marcelr.mrsleep;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Shows the sleep progress ("1/2 players sleeping") or the blocked notice, either as a boss bar (top of the
 * screen, not covered by the chat window that opens while lying in bed), action bar, subtitle or not at all.
 */
public final class SleepDisplay {

    public enum Mode { BOSSBAR, ACTIONBAR, TITLE, NONE }

    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(250));

    private final MRSleep plugin;
    /** One boss bar per world, plus the players currently seeing it. */
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Set<UUID>> viewers = new HashMap<>();

    public SleepDisplay(MRSleep plugin) {
        this.plugin = plugin;
    }

    private Settings.DisplaySettings settings() {
        return plugin.settings().display();
    }

    /** Progress for everyone in the world. */
    public void progress(World world, SleepManager.Status status) {
        Component text = plugin.messages().get("display-sleeping",
                tag("sleeping", status.sleeping()),
                tag("needed", status.needed()),
                tag("counted", status.counted()));
        float progress = status.needed() == 0 ? 0f : Math.min(1f, (float) status.sleeping() / status.needed());
        show(world, text, progress, settings().color(), world.getPlayers());
    }

    /** Blocked notice, only for the players lying in bed. */
    public void blocked(World world, String players, String nights) {
        Component text = plugin.messages().get("display-blocked", tag("players", players), tag("nights", nights));
        List<Player> sleepers = world.getPlayers().stream().filter(Player::isSleeping).toList();
        show(world, text, 1f, settings().blockedColor(), sleepers);
    }

    private void show(World world, Component text, float progress, BossBar.Color color, Collection<Player> targets) {
        switch (settings().mode()) {
            case BOSSBAR -> showBar(world, text, progress, color, targets);
            case ACTIONBAR -> targets.forEach(player -> player.sendActionBar(text));
            case TITLE -> targets.forEach(player -> player.showTitle(Title.title(Component.empty(), text, TITLE_TIMES)));
            case NONE -> {
            }
        }
    }

    private void showBar(World world, Component text, float progress, BossBar.Color color, Collection<Player> targets) {
        BossBar bar = bars.computeIfAbsent(world.getUID(),
                id -> BossBar.bossBar(text, progress, color, BossBar.Overlay.NOTCHED_10));
        bar.name(text);
        bar.progress(progress);
        bar.color(color);

        Set<UUID> now = new HashSet<>();
        for (Player player : targets) {
            player.showBossBar(bar);
            now.add(player.getUniqueId());
        }
        // Hide for players that no longer belong to the audience (left the world, got up, ...).
        Set<UUID> before = viewers.getOrDefault(world.getUID(), Set.of());
        for (UUID uuid : before) {
            if (!now.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.hideBossBar(bar);
                }
            }
        }
        viewers.put(world.getUID(), now);
    }

    /** Nobody sleeps (any more) in this world. */
    public void hide(World world) {
        BossBar bar = bars.remove(world.getUID());
        Set<UUID> seen = viewers.remove(world.getUID());
        if (bar == null || seen == null) {
            return;
        }
        for (UUID uuid : seen) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }

    public void hideAll() {
        for (World world : Bukkit.getWorlds()) {
            hide(world);
        }
        bars.clear();
        viewers.clear();
    }
}
