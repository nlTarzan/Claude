package com.nltarzan.races.leaderboard;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");

    @Test
    void keysRoundTrip() {
        for (Period period : Period.values()) {
            assertEquals(period, Period.byKey(period.key()));
        }
        assertNull(Period.byKey("year"));
    }

    @Test
    void everyPeriodStartsAtMidnightBeforeNow() {
        long now = System.currentTimeMillis();
        for (Period period : Period.values()) {
            ZonedDateTime start = start(period, DayOfWeek.MONDAY);
            assertTrue(start.toInstant().toEpochMilli() <= now, period + " starts in the future");
            assertEquals(0, start.getHour() * 60 + start.getMinute(), period + " doesn't start at midnight");
        }
    }

    @Test
    void dayIsTodayWeekIsTheWeekStartAndMonthIsTheFirst() {
        ZonedDateTime today = ZonedDateTime.now(ZONE).toLocalDate().atStartOfDay(ZONE);
        assertEquals(today, start(Period.DAY, DayOfWeek.MONDAY));
        for (DayOfWeek weekStart : DayOfWeek.values()) {
            ZonedDateTime week = start(Period.WEEK, weekStart);
            assertEquals(weekStart, week.getDayOfWeek());
            assertTrue(!week.isAfter(today) && week.isAfter(today.minusDays(7)), "week start " + week);
        }
        assertEquals(1, start(Period.MONTH, DayOfWeek.MONDAY).getDayOfMonth());
    }

    private static ZonedDateTime start(Period period, DayOfWeek weekStart) {
        return Instant.ofEpochMilli(period.start(ZONE, weekStart)).atZone(ZONE);
    }
}
