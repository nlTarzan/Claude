package com.nltarzan.structures.monument.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonumentConfigTest {

    @Test
    void defaultMonumentsYmlIsValid() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("monuments/monuments.yml")) {
            assertNotNull(in, "monuments.yml should be in the plugin's resources");
            yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertNull(MonumentConfig.locationProblem(yaml));

        MonumentConfig config = MonumentConfig.load(yaml, Logger.getAnonymousLogger());
        assertEquals("world", config.worldName());
        assertEquals(224, config.centerX());
        assertEquals(40, config.centerY());
        assertEquals(-320, config.centerZ());
        assertEquals(11, config.variants().size());
        assertEquals(4, config.intervals().size());
    }

    @Test
    void missingOrBrokenLocationIsCaught() throws InvalidConfigurationException {
        assertProblem("reset:\n  announce: true\n", "has no location: section");
        // An old config.yml copied over still uses the old section name.
        assertProblem("monument:\n  world: world\n  x: 224\n  y: 40\n  z: -320\n", "has no location: section");
        assertProblem("location:\n  x: 224\n  y: 40\n  z: -320\n", "has no location.world");
        assertProblem("location:\n  world: ''\n  x: 224\n  y: 40\n  z: -320\n", "has no location.world");
        assertProblem("location:\n  world: world\n  y: 40\n  z: -320\n", "has no whole number for location.x");
        assertProblem("location:\n  world: world\n  x: 224\n  y: forty\n  z: -320\n", "has no whole number for location.y");
        assertProblem("location:\n  world: world\n  x: 224\n  y: 40\n  z: -320.5\n", "has no whole number for location.z");
        assertNullProblem("location:\n  world: world\n  x: 224\n  y: 40\n  z: -320\n");
    }

    @Test
    void brokenYamlCantBeRead() {
        // A missing space after the colon and a bad indent: this is what makes the whole file unreadable.
        YamlConfiguration yaml = new YamlConfiguration();
        assertThrows(InvalidConfigurationException.class,
                () -> yaml.loadFromString("location:\n  world: world\n x:224\n    y: 40\n"));
    }

    @Test
    void emptyFileHasNoLocation() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("");
        assertTrue(MonumentConfig.locationProblem(yaml).contains("no location"));
    }

    private static void assertProblem(String yamlText, String expected) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(yamlText);
        assertEquals(expected, MonumentConfig.locationProblem(yaml), yamlText);
    }

    private static void assertNullProblem(String yamlText) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(yamlText);
        assertNull(MonumentConfig.locationProblem(yaml), yamlText);
    }
}
