package at.marcelr.mrsleep;

import at.marcelr.mrsleep.api.MRadminLink;
import at.marcelr.mrsleep.command.MRSleepCommand;
import at.marcelr.mrsleep.command.NoSleepCommand;
import at.marcelr.mrsleep.gui.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Main class of MRsleep: loads config and texts and starts the sleep vote, /nosleep and the MRadmin link. */
public final class MRSleep extends JavaPlugin {

    private FileConfiguration config;
    private Settings settings;
    // lang-version 2: the version display got the Minecraft version.
    private final Messages messages = new Messages(4, Map.of(2, Set.of("version", "gui-info-name"),
            // 3: "Website | GitHub" in /mrsleep version.
            3, Set.of("version-links"),
            // 4: short chat messages, details on hover.
            4, Set.of(
                    "night-start-blocked", "bed-prevented", "bed-blocked-warning", "bed-excluded", "join-blocked-info",
                    "nosleep-activated", "nosleep-deactivated-free", "nosleep-deactivated-cooldown",
                    "nosleep-pending-info", "nosleep-cooldown-started", "nosleep-invalid-nights", "nosleep-cooldown",
                    "nosleep-banned", "nosleep-broadcast-quit", "nosleep-broadcast-released", "nosleep-expired", "status",
                    "cleared-broadcast", "bypass-set", "limit-bypass", "bypass-granted-notify", "bypass-removed-notify",
                    "ban-set", "limit-ban", "ban-notify", "debug-world", "discord-status-active", "discord-test-hint")));
    private final DiscordWebhook discord = new DiscordWebhook(this);
    private final AdminActions adminActions = new AdminActions(this);
    private UpdateChecker updateChecker;
    private MenuListener menuListener;
    private NoSleepManager noSleep;
    private SleepManager sleep;
    private MRadminLink mradmin;

    @Override
    public void onEnable() {
        ConfigUpdater.update(this);
        loadConfiguration();

        // Optional: MRadmin (module API) - dashboard, player menu, web interface, admin mode for the quota.
        mradmin = new MRadminLink(this);
        getServer().getPluginManager().registerEvents(mradmin, this);
        mradmin.connect();
        noSleep = new NoSleepManager(this);
        noSleep.load();
        sleep = new SleepManager(this);
        sleep.start();
        updateChecker = new UpdateChecker(this);
        updateChecker.start();
        menuListener = new MenuListener(this);
        menuListener.start();

        getServer().getPluginManager().registerEvents(new SleepListener(this), this);
        getServer().getPluginManager().registerEvents(menuListener, this);
        register("nosleep", new NoSleepCommand(this));
        register("mrsleep", new MRSleepCommand(this));
    }

    @Override
    public void onDisable() {
        if (menuListener != null) {
            menuListener.stop();
            menuListener.closeAll();
        }
        if (updateChecker != null) {
            updateChecker.stop();
        }
        if (sleep != null) {
            sleep.stop();
        }
        if (noSleep != null) {
            noSleep.save();
        }
        discord.shutdown();
    }

    /** Reloads config.yml (settings, messages, Discord, update check, menu refresh) and re-applies the gamerule. */
    public void reload() {
        ConfigUpdater.update(this);
        loadConfiguration();
        sleep.resetDisplay();
        Bukkit.getWorlds().forEach(sleep::applyGameRule);
        updateChecker.start();
        menuListener.start();
    }

    private void loadConfiguration() {
        config = ConfigUpdater.load(this);
        settings = Settings.from(config, getLogger());
        messages.load(this, config.getString("language", "auto"), config.getString("fallback-language", "en"),
                config.getString("server-language", "en"));
        discord.load(config.getConfigurationSection("discord"));
    }

    /** config.yml is read by {@link ConfigUpdater#load}, which also tolerates the UTF-8 BOM. */
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            config = ConfigUpdater.load(this);
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        config = ConfigUpdater.load(this);
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), name);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public DiscordWebhook discord() {
        return discord;
    }

    public AdminActions adminActions() {
        return adminActions;
    }

    public UpdateChecker updateChecker() {
        return updateChecker;
    }

    public NoSleepManager noSleep() {
        return noSleep;
    }

    public SleepManager sleep() {
        return sleep;
    }

    /** The optional connection to MRadmin. */
    public MRadminLink mradmin() {
        return mradmin;
    }
}
