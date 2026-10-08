package com.nltarzan.races.race;

import org.bukkit.Material;

/**
 * Vehicles the plugin can spawn for VEHICLE races. Entities are looked up by their Minecraft id
 * (not by Java enum constant) so renames between Minecraft versions don't break the plugin.
 */
public enum VehicleType {
    BOAT("oak_boat", null),
    MINECART("minecart", null),
    HORSE("horse", null),
    DONKEY("donkey", null),
    MULE("mule", null),
    CAMEL("camel", null),
    PIG("pig", Material.CARROT_ON_A_STICK),
    STRIDER("strider", Material.WARPED_FUNGUS_ON_A_STICK),
    HAPPY_GHAST("happy_ghast", null),
    NAUTILUS("nautilus", null);

    private final String entityId;
    private final Material steeringItem;

    VehicleType(String entityId, Material steeringItem) {
        this.entityId = entityId;
        this.steeringItem = steeringItem;
    }

    /** The minecraft:... entity id to spawn. */
    public String entityId() {
        return entityId;
    }

    /** The item a rider must hold to steer (pig, strider), or null. */
    public Material steeringItem() {
        return steeringItem;
    }
}
