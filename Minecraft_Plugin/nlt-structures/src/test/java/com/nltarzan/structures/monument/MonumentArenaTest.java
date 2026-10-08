package com.nltarzan.structures.monument;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonumentArenaTest {

    private static final double RADIUS = 40;

    @Test
    void farCornersOfTheMonumentCountAsInside() {
        // Bottom corners are about 40.3 blocks away, top corners about 45.6: outside the 40-block sphere.
        assertTrue(MonumentArena.isAround(28.5, 0.5, -28.5, RADIUS));
        assertTrue(MonumentArena.isAround(-28.5, 0.5, 28.5, RADIUS));
        assertTrue(MonumentArena.isAround(28.5, 20.5, 28.5, RADIUS));
        assertTrue(MonumentArena.isAround(-28.5, 20.5, -28.5, RADIUS));
        assertTrue(MonumentArena.isAround(-28.5, -0.5, -28.5, RADIUS)); // the shell's floor, 1 below the center
    }

    @Test
    void sphereStillCountsOutsideTheBox() {
        assertTrue(MonumentArena.isAround(0, 35, 0, RADIUS));   // above the roof, within 40
        assertTrue(MonumentArena.isAround(38, 0, 0, RADIUS));   // just outside the east side, within 40
        assertFalse(MonumentArena.isAround(0, 41, 0, RADIUS));  // too high
        assertFalse(MonumentArena.isAround(35, 0, 35, RADIUS)); // beyond a corner and the sphere
    }

    @Test
    void smallRadiusStillCoversTheWholeBox() {
        assertTrue(MonumentArena.isAround(28.5, 20.5, -28.5, 5));
        assertFalse(MonumentArena.isAround(29.5, 0, 0, 5));
    }

    @Test
    void boxEdges() {
        assertTrue(MonumentArena.inBox(-29, -1, -29));
        assertTrue(MonumentArena.inBox(28, 20, 28));
        assertFalse(MonumentArena.inBox(-30, 0, 0));
        assertFalse(MonumentArena.inBox(29, 0, 0));
        assertFalse(MonumentArena.inBox(0, -2, 0));
        assertFalse(MonumentArena.inBox(0, 21, 0));
    }
}
