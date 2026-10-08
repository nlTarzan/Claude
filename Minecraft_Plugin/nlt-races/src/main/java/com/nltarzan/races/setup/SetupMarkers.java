package com.nltarzan.races.setup;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fake blocks shown only to the admin, marking blocks they clicked during setup (checkpoint corners,
 * grid spots). The world itself is never changed. Which block is used comes from setup-markers in config.yml.
 */
final class SetupMarkers {

    /** Marker list for the first corner of a region, while the second hasn't been clicked yet. */
    static final String PENDING = "pending";

    private final JavaPlugin plugin;

    SetupMarkers(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** The config key for a step's marker block, or null when that step has no markers. */
    private static String configKey(String stepId) {
        return switch (stepId) {
            case "start-checkpoint" -> "start-checkpoint";
            case "middle-checkpoint" -> "middle-checkpoint";
            case "finish-checkpoint" -> "finish-checkpoint";
            case "grid-slot", "grid-region" -> "grid-spot";
            case "point-zone" -> "point-zone";
            default -> null;
        };
    }

    boolean hasMarkers(String stepId) {
        return configKey(stepId) != null;
    }

    /** Marks one more block for a step (or for {@link #PENDING}, using {@code stepId}'s block). */
    void add(Player player, SetupSession session, String listKey, String stepId, Location block) {
        BlockData data = blockFor(stepId);
        if (data == null) {
            return;
        }
        session.markers.computeIfAbsent(listKey, key -> new ArrayList<>()).add(block.clone());
        player.sendBlockChange(block, data);
        session.markerData.put(listKey, data);
    }

    /** Replaces a step's markers with {@code blocks} (old ones that aren't reused revert to the real block). */
    void replace(Player player, SetupSession session, String stepId, List<Location> blocks) {
        clear(player, session, stepId, blocks);
        for (Location block : blocks) {
            add(player, session, stepId, stepId, block);
        }
    }

    /** Removes a step's markers, showing the real blocks again (except those listed in {@code keep}). */
    void clear(Player player, SetupSession session, String listKey, List<Location> keep) {
        List<Location> old = session.markers.remove(listKey);
        if (old != null) {
            for (Location block : old) {
                if (!keep.contains(block)) {
                    revert(player, block);
                }
            }
        }
    }

    /** Forgets the pending first-corner marker without reverting it (it became part of the region). */
    void forgetPending(SetupSession session) {
        session.markers.remove(PENDING);
    }

    void removeLast(Player player, SetupSession session, String listKey) {
        List<Location> list = session.markers.get(listKey);
        if (list != null && !list.isEmpty()) {
            revert(player, list.removeLast());
        }
    }

    /** Sends all markers again; the client drops them when the block is clicked or its chunk reloads. */
    void refresh(Player player, SetupSession session) {
        for (Map.Entry<String, List<Location>> entry : session.markers.entrySet()) {
            BlockData data = session.markerData.get(entry.getKey());
            if (data != null) {
                for (Location block : entry.getValue()) {
                    player.sendBlockChange(block, data);
                }
            }
        }
    }

    /** Shows the real blocks again, now or after the linger time from config.yml. */
    void clearAll(Player player, SetupSession session, boolean linger) {
        List<Location> blocks = session.markers.values().stream().flatMap(List::stream).toList();
        session.markers.clear();
        if (blocks.isEmpty()) {
            return;
        }
        Runnable revertAll = () -> {
            if (player.isOnline()) {
                blocks.forEach(block -> revert(player, block));
            }
        };
        long delay = linger ? Math.max(0, plugin.getConfig().getInt("setup-markers.linger-seconds", 5)) * 20L : 0;
        if (delay == 0) {
            revertAll.run();
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, revertAll, delay);
        }
    }

    private static void revert(Player player, Location block) {
        if (block.getWorld() != null && block.getWorld().equals(player.getWorld())) {
            player.sendBlockChange(block, block.getBlock().getBlockData());
        }
    }

    private BlockData blockFor(String stepId) {
        String key = configKey(stepId);
        if (key == null) {
            return null;
        }
        // No explicit default, so a config.yml from before this setting existed falls back to the jar's value.
        String name = String.valueOf(plugin.getConfig().getString("setup-markers." + key));
        Material material = Material.matchMaterial(name);
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("setup-markers." + key + " in config.yml isn't a block: '" + name
                    + "'. Using glowstone instead.");
            material = Material.GLOWSTONE;
        }
        return material.createBlockData();
    }
}
