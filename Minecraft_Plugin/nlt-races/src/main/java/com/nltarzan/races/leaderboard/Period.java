package com.nltarzan.races.leaderboard;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** The leaderboard periods. Each resets at its start: midnight, the first day of the week, the 1st of the month. */
public enum Period {
    DAY,
    WEEK,
    MONTH;

    /** When the current period began, in epoch milliseconds. */
    public long start(ZoneId zone, DayOfWeek weekStart) {
        ZonedDateTime today = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone);
        ZonedDateTime start = switch (this) {
            case DAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(weekStart));
            case MONTH -> today.withDayOfMonth(1);
        };
        return start.toInstant().toEpochMilli();
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Period byKey(String key) {
        for (Period period : values()) {
            if (period.key().equals(key)) {
                return period;
            }
        }
        return null;
    }
}
