package com.nltarzan.races.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimesTest {

    @Test
    void formatsRaceTimes() {
        assertEquals("0.000", Times.format(0));
        assertEquals("23.456", Times.format(23_456));
        assertEquals("59.999", Times.format(59_999));
        assertEquals("1:00.000", Times.format(60_000));
        assertEquals("1:23.456", Times.format(83_456));
        assertEquals("12:03.007", Times.format(723_007));
    }

    @Test
    void formatsWholeSeconds() {
        assertEquals("0:00", Times.formatSeconds(0));
        assertEquals("0:45", Times.formatSeconds(45));
        assertEquals("5:00", Times.formatSeconds(300));
        assertEquals("61:01", Times.formatSeconds(3_661));
    }
}
