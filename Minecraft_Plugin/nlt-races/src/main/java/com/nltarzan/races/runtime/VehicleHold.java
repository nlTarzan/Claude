package com.nltarzan.races.runtime;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps a vehicle on its grid spot until GO: ridden animals get zero speed and jump, minecarts a zero
 * max speed, and anything that still drifts (boats) is put back on its spot every tick.
 */
final class VehicleHold {

    private static final List<Attribute> HELD_ATTRIBUTES =
            List.of(Attribute.MOVEMENT_SPEED, Attribute.FLYING_SPEED, Attribute.JUMP_STRENGTH);
    /** How far (squared, in blocks) a held vehicle may drift before it's put back. */
    private static final double DRIFT_SQUARED = 0.2 * 0.2;

    private final Map<Attribute, Double> savedAttributes = new HashMap<>();
    private Double savedMinecartSpeed;

    /** Stops the vehicle from moving, remembering its original values. */
    static VehicleHold hold(Entity vehicle) {
        VehicleHold hold = new VehicleHold();
        if (vehicle instanceof LivingEntity living) {
            for (Attribute attribute : HELD_ATTRIBUTES) {
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null) {
                    hold.savedAttributes.put(attribute, instance.getBaseValue());
                    instance.setBaseValue(0);
                }
            }
        }
        if (vehicle instanceof Minecart minecart) {
            hold.savedMinecartSpeed = minecart.getMaxSpeed();
            minecart.setMaxSpeed(0);
        }
        return hold;
    }

    /** Gives the vehicle its speed back (at GO). */
    void release(Entity vehicle) {
        if (vehicle instanceof LivingEntity living) {
            savedAttributes.forEach((attribute, value) -> {
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null) {
                    instance.setBaseValue(value);
                }
            });
        }
        if (vehicle instanceof Minecart minecart && savedMinecartSpeed != null) {
            minecart.setMaxSpeed(savedMinecartSpeed);
        }
    }

    /**
     * Puts a sideways-drifting vehicle (and its rider) back above its spot. Only horizontal movement is
     * corrected: the vehicle keeps its height and falling speed, so it can settle onto water or ground.
     */
    static void keepOnSpot(Entity vehicle, Location spot) {
        Location now = vehicle.getLocation();
        if (!now.getWorld().equals(spot.getWorld())) {
            return;
        }
        double dx = now.getX() - spot.getX();
        double dz = now.getZ() - spot.getZ();
        if (dx * dx + dz * dz <= DRIFT_SQUARED) {
            return;
        }
        Location target = new Location(spot.getWorld(), spot.getX(), now.getY(), spot.getZ(),
                now.getYaw(), now.getPitch());
        Vector velocity = vehicle.getVelocity();
        vehicle.teleport(target); // since Paper 1.21.10 the rider stays on board by default
        vehicle.setVelocity(new Vector(0, velocity.getY(), 0));
    }
}
