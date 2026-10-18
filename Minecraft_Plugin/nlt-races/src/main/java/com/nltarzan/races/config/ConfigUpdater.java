package com.nltarzan.races.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Brings the server's config.yml up to date with the one in the jar. When settings were added or removed in
 * an update, the file is rewritten in the new layout (with its comments), keeping the admin's value for every
 * setting that still exists. The old file goes to the backups folder first. An up-to-date file is left alone.
 */
public final class ConfigUpdater {

    private static final String FILE_NAME = "config.yml";

    private ConfigUpdater() {
    }

    public static void update(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveDefaultConfig();
            return;
        }
        YamlConfiguration merged = loadDefaults(plugin);
        if (merged == null) {
            return;
        }
        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        List<String> added = settings(merged).stream().filter(key -> !current.contains(key, true)).toList();
        List<String> removed = settings(current).stream().filter(key -> !merged.contains(key, true)).toList();
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }

        keepValues(merged, current);
        String backup = Backups.move(plugin, file);
        if (backup == null) {
            plugin.getLogger().warning("Could not back up config.yml, so it wasn't updated. Missing settings use "
                    + "their defaults: " + String.join(", ", added));
            return;
        }
        try {
            merged.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write the updated config.yml (" + e.getMessage()
                    + "). Your previous copy is in " + backup + ".");
            return;
        }
        plugin.getLogger().info("Updated config.yml to this version; your settings were kept."
                + (added.isEmpty() ? "" : " Added: " + String.join(", ", added) + ".")
                + (removed.isEmpty() ? "" : " Removed (no longer used): " + String.join(", ", removed) + ".")
                + " Previous copy: " + backup);
    }

    /** Copies the admin's value of every setting {@code defaults} still has into it; its comments stay. */
    static void keepValues(YamlConfiguration defaults, YamlConfiguration current) {
        for (String key : settings(current)) {
            if (defaults.contains(key, true) && !defaults.isConfigurationSection(key)) {
                defaults.set(key, current.get(key));
            }
        }
    }

    /** Every setting (not the sections that only group them), as full paths like placeholders.cycle.seconds. */
    static List<String> settings(YamlConfiguration config) {
        return config.getKeys(true).stream().filter(key -> !config.isConfigurationSection(key)).toList();
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            return in == null ? null
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read the default config.yml: " + e.getMessage());
            return null;
        }
    }
}
