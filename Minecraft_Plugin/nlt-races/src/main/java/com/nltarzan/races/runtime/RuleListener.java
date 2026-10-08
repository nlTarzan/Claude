package com.nltarzan.races.runtime;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.TravelMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/** Turns things a racer isn't allowed to do into disqualifications. Each rule has its own message. */
public final class RuleListener implements Listener {

    /** A freeze "put back" moves the player less than this (squared, in blocks); real teleports go further. */
    private static final double FREEZE_SNAP_BACK_SQUARED = 2 * 2;

    private final NltRacesPlugin plugin;
    private final RaceService service;

    public RuleListener(NltRacesPlugin plugin, RaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** Teleports that would skip part of the course. Dismount and bed teleports are fine. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (service.raceOf(player) == null || service.isInternalTeleport(player)) {
            return;
        }
        // Racers frozen on the grid before GO are put back on their spot when they try to move;
        // Paper reports that as a teleport. Only a real teleport (further than a step) counts.
        if (service.isFrozen(player) && event.getFrom().getWorld().equals(event.getTo().getWorld())
                && event.getFrom().distanceSquared(event.getTo()) < FREEZE_SNAP_BACK_SQUARED) {
            return;
        }
        DqReason reason = switch (event.getCause()) {
            case ENDER_PEARL -> DqReason.ENDER_PEARL;
            case CONSUMABLE_EFFECT -> DqReason.CHORUS_FRUIT; // chorus fruit, or any food with a teleport effect
            case COMMAND, PLUGIN, SPECTATE, END_GATEWAY -> DqReason.TELEPORT;
            case NETHER_PORTAL, END_PORTAL -> DqReason.WORLD_CHANGE;
            default -> null;
        };
        if (reason == null) {
            return;
        }
        event.setCancelled(true);
        // Teleporting to the lobby from inside another teleport event is unreliable, so do it next tick.
        plugin.sync(() -> service.disqualify(player, reason));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        service.disqualify(event.getPlayer(), DqReason.GAMEMODE);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (service.raceOf(event.getEntity()) != null) {
            service.handleDeathDrops(event.getDrops());
        }
        service.disqualify(event.getEntity(), DqReason.DEATH);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location lobby = service.takeRespawn(event.getPlayer());
        if (lobby != null) {
            event.setRespawnLocation(lobby);
        }
    }

    /** Fallback for world changes that didn't come through a teleport we caught. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!service.isInternalTeleport(event.getPlayer())) {
            service.disqualify(event.getPlayer(), DqReason.WORLD_CHANGE);
        }
    }

    /**
     * Race vehicles can only be ridden by their own racer. Vehicle racers can only ride their own vehicle
     * (lenient rules: to remount it). Foot racers can't ride anything.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        Entity mount = event.getMount();
        UUID owner = service.vehicleOwner(mount);
        if (owner != null && !owner.equals(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Player player) {
                plugin.getMessages().send(player, "play.vehicle-taken");
            }
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        TravelMode travel = travelOf(player);
        if (travel == TravelMode.FOOT) {
            event.setCancelled(true);
            service.disqualify(player, DqReason.FOOT_MOUNT);
        } else if (travel == TravelMode.VEHICLE && !mount.equals(service.vehicleOf(player))) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "play.not-your-vehicle");
        }
    }

    // ---------------------------------------------------------------- race items (steering sticks)

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (service.raceItems().isRaceItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        RaceItems items = service.raceItems();
        if (items.isRaceItem(event.getCurrentItem()) || items.isRaceItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    /** Removes race items left over if the server stopped mid-race without shutting down cleanly. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.raceItems().removeAll(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.isGliding() && event.getEntity() instanceof Player player
                && travelOf(player) == TravelMode.FOOT) {
            event.setCancelled(true);
            service.disqualify(player, DqReason.FOOT_GLIDE);
        }
    }

    private TravelMode travelOf(Player player) {
        Race race = service.raceOf(player);
        return race == null ? null : race.getTravel();
    }
}
