package com.nltarzan.races.runtime;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Items handed out for a race (steering sticks). They're marked so they can never be kept. */
public final class RaceItems {

    private final NamespacedKey key;

    public RaceItems(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "race_item");
    }

    public ItemStack create(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRaceItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Removes every race item the player has, including one on their cursor. */
    public void removeAll(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isRaceItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
        if (isRaceItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }
}
