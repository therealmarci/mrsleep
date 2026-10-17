package at.marcelr.mrsleep;

import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Formatted status values of a player (counts, sleeping, blocking, cooldown, ban), shared by the admin menu
 * and {@code /nosleep info}, so both always show the same. Texts come from {@code messages.gui-value-*}.
 */
public final class StatusTexts {

    private StatusTexts() {
    }

    public static Component counts(MRSleep plugin, Player player) {
        Messages messages = plugin.messages();
        String reason = plugin.sleep().exclusionReason(player);
        return reason == null
                ? messages.get("gui-value-counts-yes")
                : messages.get("gui-value-counts-no", tag("reason", messages.plain(reason)));
    }

    public static Component sleeping(MRSleep plugin, Player player) {
        Messages messages = plugin.messages();
        return player.isSleeping()
                ? messages.get("gui-value-sleeping-yes",
                        tag("seconds", String.format(Locale.ROOT, "%.1f", Math.min(player.getSleepTicks(), 100) / 20.0)))
                : messages.get("gui-value-sleeping-no");
    }

    /** @return the player's own block in their current world */
    public static Component blocking(MRSleep plugin, Player player) {
        Messages messages = plugin.messages();
        World world = player.getWorld();
        int nights = plugin.sleep().isManaged(world) ? plugin.noSleep().remainingNights(world, player.getUniqueId()) : 0;
        return nights > 0
                ? messages.get("gui-value-blocking-yes", tag("nights", messages.nights(nights)))
                : messages.get("gui-value-blocking-no");
    }

    public static Component cooldown(MRSleep plugin, Player player) {
        Messages messages = plugin.messages();
        NoSleepManager noSleep = plugin.noSleep();
        long bypass = noSleep.bypassRemaining(player.getUniqueId());
        if (player.hasPermission(NoSleepManager.PERM_NO_COOLDOWN)) {
            return messages.get("gui-value-cooldown-permission");
        }
        if (bypass > 0) {
            return messages.get("gui-value-cooldown-bypass", tag("time", DurationUtil.format(bypass)));
        }
        if (noSleep.isPendingAnywhere(player.getUniqueId())) {
            return messages.get("gui-value-cooldown-pending");
        }
        long remaining = noSleep.remainingCooldown(player);
        return remaining > 0
                ? messages.get("gui-value-cooldown-remaining", tag("time", DurationUtil.format(remaining)))
                : messages.get("gui-value-cooldown-ready");
    }

    public static Component ban(MRSleep plugin, Player player) {
        Messages messages = plugin.messages();
        NoSleepManager.Ban ban = plugin.noSleep().ban(player.getUniqueId());
        return ban == null
                ? messages.get("gui-value-ban-none")
                : messages.get("gui-value-ban-active",
                        tag("duration", plugin.adminActions().banDuration(ban)), tag("reason", ban.reason()));
    }
}
