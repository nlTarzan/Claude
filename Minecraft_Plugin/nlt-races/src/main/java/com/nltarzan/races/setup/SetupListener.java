package com.nltarzan.races.setup;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * While an admin is in setup, right-clicks set the current step: a block in reach is used as clicked, and a
 * right-click in the air uses the block the admin stands on (or hovers above), so spots can be set midair.
 * Minecraft only reports right-clicks in the air while an item is held.
 */
public final class SetupListener implements Listener {

    private final SetupService service;

    public SetupListener(SetupService service) {
        this.service = service;
    }

    /** Runs first, and stops the click from doing anything else (pressing buttons, opening doors, placing). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        boolean blockClick = action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null;
        if ((!blockClick && action != Action.RIGHT_CLICK_AIR) || !service.isInSession(player)) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (!isPrimaryHand(event, player)) {
            return; // each hand fires its own event for the same click
        }
        if (blockClick) {
            service.handleClick(player, event.getClickedBlock(), false);
        } else {
            service.handleClick(player, blockUnderFeet(player), true);
        }
    }

    /** The main hand, or the off-hand when the main hand is empty (then only the off-hand fires in the air). */
    private static boolean isPrimaryHand(PlayerInteractEvent event, Player player) {
        if (event.getHand() == EquipmentSlot.HAND) {
            return true;
        }
        return event.getAction() == Action.RIGHT_CLICK_AIR
                && player.getInventory().getItemInMainHand().getType().isAir();
    }

    /** The block the player stands on; midair, the block at their feet. Slabs and other partial blocks count. */
    private static Block blockUnderFeet(Player player) {
        Location feet = player.getLocation();
        return feet.getWorld().getBlockAt(feet.getBlockX(), (int) Math.floor(feet.getY() - 0.01), feet.getBlockZ());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.cancel(event.getPlayer());
    }
}
