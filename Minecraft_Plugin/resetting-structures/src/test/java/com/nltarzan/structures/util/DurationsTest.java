package com.nltarzan.structures.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationsTest {

    @Test
    void parsesScheduleUnits() {
        assertEquals(5, Durations.parseTicks("5t"));
        assertEquals(200, Durations.parseTicks("10s"));
        assertEquals(24_000, Durations.parseTicks("1d"));
        assertEquals(1_728_000, Durations.parseTicks("72d"));
        assertEquals(36_000, Durations.parseTicks("1.5d"));
    }

    @Test
    void plainNumberIsTicks() {
        assertEquals(40, Durations.parseTicks("40"));
        assertEquals(40, Durations.parseTicks(" 40 "));
    }

    @Test
    void rejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseTicks(""));
        assertThrows(IllegalArgumentException.class, () -> Durations.parseTicks("abc"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parseTicks("5m"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parseTicks("0s"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parseTicks("-1d"));
    }

    @Test
    void formatsRealTime() {
        assertEquals("5s", Durations.format(100));
        assertEquals("20m 0s", Durations.format(24_000));
        assertEquals("24h 0m 0s", Durations.format(1_728_000));
    }
}
