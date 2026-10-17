package at.marcelr.mrsleep.gui;

import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.NoSleepManager;
import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

import static at.marcelr.mrsleep.Messages.tag;

/** Builds a player head showing that player's live sleep / nosleep status. */
final class PlayerHead {

    /** Sort rank: blockers first, then sleepers, counting players, excluded players. */
    static int rank(MRSleep plugin, Player player) {
        if (plugin.noSleep().remainingNights(player.getWorld(), player.getUniqueId()) > 0) {
            return 0;
        }
        if (player.isSleeping()) {
            return 1;
        }
        return plugin.sleep().isExcluded(player) ? 3 : 2;
    }

    static ItemStack build(MRSleep plugin, Player target, boolean clickHint) {
        Messages messages = plugin.messages();
        NoSleepManager noSleep = plugin.noSleep();
        World world = target.getWorld();
        boolean managed = plugin.sleep().isManaged(world);

        String reason = plugin.sleep().exclusionReason(target);
        Component counts = reason == null
                ? messages.get("gui-value-counts-yes")
                : messages.get("gui-value-counts-no", tag("reason", messages.plain(reason)));

        Component sleeping = target.isSleeping()
                ? messages.get("gui-value-sleeping-yes",
                        tag("seconds", String.format(Locale.ROOT, "%.1f", Math.min(target.getSleepTicks(), 100) / 20.0)))
                : messages.get("gui-value-sleeping-no");

        int nights = managed ? noSleep.remainingNights(world, target.getUniqueId()) : 0;
        Component blocking = nights > 0
                ? messages.get("gui-value-blocking-yes", tag("nights", messages.nights(nights)))
                : messages.get("gui-value-blocking-no");

        Component cooldown;
        long bypass = noSleep.bypassRemaining(target.getUniqueId());
        if (target.hasPermission(NoSleepManager.PERM_NO_COOLDOWN)) {
            cooldown = messages.get("gui-value-cooldown-permission");
        } else if (bypass > 0) {
            cooldown = messages.get("gui-value-cooldown-bypass", tag("time", DurationUtil.format(bypass)));
        } else {
            long remaining = noSleep.remainingCooldown(target);
            cooldown = remaining > 0
                    ? messages.get("gui-value-cooldown-remaining", tag("time", DurationUtil.format(remaining)))
                    : messages.get("gui-value-cooldown-ready");
        }

        NoSleepManager.Ban ban = noSleep.ban(target.getUniqueId());
        Component banText = ban == null
                ? messages.get("gui-value-ban-none")
                : messages.get("gui-value-ban-active",
                        tag("duration", plugin.adminActions().banDuration(ban)), tag("reason", ban.reason()));

        Items item = Items.head(target)
                .name(messages.item("gui-head-name", tag("player", target.getName())))
                .lore(messages.lore("gui-head-lore",
                        tag("world", world.getName()),
                        tag("counts", counts),
                        tag("sleeping", sleeping),
                        tag("blocking", blocking),
                        tag("cooldown", cooldown),
                        tag("ban", banText)))
                .glint(nights > 0);
        if (clickHint) {
            item.lore(Component.empty()).lore(messages.item("gui-head-click"));
        }
        return item.build();
    }

    private PlayerHead() {
    }
}
