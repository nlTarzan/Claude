package com.nltarzan.races.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Updating an older config.yml keeps the admin's values, adds new settings with comments and drops old ones.
 * A file with a YAML mistake is reported instead of being treated as empty.
 */
class ConfigUpdaterTest {

    @Test
    void oldConfigIsMergedIntoTheNewLayout() throws IOException, InvalidConfigurationException {
        YamlConfiguration merged = defaults();
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("""
                database:
                  type: mysql
                  mysql:
                    password: secret
                leaderboards:
                  time-zone: Europe/Amsterdam
                old-setting: 5
                """);

        ConfigUpdater.keepValues(merged, old);
        String saved = merged.saveToString();
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(saved);

        assertEquals("mysql", reloaded.getString("database.type"));
        assertEquals("secret", reloaded.getString("database.mysql.password"));
        assertEquals("Europe/Amsterdam", reloaded.getString("leaderboards.time-zone"));
        assertEquals(20, reloaded.getInt("placeholders.cycle.seconds"), "new settings get their default");
        assertFalse(reloaded.contains("old-setting"), "settings no longer in the default are dropped");
        assertTrue(saved.contains("# How many seconds each period shows"), "comments of the new file are kept");
    }

    @Test
    void fileWithAYamlMistakeIsReportedNotReadAsEmpty(@TempDir Path folder) throws IOException {
        File file = folder.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), """
                database:
                  type: mysql
                    password: secret
                """);

        String problem = ConfigUpdater.load(file, new YamlConfiguration(), Logger.getAnonymousLogger());

        assertNotNull(problem, "a file with a YAML mistake must not count as an empty file with every setting missing");
        assertTrue(problem.startsWith("can't be read ("), problem);
    }

    @Test
    void readableFileHasNoProblem(@TempDir Path folder) throws IOException {
        File file = folder.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "database:\n  type: mysql\n");
        YamlConfiguration into = new YamlConfiguration();

        assertNull(ConfigUpdater.load(file, into, Logger.getAnonymousLogger()));
        assertEquals("mysql", into.getString("database.type"));
    }

    private YamlConfiguration defaults() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(in, "config.yml should be in the plugin's resources");
            yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return yaml;
    }
}
