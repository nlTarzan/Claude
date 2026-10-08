package com.nltarzan.races.runtime;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Spawns a ready-to-ride vehicle: adult, tamed by the racer, saddled or harnessed, never saved to disk. */
final class VehicleSpawner {

    private VehicleSpawner() {
    }

    /**
     * Returns the spawned vehicle, or null if this server doesn't know the entity.
     *
     * @param entityId e.g. "horse" or "dark_oak_boat" (see {@link com.nltarzan.races.race.Race#vehicleEntityId()})
     */
    static Entity spawn(String entityId, Location at, Player rider) {
        EntityType entityType = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(entityId));
        if (entityType == null || entityType.getEntityClass() == null || at.getWorld() == null) {
            return null;
        }
        return at.getWorld().spawn(at, entityType.getEntityClass(), entity -> prepare(entity, rider));
    }

    private static void prepare(Entity entity, Player rider) {
        entity.setPersistent(false); // a crash or restart can't leave race vehicles lying around
        entity.setInvulnerable(true);
        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
        }
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(rider);
        }
        if (entity instanceof LivingEntity living && living.getEquipment() != null) {
            EntityEquipment equipment = living.getEquipment();
            if (living instanceof HappyGhast) {
                equipment.setItem(EquipmentSlot.BODY, new ItemStack(Material.WHITE_HARNESS));
                equipment.setDropChance(EquipmentSlot.BODY, 0f);
            } else if (living.canUseEquipmentSlot(EquipmentSlot.SADDLE)) {
                equipment.setItem(EquipmentSlot.SADDLE, new ItemStack(Material.SADDLE));
                equipment.setDropChance(EquipmentSlot.SADDLE, 0f);
            }
        }
    }
}
