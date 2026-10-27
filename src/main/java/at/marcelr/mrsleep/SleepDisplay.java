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
import java.util.function.Supplier;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Shows the sleep progress ("1/2 players sleeping") or the blocked notice, either as a boss bar (top of the
 * screen, not covered by the chat window that opens while lying in bed), action bar, subtitle or not at all.
 * Every player gets the text in their own language, so every player has their own boss bar.
 */
public final class SleepDisplay {

    public enum Mode { BOSSBAR, ACTIONBAR, TITLE, NONE }

    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(250));

    private final MRSleep plugin;
    /** The boss bar of each player currently seeing one. */
    private final Map<UUID, BossBar> bars = new HashMap<>();
    /** Per world the players currently seeing its display. */
    private final Map<UUID, Set<UUID>> viewers = new HashMap<>();

    public SleepDisplay(MRSleep plugin) {
        this.plugin = plugin;
    }

    private Settings.DisplaySettings settings() {
        return plugin.settings().display();
    }

    /** Progress for everyone in the world. */
    public void progress(World world, SleepManager.Status status) {
        float progress = status.needed() == 0 ? 0f : Math.min(1f, (float) status.sleeping() / status.needed());
        show(world, () -> plugin.messages().get("display-sleeping",
                tag("sleeping", status.sleeping()),
                tag("needed", status.needed()),
                tag("counted", status.counted())), progress, settings().color(), world.getPlayers());
    }

    /** Blocked notice, only for the players lying in bed. */
    public void blocked(World world, String players, int nights) {
        List<Player> sleepers = world.getPlayers().stream().filter(Player::isSleeping).toList();
        show(world, () -> plugin.messages().get("display-blocked", tag("players", players),
                tag("nights", plugin.messages().nights(nights))), 1f, settings().blockedColor(), sleepers);
    }

    /** @param text builds the text; called once per player in their language */
    private void show(World world, Supplier<Component> text, float progress, BossBar.Color color,
                      Collection<Player> targets) {
        switch (settings().mode()) {
            case BOSSBAR -> showBars(world, text, progress, color, targets);
            case ACTIONBAR -> plugin.messages().each(targets, player -> ((Player) player).sendActionBar(text.get()));
            case TITLE -> plugin.messages().each(targets,
                    player -> ((Player) player).showTitle(Title.title(Component.empty(), text.get(), TITLE_TIMES)));
            case NONE -> {
            }
        }
    }

    private void showBars(World world, Supplier<Component> text, float progress, BossBar.Color color,
                          Collection<Player> targets) {
        Set<UUID> now = new HashSet<>();
        for (Player player : targets) {
            Component name = plugin.messages().as(player, text);
            BossBar bar = bars.computeIfAbsent(player.getUniqueId(),
                    id -> BossBar.bossBar(name, progress, color, BossBar.Overlay.NOTCHED_10));
            bar.name(name);
            bar.progress(progress);
            bar.color(color);
            player.showBossBar(bar);
            now.add(player.getUniqueId());
        }
        // Hide for players that no longer belong to the audience (left the world, got up, ...).
        for (UUID uuid : viewers.getOrDefault(world.getUID(), Set.of())) {
            if (!now.contains(uuid)) {
                hideBar(uuid);
            }
        }
        viewers.put(world.getUID(), now);
    }

    private void hideBar(UUID uuid) {
        BossBar bar = bars.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (bar != null && player != null) {
            player.hideBossBar(bar);
        }
    }

    /** Nobody sleeps (any more) in this world. */
    public void hide(World world) {
        Set<UUID> seen = viewers.remove(world.getUID());
        if (seen != null) {
            seen.forEach(this::hideBar);
        }
    }

    public void hideAll() {
        for (World world : Bukkit.getWorlds()) {
            hide(world);
        }
        Set.copyOf(bars.keySet()).forEach(this::hideBar);
        viewers.clear();
    }
}
