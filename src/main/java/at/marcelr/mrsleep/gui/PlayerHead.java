package at.marcelr.mrsleep.gui;

import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.StatusTexts;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        World world = target.getWorld();
        boolean blocking = plugin.sleep().isManaged(world)
                && plugin.noSleep().remainingNights(world, target.getUniqueId()) > 0;

        Items item = Items.head(target)
                .name(messages.item("gui-head-name", tag("player", target.getName())))
                .lore(messages.lore("gui-head-lore",
                        tag("world", world.getName()),
                        tag("counts", StatusTexts.counts(plugin, target)),
                        tag("sleeping", StatusTexts.sleeping(plugin, target)),
                        tag("blocking", StatusTexts.blocking(plugin, target)),
                        tag("cooldown", StatusTexts.cooldown(plugin, target)),
                        tag("ban", StatusTexts.ban(plugin, target))))
                .glint(blocking);
        if (clickHint) {
            item.lore(Component.empty()).lore(messages.item("gui-head-click"));
        }
        return item.build();
    }

    private PlayerHead() {
    }
}
