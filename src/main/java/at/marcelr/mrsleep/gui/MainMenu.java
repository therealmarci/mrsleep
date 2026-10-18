package at.marcelr.mrsleep.gui;

import at.marcelr.mrsleep.MRSleep;
import at.marcelr.mrsleep.Messages;
import at.marcelr.mrsleep.NoSleepManager;
import at.marcelr.mrsleep.SleepManager;
import at.marcelr.mrsleep.UpdateChecker;
import at.marcelr.mrsleep.util.DurationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static at.marcelr.mrsleep.Messages.tag;

/**
 * Overview: world status on top, one head per online player in the middle (sorted, filterable, paged),
 * navigation and global actions at the bottom.
 */
public final class MainMenu extends Menu {

    private static final int ROWS = 6;
    private static final int HEADS_PER_PAGE = 36;
    private static final int FIRST_HEAD_SLOT = 9;

    enum Filter {
        ALL("gui-filter-all"),
        SLEEPING("gui-filter-sleeping"),
        BLOCKING("gui-filter-blocking"),
        EXCLUDED("gui-filter-excluded"),
        BANNED("gui-filter-banned");

        final String key;

        Filter(String key) {
            this.key = key;
        }

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private int worldIndex;
    private int page;
    private Filter filter = Filter.ALL;

    public MainMenu(MRSleep plugin, Player viewer) {
        super(plugin, viewer, ROWS, plugin.messages().get("gui-title-main"));
        // Start with the viewer's own world if it is managed.
        List<World> worlds = managedWorlds();
        worldIndex = Math.max(0, worlds.indexOf(viewer.getWorld()));
    }

    private List<World> managedWorlds() {
        List<World> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (plugin.sleep().isManaged(world)) {
                worlds.add(world);
            }
        }
        return worlds;
    }

    @Override
    protected void draw() {
        Messages messages = plugin.messages();
        List<World> worlds = managedWorlds();

        // ---- top row: world, info, clear
        if (!worlds.isEmpty()) {
            worldIndex = Math.floorMod(worldIndex, worlds.size());
            World world = worlds.get(worldIndex);
            drawWorld(world, worlds.size() > 1);
            drawClear(world);
        }
        drawInfo();
        fillRow(0);

        // ---- heads
        List<Player> players = filteredPlayers();
        int pages = Math.max(1, (players.size() + HEADS_PER_PAGE - 1) / HEADS_PER_PAGE);
        page = Math.min(page, pages - 1);
        int from = page * HEADS_PER_PAGE;
        for (int i = 0; i < HEADS_PER_PAGE && from + i < players.size(); i++) {
            Player target = players.get(from + i);
            set(FIRST_HEAD_SLOT + i, PlayerHead.build(plugin, target, true), click -> {
                clickSound();
                new PlayerMenu(plugin, viewer, target.getUniqueId(), this).open();
            });
        }
        if (players.isEmpty()) {
            set(22, Items.of(Material.STRUCTURE_VOID).name(messages.item("gui-empty")).build());
        }

        // ---- bottom row: navigation
        var pageLore = messages.lore("gui-page-lore", tag("page", page + 1), tag("pages", pages));
        if (page > 0) {
            set(45, Items.of(Material.ARROW).name(messages.item("gui-prev")).lore(pageLore).build(), click -> {
                page--;
                clickSound();
                render();
            });
        }
        if (page < pages - 1) {
            set(53, Items.of(Material.ARROW).name(messages.item("gui-next")).lore(pageLore).build(), click -> {
                page++;
                clickSound();
                render();
            });
        }
        set(47, Items.of(Material.HOPPER)
                .name(messages.item("gui-filter-name", tag("filter", messages.plain(filter.key))))
                .lore(messages.lore("gui-filter-lore"))
                .build(), click -> {
            filter = filter.next();
            page = 0;
            clickSound();
            render();
        });
        set(49, Items.of(Material.BARRIER).name(messages.item("gui-close")).build(), click -> viewer.closeInventory());

        Items reload = Items.of(Material.COMPARATOR)
                .name(messages.item("gui-reload-name"))
                .lore(messages.lore("gui-reload-lore"));
        if (!viewer.hasPermission("mrsleep.admin.reload")) {
            reload.lore(messages.item("gui-no-permission"));
        }
        set(51, reload.build(), click -> {
            if (allowed("mrsleep.admin.reload")) {
                plugin.adminActions().reload(viewer);
                successSound();
                render();
            }
        });
        fillRow(5);
    }

