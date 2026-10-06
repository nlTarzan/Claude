package com.nltarzan.structures.monument.listener;

import com.nltarzan.structures.monument.Monuments;
import com.nltarzan.structures.monument.MonumentArena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the Monument Hoe to its one job, mining the monument's sponges:
 * <ul>
 *     <li>Using it outside the monument, or a hoe from an earlier monument, deletes every Monument
 *     Hoe from that player's inventory (so hoes smuggled out in an Ender Chest or bundle are gone
 *     the moment they're used).</li>
 *     <li>In the monument, sponges can only be mined with the hoe, and the hoe only mines sponges.</li>
 *     <li>It only loses durability for mining a sponge, one use each, and can't be repaired,
 *     enchanted or combined.</li>
 * </ul>
 * GriefDefender (or another protection plugin) still decides whether a sponge may be broken at all;
 * this listener only ever cancels, it never allows something that was cancelled.
 */
public final class MonumentHoeListener implements Listener {

    private final Monuments monuments;
    private final MonumentHoe hoe;
    /** Players whose next durability loss is from a sponge they just mined. */
    private final Set<UUID> minedSponge = new HashSet<>();

    public MonumentHoeListener(Monuments monuments, MonumentHoe hoe) {
        this.monuments = monuments;
        this.hoe = hoe;
    }

    /** Any click with the hoe. Runs on cancelled events too: right-clicking air arrives cancelled. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!monuments.isActive()) {
            return; // no hoe rules while monuments.yml is broken or inactive, so nobody loses a hoe over it
        }
        ItemStack item = event.getItem();
        if (!hoe.is(item)) {
            return;
        }
        Block clicked = event.getClickedBlock();
        Location where = clicked != null ? clicked.getLocation() : event.getPlayer().getLocation();
        if (!usable(item, where)) {
            event.setCancelled(true);
            confiscate(event.getPlayer());
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            // No tilling, stripping or other hoe actions; clicking doors and the like still works.
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!monuments.isActive()) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (hoe.is(item)) {
            if (!usable(item, block.getLocation())) {
                event.setCancelled(true);
                confiscate(player);
            } else if (!MonumentHoe.isSponge(block)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("The Monument Hoe only mines sponges.", NamedTextColor.RED));
            }
            return;
        }
        MonumentArena arena = monuments.arena();
        if (arena != null && arena.inMonument(block.getLocation()) && MonumentHoe.isSponge(block)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Sponges can only be mined with the Monument Hoe.", NamedTextColor.RED));
        }
    }

    /** A sponge really got mined with the hoe: let the durability loss that follows through. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeMined(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hoe.is(player.getInventory().getItemInMainHand()) && MonumentHoe.isSponge(event.getBlock())) {
            UUID id = player.getUniqueId();
            minedSponge.add(id);
            monuments.getServer().getScheduler().runTask(monuments.plugin(), () -> minedSponge.remove(id));
        }
    }

    /** The hoe only wears down from mining sponges: exactly one use per sponge. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDurability(PlayerItemDamageEvent event) {
        if (!hoe.is(event.getItem())) {
            return;
        }
        if (minedSponge.remove(event.getPlayer().getUniqueId())) {
            event.setDamage(1);
        } else {
            event.setCancelled(true);
        }
    }

    /** Hitting something with the hoe outside the monument also counts as using it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!monuments.isActive()) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (hoe.is(item) && !usable(item, player.getLocation())) {
            event.setCancelled(true);
            confiscate(player);
        }
    }

    // No repairing, enchanting or combining: the durability has to stay one use per sponge.

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (contains(event.getInventory())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        if (contains(event.getInventory())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (contains(event.getInventory())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (hoe.is(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** In the current monument, and belonging to it (not kept from an earlier one). */
    private boolean usable(ItemStack item, Location where) {
        MonumentArena arena = monuments.arena();
        return arena != null && arena.inMonument(where)
                && hoe.monument(item).orElse(0) == monuments.currentMonument().id();
    }

    /** Deletes every Monument Hoe the player carries. */
    private void confiscate(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        boolean removed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            if (hoe.is(contents[slot])) {
                inventory.setItem(slot, null);
                removed = true;
            }
        }
        if (hoe.is(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removed = true;
        }
        if (removed) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(Component.text("The Monument Hoe crumbles away, it only works inside the Ocean Monument.",
                    NamedTextColor.RED));
        }
    }

    private boolean contains(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (hoe.is(item)) {
                return true;
            }
        }
        return false;
    }
}
