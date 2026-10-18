package at.marcelr.mrsleep.api;

import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.SleepManager;
import at.marcelr.mrsleep.gui.PlayerHead;
import at.marcelr.mrsleep.gui.PlayerMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Connection point for MRadmin's admin compass. MRadmin finds this class at runtime (no dependency in either
 * direction) and calls these methods; MRsleep works the same without MRadmin. Keep the signatures stable and raise
 * {@link #API_VERSION} only together with MRadmin.
 */
public final class MRadminHook {

    public static final int API_VERSION = 1;
    private static final String PERM_GUI = "mrsleep.admin.gui";
    private static final int MAX_WORLDS = 3;

    private MRadminHook() {
    }

    private static MRSleep plugin() {
        return JavaPlugin.getPlugin(MRSleep.class);
    }

    /** Lines for the MRsleep button: sleep status per world (up to three), active /nosleep and bans. */
    public static List<Component> summary(Player viewer) {
        MRSleep plugin = plugin();
        return plugin.messages().as(viewer, () -> {
            var messages = plugin.messages();
            List<Component> lines = new ArrayList<>();
            int shown = 0;
            int blocked = 0;
            for (World world : Bukkit.getWorlds()) {
                if (!plugin.sleep().isManaged(world)) {
                    continue;
                }
                blocked += plugin.noSleep().activeBlockers(world).size();
                if (shown++ < MAX_WORLDS) {
                    SleepManager.Status status = plugin.sleep().status(world);
                    lines.addAll(messages.lore("mradmin-summary-world",
                            tag("world", world.getName()),
                            tag("sleeping", status.sleeping()),
                            tag("needed", status.needed()),
                            tag("phase", messages.plain(SleepManager.isNight(world) ? "gui-phase-night" : "gui-phase-day"))));
                }
            }
            lines.addAll(messages.lore("mradmin-summary",
                    tag("blocked", blocked),
                    tag("bans", plugin.noSleep().activeBans().size())));
            return lines;
        });
    }

    /** The same lines as on the player's head in the admin menu. Empty if the player is offline. */
    public static List<Component> playerSummary(Player viewer, UUID target) {
        MRSleep plugin = plugin();
        Player online = Bukkit.getPlayer(target);
        if (online == null) {
            return List.of();
        }
        return plugin.messages().as(viewer, () -> {
            ItemMeta meta = PlayerHead.build(plugin, online, false).getItemMeta();
            List<Component> lore = meta == null ? null : meta.lore();
            return lore == null ? List.of() : lore;
        });
    }

    /** Opens the player's MRsleep menu; "back" returns to MRadmin. @return false without permission or offline */
    public static boolean openPlayerMenu(Player viewer, UUID target, Runnable back) {
        MRSleep plugin = plugin();
        if (!viewer.hasPermission(PERM_GUI)) {
            plugin.messages().send(viewer, "no-permission");
            return false;
        }
        if (Bukkit.getPlayer(target) == null) {
            return false;
        }
        new PlayerMenu(plugin, viewer, target, back).open();
        return true;
    }
}
