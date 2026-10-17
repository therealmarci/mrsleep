package at.marcelr.mrsleep.gui;

import at.marcelr.mrsleep.AdminActions;
import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.Settings;
import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Actions for one player. Layout (5 rows):
 * <pre>
 *  row 0:  . . . . [head] . . . .
 *  row 1:  [cooldown] [bypass1] [bypass2] [bypass3] [remove bypass] [reset cooldown] . . .
 *  row 2:  [ban]      [ban1]    [ban2]    [ban3]    [unban]  . . . .
 *  row 3:  [other]    [release block] [teleport] . . . . . .
 *  row 4:  [back] . . . [close] . . . .
 * </pre>
 */
public final class PlayerMenu extends Menu {

    private static final String PERM_BYPASS = "mrsleep.admin.bypass";
    private static final String PERM_BAN = "mrsleep.admin.ban";
    private static final String PERM_CLEAR = "mrsleep.admin.clear";
    private static final String PERM_TELEPORT = "mrsleep.admin.teleport";

    private final UUID targetId;
    private final MainMenu parent;

    public PlayerMenu(MRSleep plugin, Player viewer, UUID targetId, MainMenu parent) {
        super(plugin, viewer, 5, plugin.messages().get("gui-title-player",
                tag("player", AdminActions.name(Bukkit.getOfflinePlayer(targetId)))));
        this.targetId = targetId;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            // Player left: go back to the overview on the next tick (not while rendering).
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.messages().send(viewer, "gui-player-offline",
                        tag("player", AdminActions.name(Bukkit.getOfflinePlayer(targetId))));
                parent.open();
            });
            return;
        }
        Messages messages = plugin.messages();
        Settings.GuiSettings gui = plugin.settings().gui();
        AdminActions actions = plugin.adminActions();

        set(4, PlayerHead.build(plugin, target, false));

        // ---- cooldown
        set(9, Items.of(Material.AMETHYST_SHARD).name(messages.item("gui-section-cooldown")).build());
        List<Long> bypasses = gui.bypassDurations();
        for (int i = 0; i < bypasses.size(); i++) {
            long duration = bypasses.get(i);
            String time = DurationUtil.format(duration);
            button(10 + i, Material.CLOCK, "gui-bypass-name", "gui-bypass-lore", PERM_BYPASS, false,
                    click -> actions.bypass(viewer, target, duration), tag("time", time));
        }
        boolean hasBypass = plugin.noSleep().bypassRemaining(targetId) > 0;
        if (hasBypass) {
            button(13, Material.MILK_BUCKET, "gui-bypass-remove-name", null, PERM_BYPASS, false,
                    click -> actions.removeBypass(viewer, target));
        }
        button(14, Material.FEATHER, "gui-cooldown-reset-name", null, PERM_BYPASS, false,
                click -> actions.resetCooldown(viewer, target));

        // ---- ban
        set(18, Items.of(Material.IRON_BARS).name(messages.item("gui-section-ban")).build());
        List<Long> bans = gui.banDurations();
        for (int i = 0; i < bans.size(); i++) {
            long duration = bans.get(i);
            boolean permanent = duration == Settings.PERMANENT;
            String time = permanent ? messages.plain("ban-duration-permanent") : DurationUtil.format(duration);
            button(19 + i, permanent ? Material.OBSIDIAN : Material.TRIPWIRE_HOOK, "gui-ban-name", "gui-ban-lore", PERM_BAN, false,
                    click -> actions.ban(viewer, target, duration, gui.banReason()),
                    tag("time", time), tag("player", target.getName()));
        }
        if (plugin.noSleep().ban(targetId) != null) {
            button(22, Material.LIME_DYE, "gui-unban-name", null, PERM_BAN, true,
                    click -> actions.unban(viewer, target));
        }

        // ---- other
        set(27, Items.of(Material.COMPASS).name(messages.item("gui-section-other")).build());
        boolean blocking = plugin.noSleep().remainingNights(target.getWorld(), targetId) > 0;
        if (blocking) {
            button(28, Material.RED_BED, "gui-release-name", "gui-release-lore", PERM_CLEAR, true,
                    click -> actions.release(viewer, target), tag("player", target.getName()));
        } else {
            set(28, Items.of(Material.GRAY_BED).name(messages.item("gui-release-none-name")).build());
        }
        if (target != viewer) {
            button(29, Material.ENDER_PEARL, "gui-teleport-name", null, PERM_TELEPORT, false, click -> {
                viewer.closeInventory();
                viewer.teleportAsync(target.getLocation());
            });
        }

        // ---- navigation
        set(36, Items.of(Material.ARROW).name(messages.item("gui-back")).build(), click -> {
            clickSound();
            parent.open();
        });
        set(40, Items.of(Material.BARRIER).name(messages.item("gui-close")).build(), click -> viewer.closeInventory());

        for (int row = 0; row < 5; row++) {
            fillRow(row);
        }
    }

    /** An action button that checks its permission, runs the action and redraws the menu. */
    private void button(int slot, Material material, String nameKey, String loreKey, String permission, boolean glint,
                        Consumer<ClickType> action, TagResolver... resolvers) {
        Messages messages = plugin.messages();
        Items item = Items.of(material).name(messages.item(nameKey, resolvers)).glint(glint);
        if (loreKey != null) {
            item.lore(messages.lore(loreKey, resolvers));
        }
        if (!viewer.hasPermission(permission)) {
            item.lore(messages.item("gui-no-permission"));
        }
        set(slot, item.build(), click -> {
            if (allowed(permission)) {
                action.accept(click);
                successSound();
                render();
            }
        });
    }
}
