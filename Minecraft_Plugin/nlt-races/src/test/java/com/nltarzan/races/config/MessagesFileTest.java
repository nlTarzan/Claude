package com.nltarzan.races.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The messages.yml in the jar: valid YAML, versioned, and every text readable as MiniMessage. */
class MessagesFileTest {

    @Test
    void defaultMessagesYmlIsValid() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
            assertNotNull(in, "messages.yml should be in the plugin's resources");
            yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertTrue(yaml.getInt("messages-version") > 0, "messages-version is missing");
        assertNotNull(yaml.getString("prefix"), "prefix is missing");

        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                String text = yaml.getString(key);
                assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(text), key);
            }
        }
    }
}
