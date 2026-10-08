package com.nltarzan.races.util;

import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnumsTest {

    @Test
    void parsesWhateverCaseAndDashes() {
        assertEquals(VehicleType.HAPPY_GHAST, Enums.parse(VehicleType.class, "happy-ghast"));
        assertEquals(VehicleType.HAPPY_GHAST, Enums.parse(VehicleType.class, "HAPPY_GHAST"));
        assertEquals(RaceFormat.GROUP, Enums.parse(RaceFormat.class, " Group "));
    }

    @Test
    void unknownOrMissingIsNull() {
        assertNull(Enums.parse(RaceFormat.class, "relay"));
        assertNull(Enums.parse(RaceFormat.class, ""));
        assertNull(Enums.parse(RaceFormat.class, null));
    }

    @Test
    void displayIsWhatPlayersType() {
        assertEquals("happy-ghast", Enums.display(VehicleType.HAPPY_GHAST));
        assertEquals(List.of("solo", "group", "points"), Enums.options(RaceFormat.class));
        // Everything shown can be typed back in.
        for (VehicleType type : VehicleType.values()) {
            assertEquals(type, Enums.parse(VehicleType.class, Enums.display(type)));
        }
    }
}
