package at.marcelr.mrsleep.api;

import at.marcelr.mradmin.api.AdminModule;
import at.marcelr.mradmin.api.MRadminService;
import at.marcelr.mradmin.api.WebAlert;
import at.marcelr.mradmin.api.WebModule;
import at.marcelr.mradmin.api.WebUser;
import at.marcelr.mrsleep.AdminActions;
import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.SleepManager;
import at.marcelr.mrsleep.gui.PlayerHead;
import at.marcelr.mrsleep.gui.PlayerMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * MRsleep as a module of MRadmin (module API, see MRadmin's docs/MODULE-API.md): sleep status on the dashboard button,
 * the player's sleep info in the player menu and the sleep pages of the web interface. <b>Only loaded while MRadmin
 * runs</b> - everything else in MRsleep goes through {@link MRadminLink}, which has no MRadmin types.
 */
final class MRadminModule implements AdminModule, WebModule {

    static final String ID = "mrsleep";
    private static final String PERM_GUI = "mrsleep.admin.gui";
    private static final int MAX_WORLDS = 3;

    /** The bypass and ban levels: the web sends the action of the level the duration needs (limits in config.yml). */
    private static final Map<String, String> PERMISSIONS = Map.of(
            "clear", "mrsleep.admin.clear", "bypass", AdminActions.PERM_BYPASS, "ban", AdminActions.PERM_BAN,
            "bypassStaff", AdminActions.PERM_BYPASS_STAFF, "bypassUnlimited", AdminActions.PERM_BYPASS_UNLIMITED,
            "banStaff", AdminActions.PERM_BAN_STAFF, "banUnlimited", AdminActions.PERM_BAN_UNLIMITED,
            "reset", AdminActions.PERM_RESET);
    private static final Map<String, String> ACTIONS = Map.ofEntries(
            Map.entry("clear", "clear"), Map.entry("release", "clear"), Map.entry("bypass", "bypass"),
            Map.entry("bypassStaff", "bypassStaff"), Map.entry("bypassUnlimited", "bypassUnlimited"),
            Map.entry("removeBypass", "bypass"), Map.entry("resetCooldown", "reset"), Map.entry("ban", "ban"),
            Map.entry("banStaff", "banStaff"), Map.entry("banUnlimited", "banUnlimited"), Map.entry("unban", "ban"));

    private final MRSleep plugin;

    private MRadminModule(MRSleep plugin) {
        this.plugin = plugin;
    }

    /** Registers the module with the running MRadmin. @return false if MRadmin offers no module API */
    static boolean register(MRSleep plugin) {
        MRadminService service = MRadminService.get();
        if (service == null) {
            return false;
        }
        service.register(new MRadminModule(plugin));
        return true;
    }

    /** Whether the player is in MRadmin's admin mode. */
    static boolean adminMode(Player player) {
        MRadminService service = MRadminService.get();
        return service != null && service.adminMode(player);
    }

    /** Another module has the player busy (e.g. MRpvp during a duel). */
    static boolean busyElsewhere(Player player) {
        MRadminService service = MRadminService.get();
        String module = service == null ? null : service.busy(player);
        return module != null && !module.equals(ID);
    }

    /** The world belongs to another module (e.g. MRpvp's arena world). */
    static boolean reservedWorld(World world) {
        MRadminService service = MRadminService.get();
        String module = service == null ? null : service.worldOwner(world);
        return module != null && !module.equals(ID);
    }

    // -------------------------------------------------------------- module

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public String viewPermission() {
        return PERM_GUI;
    }

    @Override
    public Material icon() {
        return Material.RED_BED;
    }

    /** Sleep status per world (up to three), active /nosleep blocks and bans. */
    @Override
    public List<Component> summary(Player viewer) {
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

    /** The same lines as on the player's head in MRsleep's admin menu. Empty if the player is offline. */
    @Override
    public List<Component> playerSummary(Player viewer, UUID player) {
        Player online = Bukkit.getPlayer(player);
        if (online == null) {
            return List.of();
        }
        return plugin.messages().as(viewer, () -> {
            ItemMeta meta = PlayerHead.build(plugin, online, false).getItemMeta();
            List<Component> lore = meta == null ? null : meta.lore();
            return lore == null ? List.of() : lore;
        });
    }

    @Override
    public boolean openMenu(Player viewer, Runnable back) {
        if (!viewer.hasPermission(PERM_GUI)) {
            plugin.messages().send(viewer, "no-permission");
            return false;
        }
        plugin.messages().run(viewer, () -> new at.marcelr.mrsleep.gui.MainMenu(plugin, viewer).open());
        return true;
    }

    @Override
    public boolean openPlayerMenu(Player viewer, UUID player, Runnable back) {
        if (!viewer.hasPermission(PERM_GUI)) {
            plugin.messages().send(viewer, "no-permission");
            return false;
        }
        if (Bukkit.getPlayer(player) == null) {
            return false;
        }
        plugin.messages().run(viewer, () -> new PlayerMenu(plugin, viewer, player, back).open());
        return true;
    }

    // ----------------------------------------------------------------- web

    @Override
    public WebModule web() {
        return this;
    }

    @Override
    public Map<String, String> permissions() {
        return PERMISSIONS;
    }

    @Override
    public @Nullable Object data(String request, Map<String, String> args) {
        return request.equals("overview") ? WebData.handle(plugin, request, args) : null;
    }

    @Override
    public Map<String, String> actions() {
        return ACTIONS;
    }

    /** {@code clear} etc. with uuid or world, {@code duration} in ms, {@code reason}. */
    @Override
    public @Nullable String action(String action, Map<String, Object> args, WebUser user) {
        Map<String, Object> all = new HashMap<>(args);
        all.put("action", action);
        all.put("admin", user.name());
        // The web sends numbers as text.
        if (all.get("duration") instanceof String text) {
            try {
                all.put("duration", Long.parseLong(text));
            } catch (NumberFormatException e) {
                return "invalid-duration";
            }
        }
        Object error = WebData.handle(plugin, "action", all);
        return error == null ? null : String.valueOf(error);
    }

    @Override
    public @Nullable Object player(UUID player) {
        return WebData.handle(plugin, "player", Map.of("uuid", player.toString()));
    }

    /** /nosleep blocks right now. */
    @Override
    public List<WebAlert> alerts() {
        int blocked = 0;
        for (World world : Bukkit.getWorlds()) {
            if (plugin.sleep().isManaged(world)) {
                blocked += plugin.noSleep().activeBlockers(world).size();
            }
        }
        return blocked == 0 ? List.of() : List.of(new WebAlert("info",
                plugin.messages().plain("web-alert-blocked", "count", String.valueOf(blocked)), "/sleep"));
    }

    @Override
    public Map<UUID, List<String>> flags() {
        Map<UUID, List<String>> flags = new LinkedHashMap<>();
        if (WebData.handle(plugin, "flags", Map.of()) instanceof Map<?, ?> answer) {
            answer.forEach((uuid, keys) -> {
                if (keys instanceof List<?> list) {
                    flags.put(UUID.fromString(String.valueOf(uuid)), list.stream().map(String::valueOf).toList());
                }
            });
        }
        return flags;
    }
}
