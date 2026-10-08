package com.nltarzan.structures.monument.listener;

import com.nltarzan.structures.monument.Monuments;
import com.nltarzan.structures.monument.MonumentArena;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Replaces the monument_elder_guardian_killed advancement and monument_elder_kill.mcfunction:
 * when a player kills the last Elder Guardian of the monument, they get the Monument Hoe, an iron
 * hoe whose durability is exactly the number of sponges in the monument.
 */
public final class ElderGuardianListener implements Listener {

    private final Monuments monuments;
    private final MonumentHoe hoe;

    public ElderGuardianListener(Monuments monuments, MonumentHoe hoe) {
        this.monuments = monuments;
        this.hoe = hoe;
    }

    @EventHandler(ignoreCancelled = true)
    public void onElderGuardianDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ElderGuardian elder) || !monuments.settings().rewardEnabled()) {
            return;
        }
        Player killer = elder.getKiller();
        MonumentArena arena = monuments.arena();
        if (killer == null || arena == null || !arena.contains(elder.getLocation())) {
            return;
        }
        boolean othersAlive = arena.entities(ElderGuardian.class).stream().anyMatch(other -> !other.equals(elder));
        if (othersAlive) {
            return;
        }

        // The count from when the monument was placed; counted now for a monument placed before the
        // plugin kept track of it.
        int sponges = monuments.currentMonument().sponges().orElseGet(arena::countSponges);
        if (sponges == 0) {
            return;
        }
        killer.getInventory().addItem(hoe.create(sponges, monuments.currentMonument().id())).values()
                .forEach(leftover -> killer.getWorld().dropItem(killer.getLocation(), leftover));
    }
}
