package com.nltarzan.races.runtime;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.race.BlockPos;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.RaceRegistry.ButtonLink;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Linked buttons, pressure plates and signs, plus players leaving mid-race. */
public final class RaceListener implements Listener {

    public static final String USER_PERMISSION = "nltraces.user";
    /** Pressure plates fire every tick while stood on; ignore repeats within this window. */
    private static final long USE_COOLDOWN_MS = 1000;

    private final NltRacesPlugin plugin;
    private final RaceService service;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public RaceListener(NltRacesPlugin plugin, RaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null || (action != Action.RIGHT_CLICK_BLOCK && action != Action.PHYSICAL)
                || plugin.getSetupService().isInSession(event.getPlayer())) {
            return; // admins in setup are clicking to set things, not to use buttons
        }
        ButtonLink link = plugin.getRaces().findButton(BlockPos.of(block));
        if (link == null) {
            return;
        }
        if (link.action() == ButtonAction.GATE) {
            // Gate levers are switched by the race. Deny both hands, or the off-hand would flip it.
            event.setUseInteractedBlock(Event.Result.DENY);
            if (event.getHand() == EquipmentSlot.HAND) {
                plugin.getMessages().send(event.getPlayer(), "play.gate-lever");
            }
            return;
        }
        boolean click = action == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND;
        if (!click && action != Action.PHYSICAL) {
            return;
        }
        if (block.getState() instanceof Sign) {
            event.setCancelled(true); // don't open the sign editor
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long previous = lastUse.get(player.getUniqueId());
        if (previous != null && now - previous < USE_COOLDOWN_MS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        if (!player.hasPermission(USER_PERMISSION)) {
            plugin.getMessages().send(player, "general.no-permission");
            return;
        }
        service.handleButton(player, link.race(), link.action());
    }

    /** Group racers on the grid can look around but not move until GO. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())
                && service.isFrozen(event.getPlayer())) {
            Location frozen = from.clone();
            frozen.setYaw(to.getYaw());
            frozen.setPitch(to.getPitch());
            event.setTo(frozen);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastUse.remove(event.getPlayer().getUniqueId());
        service.handleQuit(event.getPlayer());
    }
}