    private void drawWorld(World world, boolean switchable) {
        Messages messages = plugin.messages();
        SleepManager.Status status = plugin.sleep().status(world);
        Map<UUID, Integer> blockers = plugin.noSleep().activeBlockers(world);
        boolean night = SleepManager.isNight(world);

        Items item = Items.of(night ? Material.RECOVERY_COMPASS : Material.CLOCK)
                .name(messages.item("gui-world-name", tag("world", world.getName())))
                .lore(messages.lore("gui-world-lore",
                        tag("clock", clock(world.getTime())),
                        tag("phase", messages.plain(night ? "gui-phase-night" : "gui-phase-day")),
                        tag("counted", status.counted()),
                        tag("exempt", status.exempt()),
                        tag("sleeping", status.sleeping()),
                        tag("needed", status.needed()),
                        tag("deep", status.deepSleeping()),
                        tag("players", blockers.isEmpty() ? messages.plain("status-none")
                                : NoSleepManager.names(blockers.keySet()))));
        if (switchable) {
            item.lore(Component.empty()).lore(messages.item("gui-world-switch"));
        }
        set(1, item.build(), click -> {
            if (switchable) {
                worldIndex++;
                clickSound();
                render();
            }
        });
    }

    private void drawClear(World world) {
        Messages messages = plugin.messages();
        if (!plugin.noSleep().isBlocked(world)) {
            set(7, Items.of(Material.LIME_DYE).name(messages.item("gui-clear-none-name")).build());
            return;
        }
        Items item = Items.of(Material.TNT)
                .name(messages.item("gui-clear-name"))
                .lore(messages.lore("gui-clear-lore", tag("world", world.getName())));
        if (!viewer.hasPermission("mrsleep.admin.clear")) {
            item.lore(messages.item("gui-no-permission"));
        }
        set(7, item.build(), click -> {
            if (allowed("mrsleep.admin.clear")) {
                plugin.adminActions().clear(viewer, world);
                successSound();
                render();
            }
        });
    }

    private void drawInfo() {
        Messages messages = plugin.messages();
        UpdateChecker updates = plugin.updateChecker();
        Component update = switch (updates.state()) {
            case UP_TO_DATE -> messages.get("gui-update-ok");
            case OUTDATED -> messages.get("gui-update-outdated", tag("latest", updates.latestVersion()));
            default -> messages.get("gui-update-unknown");
        };
        set(4, Items.of(Material.KNOWLEDGE_BOOK)
                .name(messages.item("gui-info-name", tag("version", updates.currentVersion()), tag("mc", updates.minecraftVersion())))
                .lore(messages.lore("gui-info-lore",
                        tag("update", update),
                        tag("percentage", plugin.settings().percentage()),
                        tag("cooldown", DurationUtil.format(plugin.settings().cooldownMillis()))))
                .build());
    }

    private List<Player> filteredPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean include = switch (filter) {
                case ALL -> true;
                case SLEEPING -> player.isSleeping();
                case BLOCKING -> plugin.noSleep().remainingNights(player.getWorld(), player.getUniqueId()) > 0;
                case EXCLUDED -> plugin.sleep().isExcluded(player);
                case BANNED -> plugin.noSleep().ban(player.getUniqueId()) != null;
            };
            if (include) {
                players.add(player);
            }
        }
        players.sort(Comparator.<Player>comparingInt(p -> PlayerHead.rank(plugin, p))
                .thenComparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        return players;
    }

    /** Minecraft ticks to a 24h clock (tick 0 = 06:00). */
    static String clock(long ticks) {
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }
}
