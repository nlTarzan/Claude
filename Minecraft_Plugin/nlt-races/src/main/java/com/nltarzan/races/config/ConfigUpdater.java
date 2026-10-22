package com.nltarzan.races.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

/**
 * Brings the server's config.yml up to date with the one in the jar. When settings were added or removed in
 * an update, the file is rewritten in the new layout (with its comments), keeping the admin's value for every
 * setting that still exists. The old file goes to the backups folder first. An up-to-date file is left alone.
 *
 * <p>A file that can't be read (a YAML mistake) is left alone too: read loosely it would look empty, and every
 * setting would be "missing", so the admin's file would be replaced by the defaults. The caller is told instead.
 */
public final class ConfigUpdater {

    private static final String FILE_NAME = "config.yml";

    private ConfigUpdater() {
    }

    /**
     * @return null when config.yml is fine (and now up to date), otherwise what's wrong with it, e.g.
     *         "can't be read (while parsing a block mapping)"; the file is then left as it is
     */
    public static String update(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveDefaultConfig();
            return null;
        }
        YamlConfiguration current = new YamlConfiguration();
        String problem = load(file, current, plugin.getLogger());
        if (problem != null) {
            return problem;
        }
        YamlConfiguration merged = loadDefaults(plugin);
        if (merged == null) {
            return null;
        }
        List<String> added = settings(merged).stream().filter(key -> !current.contains(key, true)).toList();
        List<String> removed = settings(current).stream().filter(key -> !merged.contains(key, true)).toList();
        if (added.isEmpty() && removed.isEmpty()) {
            return null;
        }

        keepValues(merged, current);
        String backup = Backups.move(plugin, file);
        if (backup == null) {
            plugin.getLogger().warning("Could not back up config.yml, so it wasn't updated. Missing settings use "
                    + "their defaults: " + String.join(", ", added));
            return null;
        }
        try {
            merged.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write the updated config.yml (" + e.getMessage()
                    + "). Your previous copy is in " + backup + ".");
            return null;
        }
        plugin.getLogger().info("Updated config.yml to this version; your settings were kept."
                + (added.isEmpty() ? "" : " Added: " + String.join(", ", added) + ".")
                + (removed.isEmpty() ? "" : " Removed (no longer used): " + String.join(", ", removed) + ".")
                + " Previous copy: " + backup);
        return null;
    }

    /**
     * Reads {@code file} strictly into {@code into}.
     *
     * @return null when it was read, otherwise what's wrong with it (the full error goes to {@code logger})
     */
    static String load(File file, YamlConfiguration into, Logger logger) {
        try {
            into.load(file);
            return null;
        } catch (IOException | InvalidConfigurationException e) {
            logger.severe(String.valueOf(e.getMessage()));
            return "can't be read (" + firstLine(e.getMessage()) + ")";
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown error";
        }
        String line = message.strip().lines().findFirst().orElse(message);
        return line.length() > 120 ? line.substring(0, 120) + "..." : line;
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
