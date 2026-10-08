package com.nltarzan.races.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Reads messages.yml (MiniMessage format). Keys missing from the server's copy fall back to the default
 * file inside the jar, so new messages added in an update work without the admin editing anything.
 */
public final class Messages {

    private static final String FILE_NAME = "messages.yml";
    /** Raised in the bundled messages.yml whenever existing texts change, so servers pick up the new wording. */
    private static final String VERSION_KEY = "messages-version";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private YamlConfiguration messages;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration defaults = loadDefaults();
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        } else if (defaults != null && isOutdated(file, defaults)) {
            replaceOutdated(file);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        if (defaults != null) {
            messages.setDefaults(defaults);
        }
    }

    private YamlConfiguration loadDefaults() {
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            return in == null ? null
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read the default messages.yml: " + e.getMessage());
            return null;
        }
    }

    /** The server's copy predates reworded messages in this version of the plugin. */
    private static boolean isOutdated(File file, YamlConfiguration defaults) {
        return YamlConfiguration.loadConfiguration(file).getInt(VERSION_KEY, 0) < defaults.getInt(VERSION_KEY, 0);
    }

    /** Keeps the admin's old file as messages-backup.yml and writes the new one. */
    private void replaceOutdated(File file) {
        File backup = new File(plugin.getDataFolder(), "messages-backup.yml");
        if (backup.exists() && !backup.delete()) {
            plugin.getLogger().warning("Could not replace " + backup.getName() + "; keeping the old messages.yml.");
            return;
        }
        if (!file.renameTo(backup)) {
            plugin.getLogger().warning("Could not back up messages.yml; keeping the old one.");
            return;
        }
        plugin.saveResource(FILE_NAME, false);
        plugin.getLogger().warning("messages.yml was out of date and has been replaced. Your previous copy is "
                + "saved as messages-backup.yml; copy any custom texts back from there.");
    }

    /**
     * Returns a message without the prefix. {@code placeholders} are pairs of name and value, used as
     * {@code <name>} in messages.yml: {@code get("help.line", "label", "nltraces", "usage", "help")}.
     * String values are inserted as plain text; Component values (e.g. another message) keep their styling.
     */
    public Component get(String key, Object... placeholders) {
        String text = messages.getString(key);
        if (text == null) {
            return Component.text("[missing message: " + key + "]", NamedTextColor.RED);
        }
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String name = String.valueOf(placeholders[i]);
            Object value = placeholders[i + 1];
            resolvers.resolver(value instanceof Component component
                    ? Placeholder.component(name, component)
                    : Placeholder.unparsed(name, String.valueOf(value)));
        }
        return MINI_MESSAGE.deserialize(text, resolvers.build());
    }

    /** A message that runs {@code command} when clicked, with a "Click to run ..." hover. */
    public Component button(String key, String command, Object... placeholders) {
        return get(key, placeholders)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(get("general.click-to-run", "command", command)));
    }

    /** Sends a message with the prefix in front. */
    public void send(Audience to, String key, Object... placeholders) {
        to.sendMessage(get("prefix").append(get(key, placeholders)));
    }

    /** Sends a message without the prefix, for list lines such as help entries. */
    public void sendRaw(Audience to, String key, Object... placeholders) {
        to.sendMessage(get(key, placeholders));
    }

    /** Shows a message above the player's hotbar. */
    public void actionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    /** Shows a title; {@code subtitleKey} may be null. Stays for {@code staySeconds}. */
    public void title(Player player, String titleKey, String subtitleKey, int staySeconds, Object... placeholders) {
        Component subtitle = subtitleKey == null ? Component.empty() : get(subtitleKey, placeholders);
        player.showTitle(Title.title(get(titleKey, placeholders), subtitle,
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(staySeconds), Duration.ofMillis(500))));
    }
}
