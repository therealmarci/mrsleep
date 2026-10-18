package at.marcelr.mrsleep;

import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.bossbar.BossBar;
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
        DisplaySettings display,
        SleepMessage sleepMessage,
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

    public record DisplaySettings(SleepDisplay.Mode mode, BossBar.Color color, BossBar.Color blockedColor) {
    }

    /** Who receives "<player> hat sich schlafen gelegt". */
    public enum SleepMessage { WORLD, SERVER, NONE }

    public record UpdateSettings(boolean enabled, String source, boolean includePrereleases,
                                 int intervalHours, boolean notifyOnJoin) {
    }

    /** Durations in ms; ban durations may contain {@link #PERMANENT}. */
    public record GuiSettings(int refreshTicks, List<Long> bypassDurations, List<Long> banDurations,
                              boolean sounds) {
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
                c.getBoolean("gui.sounds", true));

        return new Settings(
                percentage,
                Math.max(1, c.getInt("sleep.minimum-sleepers", 1)),
                c.getBoolean("sleep.ignore-spectators", true),
                c.getBoolean("sleep.respect-sleeping-ignored", true),
                c.getBoolean("sleep.clear-weather", true),
                new DisplaySettings(
                        parseEnum(c.getString("sleep.display"), SleepDisplay.Mode.BOSSBAR, "sleep.display", log),
                        parseEnum(c.getString("sleep.bossbar-color"), BossBar.Color.YELLOW, "sleep.bossbar-color", log),
                        parseEnum(c.getString("sleep.bossbar-blocked-color"), BossBar.Color.RED, "sleep.bossbar-blocked-color", log)),
                parseEnum(c.getString("sleep.sleep-message"), SleepMessage.WORLD, "sleep.sleep-message", log),
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

    /** Case-insensitive enum value, e.g. "bossbar" or "YELLOW"; falls back to {@code fallback} with a warning. */
    private static <E extends Enum<E>> E parseEnum(String raw, E fallback, String path, Logger log) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning("Ungültiger Wert '" + raw + "' für " + path + ", verwende " + fallback.name().toLowerCase(Locale.ROOT) + ".");
            return fallback;
        }
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
