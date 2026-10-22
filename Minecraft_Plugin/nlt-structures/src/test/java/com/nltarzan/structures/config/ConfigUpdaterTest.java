package com.nltarzan.structures.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Updating an older monuments.yml keeps the admin's values and their own variants, adds new settings with
 * comments and drops old ones.
 */
class ConfigUpdaterTest {

    private static final Set<String> OWN = Set.of("variants");

    @Test
    void oldConfigIsMergedIntoTheNewLayout() throws IOException, InvalidConfigurationException {
        YamlConfiguration merged = defaults();
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("""
                location:
                  world: ocean
                  x: 1000
                  y: 39
                  z: -2000
                reset:
                  mode: generated
                  intervals:
                    - { delay: 10s, weight: 1 }
                old-setting: 5
                """);

        ConfigUpdater.keepValues(merged, old, OWN);
        String saved = merged.saveToString();
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(saved);

        assertEquals("ocean", reloaded.getString("location.world"));
        assertEquals(1000, reloaded.getInt("location.x"));
        assertEquals(39, reloaded.getInt("location.y"));
        assertEquals(-2000, reloaded.getInt("location.z"));
        assertEquals("generated", reloaded.getString("reset.mode"));
        assertEquals(1, reloaded.getMapList("reset.intervals").size(), "lists are kept whole");
        assertEquals(40, reloaded.getInt("location.check-radius"), "new settings get their default");
        assertTrue(reloaded.getBoolean("reward.enabled"), "new settings get their default");
        assertFalse(reloaded.contains("old-setting"), "settings no longer in the default are dropped");
        assertTrue(saved.contains("# Center of the monument."), "comments of the new file are kept");
    }

    @Test
    void theAdminsOwnVariantsAreKeptExactly() throws IOException, InvalidConfigurationException {
        YamlConfiguration merged = defaults();
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("""
                variants:
                  monument_01: 50
                  my_monument: 5
                """);

        ConfigUpdater.keepValues(merged, old, OWN);
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(merged.saveToString());

        assertEquals(Set.of("monument_01", "my_monument"), reloaded.getConfigurationSection("variants").getKeys(false),
                "removed variants don't come back and added ones stay");
        assertEquals(50, reloaded.getInt("variants.monument_01"));
        assertEquals(5, reloaded.getInt("variants.my_monument"));
    }

    @Test
    void variantsDontCountAsAddedOrRemovedSettings() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = defaults();
        assertTrue(ConfigUpdater.settings(yaml, OWN).contains("reset.mode"));
        assertTrue(ConfigUpdater.settings(yaml, OWN).contains("location.x"));
        assertTrue(ConfigUpdater.settings(yaml, OWN).stream().noneMatch(key -> key.startsWith("variants")));
    }

    private YamlConfiguration defaults() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("monuments/monuments.yml")) {
            assertNotNull(in, "monuments.yml should be in the plugin's resources");
            yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return yaml;
    }
}
