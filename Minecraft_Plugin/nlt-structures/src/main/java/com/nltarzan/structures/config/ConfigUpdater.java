package com.nltarzan.structures.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Brings a structure's config file (e.g. monuments/monuments.yml) up to date with the one in the jar, like
 * nlT-Races does for its config.yml. When settings were added or removed in an update, the file is rewritten
 * in the new layout (with its comments), keeping the admin's value for every setting that still exists. The
 * old file goes to the backups folder first. An up-to-date file is left alone.
 *
 * <p>Two rules make sure an update can never move a structure or change what it builds:
 * <ul>
 *   <li>A file that can't be read, or that has a problem such as no valid location, is left alone. The
 *       structure then pauses and says why, exactly as without the updater.</li>
 *   <li>"Own" sections, such as {@code variants:}, hold keys the admin chooses. They're kept exactly as
 *       the admin has them: a variant they removed doesn't come back, one they added isn't dropped.</li>
 * </ul>
 */
public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    /**
     * @param resource    the default file inside the jar, e.g. "monuments/monuments.yml"
     * @param file        the server's copy
     * @param ownSections sections whose keys are the admin's own, kept as they are
     * @param problem     what's wrong with a loaded file, or null when it's fine; a file with a problem isn't updated
     */
    public static void update(JavaPlugin plugin, String resource, File file, Set<String> ownSections,
                              Function<ConfigurationSection, String> problem) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration current = new YamlConfiguration();
        try {
            current.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            return; // reported when the structure loads the file
        }
        if (problem.apply(current) != null) {
            return;
        }
        YamlConfiguration merged = loadDefaults(plugin, resource);
        if (merged == null) {
            return;
        }

        List<String> added = new ArrayList<>(settings(merged, ownSections).stream()
                .filter(key -> !current.contains(key, true)).toList());
        ownSections.stream().filter(section -> merged.contains(section, true) && !current.contains(section, true))
                .forEach(added::add);
        List<String> removed = settings(current, ownSections).stream()
                .filter(key -> !merged.contains(key, true)).toList();
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }

        keepValues(merged, current, ownSections);
        String backup = Backups.move(plugin, file);
        if (backup == null) {
            plugin.getLogger().warning("Could not back up " + resource + ", so it wasn't updated. Missing settings use "
                    + "their defaults: " + String.join(", ", added));
            return;
        }
        try {
            merged.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write the updated " + resource + " (" + e.getMessage()
                    + "). Your previous copy is in " + backup + ".");
            return;
        }
        plugin.getLogger().info("Updated " + resource + " to this version; your settings were kept."
                + (added.isEmpty() ? "" : " Added: " + String.join(", ", added) + ".")
                + (removed.isEmpty() ? "" : " Removed (no longer used): " + String.join(", ", removed) + ".")
                + " Previous copy: " + backup);
    }

    /**
     * Copies the admin's value of every setting {@code defaults} still has into it, and replaces the contents
     * of each own section the admin has with theirs; the comments of {@code defaults} stay.
     */
    static void keepValues(YamlConfiguration defaults, YamlConfiguration current, Set<String> ownSections) {
        for (String key : settings(current, ownSections)) {
            if (defaults.contains(key, true) && !defaults.isConfigurationSection(key)) {
                defaults.set(key, current.get(key));
            }
        }
        for (String section : ownSections) {
            ConfigurationSection theirs = current.getConfigurationSection(section);
            ConfigurationSection ours = defaults.getConfigurationSection(section);
            if (theirs == null || ours == null) {
                continue;
            }
            // Key by key, so the comment above the section itself stays.
            for (String key : ours.getKeys(false)) {
                if (!theirs.contains(key, true)) {
                    ours.set(key, null);
                }
            }
            for (String key : theirs.getKeys(false)) {
                ours.set(key, theirs.get(key));
            }
        }
    }

    /**
     * Every setting (not the sections that only group them), as full paths like reset.mode, leaving out
     * everything inside the own sections.
     */
    static List<String> settings(YamlConfiguration config, Set<String> ownSections) {
        return config.getKeys(true).stream()
                .filter(key -> !config.isConfigurationSection(key))
                .filter(key -> ownSections.stream().noneMatch(s -> key.equals(s) || key.startsWith(s + ".")))
                .toList();
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin, String resource) {
        try (InputStream in = plugin.getResource(resource)) {
            return in == null ? null
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read the default " + resource + ": " + e.getMessage());
            return null;
        }
    }
}
