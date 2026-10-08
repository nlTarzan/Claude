package com.nltarzan.races.race;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTest {

    private final Region region = new Region("world", 10, 64, -5, 12, 66, -3);

    @Test
    void serializeAndParseRoundTrip() {
        assertEquals("world,10,64,-5,12,66,-3", region.serialize());
        assertEquals(region, Region.parse(region.serialize()));
        assertEquals(region, Region.parse(" world , 10, 64, -5, 12, 66, -3 "));
    }

    @Test
    void brokenTextIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Region.parse("world,10,64,-5"));
        assertThrows(NumberFormatException.class, () -> Region.parse("world,a,64,-5,12,66,-3"));
    }

    @Test
    void bothCornerBlocksAreInside() {
        assertTrue(region.contains("world", 10, 64, -5));
        // The far corner block reaches up to (but not including) max + 1.
        assertTrue(region.contains("world", 12.99, 66.99, -3.01));
        assertFalse(region.contains("world", 13, 65, -4));
        assertFalse(region.contains("world", 9.99, 65, -4));
        assertFalse(region.contains("world", 11, 67, -4));
        assertFalse(region.contains("world", 11, 65, -2));
    }

    @Test
    void otherWorldIsNeverInside() {
        assertFalse(region.contains("world_nether", 11, 65, -4));
    }
}
