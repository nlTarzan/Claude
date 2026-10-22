package com.nltarzan.structures.monument.build;

/**
 * One step of building a generated monument. Positions are block offsets from the monument
 * center (the datapack's marker), so a plan doesn't depend on where the monument is.
 */
public sealed interface BuildStep {

    /** A block offset from the monument center. */
    record Offset(int x, int y, int z) {
        public Offset plus(int dx, int dy, int dz) {
            return new Offset(x + dx, y + dy, z + dz);
        }
    }

    /** Rotations a template can be placed with, the same as /place template's. */
    enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        COUNTERCLOCKWISE_90
    }

    /** Blocks the builder places directly. */
    enum Block {
        WATER,
        PRISMARINE,
        PRISMARINE_BRICKS,
        WET_SPONGE
    }

    /**
     * Places a template (path below structures/monument/maze/). Like /place template, the origin is
     * the template's 0,0,0 corner and a rotation turns the template around that corner.
     */
    record Place(String template, Offset origin, Rotation rotation) implements BuildStep {
    }

    /** Fills a box, both corners included, like /fill. */
    record Fill(Offset from, Offset to, Block block) implements BuildStep {
    }

    /** Spawns one of the three Elder Guardians at an exact position (offset from the center). */
    record SpawnElder(double x, double y, double z) implements BuildStep {
    }
}
