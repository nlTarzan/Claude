package com.nltarzan.races.race;

import java.util.Locale;

/** The wood a race's boats are made of. Bamboo spawns a bamboo raft. */
public enum BoatWood {
    OAK,
    SPRUCE,
    BIRCH,
    JUNGLE,
    ACACIA,
    DARK_OAK,
    MANGROVE,
    CHERRY,
    PALE_OAK,
    BAMBOO;

    /** The minecraft:... entity id to spawn, e.g. "dark_oak_boat" or "bamboo_raft". */
    public String entityId() {
        return this == BAMBOO ? "bamboo_raft" : name().toLowerCase(Locale.ROOT) + "_boat";
    }
}
