package at.marcelr.mrsleep;

import at.marcelr.mrsleep.util.DurationUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public record Settings(
        int percentage,
        int minimumSleepers,
        boolean ignoreSpectators,
        boolean respectSleepingIgnored,
        boolean clearWeather,
        boolean actionBar,
        boolean announceSkip,
        Set<String> disabledWorlds,
        long cooldownMillis,
        int maxNights,
        boolean preventBedEnter,
        boolean releaseOnQuit,
        boolean broadcast,
        UpdateSettings update,
        GuiSettings gui
) {

    /** Marker for a permanent ban in {@link GuiSettings#banDurations()}. */
    public static final long PERMANENT = -1L;

    private static final long DEFAULT_COOLDOWN = 15 * 60_000L;

    public record UpdateSettings(boolean enabled, String source, boolean includePrereleases,
                                 int intervalHours, boolean notifyOnJoin) {
    }

    /** Durations in ms; ban durations may contain {@link #PERMANENT}. */
    public record GuiSettings(int refreshTicks, List<Long> bypassDurations, List<Long> banDurations,
                              String banReason, boolean sounds) {
    }

    public static Settings from(FileConfiguration c, Logger log) {
        int percentage = Math.clamp(c.getInt("sleep.percentage", 50), 0, 100);

        String rawCooldown = c.getString("nosleep.cooldown", "15m");
        long cooldown = DurationUtil.parse(rawCooldown);
        if (cooldown < 0) {
            log.warning("Ungültiger nosleep.cooldown '" + rawCooldown + "', verwende 15m.");
            cooldown = DEFAULT_COOLDOWN;
        }

        Set<String> disabled = c.getStringList("sleep.disabled-worlds").stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        String source = c.getString("update-checker.source", "github").toLowerCase(Locale.ROOT);
        if (!source.equals("modrinth") && !source.equals("github")) {
            log.warning("Ungültige update-checker.source '" + source + "', verwende github.");
            source = "github";
        }
        UpdateSettings update = new UpdateSettings(
                c.getBoolean("update-checker.enabled", true),
                source,
                c.getBoolean("update-checker.include-prereleases", false),
                Math.max(1, c.getInt("update-checker.interval-hours", 6)),
                c.getBoolean("update-checker.notify-on-join", true));

        GuiSettings gui = new GuiSettings(
                Math.max(5, c.getInt("gui.refresh-ticks", 20)),
                durations(c.getStringList("gui.bypass-durations"), List.of("30m", "2h", "1d"), false, log),
                durations(c.getStringList("gui.ban-durations"), List.of("1h", "1d", "permanent"), true, log),
                c.getString("gui.ban-reason", "über das Admin-Menü"),
                c.getBoolean("gui.sounds", true));

        return new Settings(
                percentage,
                Math.max(1, c.getInt("sleep.minimum-sleepers", 1)),
                c.getBoolean("sleep.ignore-spectators", true),
                c.getBoolean("sleep.respect-sleeping-ignored", true),
                c.getBoolean("sleep.clear-weather", true),
                c.getBoolean("sleep.action-bar", true),
                c.getBoolean("sleep.announce-skip", true),
                disabled,
                cooldown,
                Math.max(1, c.getInt("nosleep.max-nights", 3)),
                c.getBoolean("nosleep.prevent-bed-enter", false),
                c.getBoolean("nosleep.release-on-quit", true),
                c.getBoolean("nosleep.broadcast", true),
                update,
                gui
        );
    }

    /** Parses exactly three quick-select durations, falling back to the defaults for invalid entries. */
    private static List<Long> durations(List<String> raw, List<String> defaults, boolean allowPermanent, Logger log) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String value = i < raw.size() ? raw.get(i) : defaults.get(i);
            long parsed = allowPermanent && value.equalsIgnoreCase("permanent") ? PERMANENT : DurationUtil.parse(value);
            if (parsed == 0 || (parsed < 0 && parsed != PERMANENT)) {
                log.warning("Ungültige Dauer '" + value + "' im gui-Abschnitt, verwende " + defaults.get(i) + ".");
                String fallback = defaults.get(i);
                parsed = fallback.equals("permanent") ? PERMANENT : DurationUtil.parse(fallback);
            }
            result.add(parsed);
        }
        return List.copyOf(result);
    }
}
