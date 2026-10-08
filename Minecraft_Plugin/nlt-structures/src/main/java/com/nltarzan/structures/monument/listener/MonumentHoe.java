package com.nltarzan.structures.monument.listener;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.BlockTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.OptionalLong;

/**
 * The Monument Hoe: an iron hoe with one use per sponge in the monument it came from. It carries
 * the "structures" tag, and the id of that monument so a hoe kept from an earlier one is recognised.
 */
public final class MonumentHoe {

    private final NamespacedKey tag;
    private final NamespacedKey monumentKey;

    public MonumentHoe(Plugin plugin) {
        this.tag = new NamespacedKey(plugin, "structures");
        this.monumentKey = new NamespacedKey(plugin, "monument");
    }

    /**
     * @param sponges   the hoe's durability, one use per sponge
     * @param monument  the id of the monument it belongs to
     */
    public ItemStack create(int sponges, long monument) {
        ItemStack hoe = new ItemStack(Material.IRON_HOE);
        hoe.editMeta(Damageable.class, meta -> {
            meta.setMaxDamage(sponges);
            meta.displayName(Component.text("Monument Hoe", NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Mines the sponges of the Ocean Monument.", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(tag, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(monumentKey, PersistentDataType.LONG, monument);
        });
        // Vanilla's "Can break: Sponge, Wet Sponge" tooltip. Only enforced in Adventure mode;
        // in Survival MonumentHoeListener does the enforcing.
        hoe.setData(DataComponentTypes.CAN_BREAK, ItemAdventurePredicate.itemAdventurePredicate(List.of(
                BlockPredicate.predicate()
                        .blocks(RegistrySet.keySet(RegistryKey.BLOCK, BlockTypeKeys.SPONGE, BlockTypeKeys.WET_SPONGE))
                        .build())));
        return hoe;
    }

    /** Whether an item is a Monument Hoe (has the "structures" tag). */
    public boolean is(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(tag);
    }

    /** The id of the monument a hoe belongs to; empty for hoes from before ids were added. */
    public OptionalLong monument(ItemStack item) {
        PersistentDataContainerView data = item.getPersistentDataContainer();
        Long id = data.get(monumentKey, PersistentDataType.LONG);
        return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    public static boolean isSponge(Block block) {
        Material type = block.getType();
        return type == Material.SPONGE || type == Material.WET_SPONGE;
    }
}
