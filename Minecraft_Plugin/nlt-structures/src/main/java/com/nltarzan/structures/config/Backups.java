package com.nltarzan.structures.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Moves files the plugin is about to replace into the backups folder (plugins/nlT-Structures/backups),
 * named after the file and the time, e.g. monuments-2026-10-10_14-30-05.yml, so no backup is ever overwritten.
 */
public final class Backups {

    private static final String FOLDER = "backups";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private Backups() {
    }

    /** Moves {@code file} into the backups folder. Returns the backup's path relative to the plugin folder, or null if it failed. */
    public static String move(JavaPlugin plugin, File file) {
        return move(plugin, file, LocalDateTime.now());
    }

    /**
     * Moves the messages-backup.yml that older versions left in the plugin folder into the backups folder,
     * stamped with the time it was made.
     */
    public static void moveLegacy(JavaPlugin plugin, String fileName) {
        File legacy = new File(plugin.getDataFolder(), fileName);
        if (legacy.isFile()) {
            move(plugin, legacy, LocalDateTime.ofInstant(Instant.ofEpochMilli(legacy.lastModified()), ZoneId.systemDefault()));
        }
    }

    private static String move(JavaPlugin plugin, File file, LocalDateTime time) {
        String name = file.getName().replace("-backup", "");
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String extension = dot < 0 ? "" : name.substring(dot);
        Path folder = plugin.getDataFolder().toPath().resolve(FOLDER);
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(base + "-" + STAMP.format(time) + extension);
            for (int i = 2; Files.exists(target); i++) {
                target = folder.resolve(base + "-" + STAMP.format(time) + "-" + i + extension);
            }
            Files.move(file.toPath(), target);
            return FOLDER + "/" + target.getFileName();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not move " + file.getName() + " to the " + FOLDER + " folder: " + e.getMessage());
            return null;
        }
    }
}
