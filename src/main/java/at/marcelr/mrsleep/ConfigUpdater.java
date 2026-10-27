package at.marcelr.mrsleep;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Brings an existing config.yml up to the layout of the bundled one: the bundled file (with its comments and order)
 * is taken as template and every value the server owner had set is carried over. The old file is kept as backup.
 * <p>
 * Files are always written as UTF-8 <b>with BOM</b>, so editors like Notepad++, WinSCP or FileZilla
 * recognise the encoding and show umlauts correctly.
 */
public final class ConfigUpdater {

    public static final int CURRENT_VERSION = 8;
    public static final String BOM = "﻿";
    /**
     * Texts whose content changed with new features; the new default replaces the old value.
     * Since v6 the texts live in lang/*.yml; the keys are still used when old messages are moved there.
     */
    private static final Set<String> REPLACED_KEYS = Set.of(
            "messages.admin-usage",
            "messages.debug-player",
            // v3
            "messages.version-latest",
            "messages.gui-info-lore",
            "messages.gui-update-ok",
            "messages.gui-update-outdated",
            "messages.gui-update-unknown",
            // v4
            "messages.nosleep-invalid-nights");
    /** Keys that were renamed: old path -> new path (the value is carried over). */
    private static final Map<String, String> RENAMED_KEYS = Map.of(
            "messages.actionbar-sleeping", "messages.display-sleeping",
            "messages.actionbar-blocked", "messages.display-blocked");

    private ConfigUpdater() {
    }

    public static void update(MRSleep plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();
            return;
        }
        try {
            String current = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            YamlConfiguration existing = new YamlConfiguration();
            existing.loadFromString(stripBom(current));
            int version = existing.getInt("config-version", 1);
            if (version >= CURRENT_VERSION) {
                if (!current.startsWith(BOM)) {
                    // Same content, only add the BOM so editors detect UTF-8.
                    Files.writeString(file.toPath(), BOM + current, StandardCharsets.UTF_8);
                }
                return;
            }

            if (version < 6) {
                migrateTexts(plugin, existing);
            }

            YamlConfiguration template = new YamlConfiguration();
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) {
                    return;
                }
                template.loadFromString(stripBom(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            }

            int carried = 0;
            for (String oldKey : existing.getKeys(true)) {
                String key = RENAMED_KEYS.getOrDefault(oldKey, oldKey);
                if (existing.isConfigurationSection(oldKey) || key.equals("config-version")
                        || REPLACED_KEYS.contains(key) || !template.contains(key)) {
                    continue;
                }
                template.set(key, existing.get(oldKey));
                carried++;
            }
            // v3: "sleep.action-bar: false" became "sleep.display: none".
            if (existing.contains("sleep.action-bar") && !existing.getBoolean("sleep.action-bar")) {
                template.set("sleep.display", "none");
            }
            template.set("config-version", CURRENT_VERSION);
            // Keep long messages on one line instead of folding them.
            template.options().width(10_000);

            File backup = new File(plugin.getDataFolder(), "config.v" + version + ".backup.yml");
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(file.toPath(), BOM + template.saveToString(), StandardCharsets.UTF_8);
            plugin.getLogger().info("Updated config.yml from version " + version + " to " + CURRENT_VERSION + " ("
                    + carried + " settings carried over, backup: " + backup.getName() + ").");
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Could not update config.yml: " + e.getMessage());
        }
    }

    /**
     * v6: the texts moved from config.yml (messages, discord.events.*.message, discord.test-message, gui.ban-reason)
     * into lang/de.yml. The old texts were German, so they are carried over into the German file - including
     * anything the server owner had customised.
     */
    private static void migrateTexts(MRSleep plugin, YamlConfiguration old) throws IOException {
        YamlConfiguration lang = Messages.bundled(plugin, "de");
        File file = new File(plugin.getDataFolder(), "lang/de.yml");
        if (file.exists()) {
            try {
                lang.loadFromString(stripBom(Files.readString(file.toPath(), StandardCharsets.UTF_8)));
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().warning("lang/de.yml is invalid, texts from the old config.yml are not carried over.");
                return;
            }
        }
        int carried = 0;
        for (String oldKey : old.getKeys(true)) {
            String key = RENAMED_KEYS.getOrDefault(oldKey, oldKey);
            if (old.isConfigurationSection(oldKey) || REPLACED_KEYS.contains(key)) {
                continue;
            }
            String target = null;
            if (key.startsWith("messages.")) {
                target = key.substring("messages.".length());
            } else if (key.equals("discord.test-message")) {
                target = key;
            } else if (key.startsWith("discord.events.") && key.endsWith(".message")) {
                target = "discord." + key.substring("discord.events.".length(), key.length() - ".message".length());
            } else if (key.equals("gui.ban-reason")) {
                target = "gui-ban-reason";
            }
            if (target != null && lang.contains(target)) {
                lang.set(target, old.get(oldKey));
                carried++;
            }
        }
        if (carried == 0) {
            return;
        }
        // The carried texts are from lang-version 1, so Messages renews the texts that changed since then.
        lang.set("lang-version", 1);
        Files.createDirectories(file.getParentFile().toPath());
        lang.options().width(10_000);
        Files.writeString(file.toPath(), BOM + lang.saveToString(), StandardCharsets.UTF_8);
        plugin.getLogger().info("Moved " + carried + " texts from the old config.yml to lang/de.yml.");
    }

    /** Loads config.yml (tolerating a BOM) with the bundled config as defaults for missing keys. */
    public static YamlConfiguration load(MRSleep plugin) {
        YamlConfiguration config = new YamlConfiguration();
        File file = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (file.exists()) {
                config.loadFromString(stripBom(Files.readString(file.toPath(), StandardCharsets.UTF_8)));
            }
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "config.yml is invalid, using defaults: " + e.getMessage());
        }
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                YamlConfiguration defaults = new YamlConfiguration();
                defaults.loadFromString(stripBom(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                config.setDefaults(defaults);
                // Fill in values missing from the file. Bukkit ignores the defaults whenever code passes its own
                // fallback (getString(path, "...")), so missing keys would otherwise get those fallbacks.
                for (String key : defaults.getKeys(true)) {
                    if (!defaults.isConfigurationSection(key) && !config.isSet(key)) {
                        config.set(key, defaults.get(key));
                    }
                }
            }
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read the bundled config: " + e.getMessage());
        }
        return config;
    }

    public static String stripBom(String text) {
        return text.startsWith(BOM) ? text.substring(1) : text;
    }
}
