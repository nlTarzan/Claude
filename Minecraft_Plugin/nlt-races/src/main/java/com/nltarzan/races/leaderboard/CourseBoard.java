package com.nltarzan.races.leaderboard;

import com.nltarzan.races.storage.RunStatus;
import com.nltarzan.races.storage.RunStore.RunRow;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Everything the placeholders show for one race, worked out once from its saved runs and then only read.
 *
 * @param lastRace the last group/points race: finishers by placing, then DQ and DNF (empty for solo races)
 * @param top      per period, each player's best, best first (top 5)
 * @param players  per player: last run, best per period, personal best
 */
public record CourseBoard(List<Entry> lastRace, Map<Period, List<Entry>> top, Map<UUID, PlayerBoard> players) {

    public static final int LAST_RACE_SIZE = 10;
    public static final int TOP_SIZE = 5;

    /** One result: who, and either a time, points, DQ or DNF. */
    public record Entry(String name, RunStatus status, Long timeMs, Integer points) {
    }

    public record PlayerBoard(Entry last, Map<Period, Entry> best, Entry personalBest) {
    }

    /**
     * @param pointsRace rank by points (most first, ties to whoever got there sooner) instead of by time
     * @param solo       solo races have no "last race" list
     * @param periodStarts when each period began, in epoch milliseconds
     */
    static CourseBoard compute(List<RunRow> rows, boolean pointsRace, boolean solo, Map<Period, Long> periodStarts) {
        Map<UUID, String> names = latestNames(rows);
        Comparator<RunRow> better = pointsRace
                ? Comparator.comparing((RunRow row) -> -row.points())
                .thenComparing(row -> row.timeMs() == null ? Long.MAX_VALUE : row.timeMs())
                .thenComparingLong(RunRow::completedAt)
                : Comparator.comparingLong((RunRow row) -> row.timeMs()).thenComparingLong(RunRow::completedAt);
        Predicate<RunRow> counts = row -> row.status() == RunStatus.FINISHED
                && (pointsRace ? row.points() != null : row.timeMs() != null);

        Map<UUID, RunRow> personalBests = bestPerPlayer(rows, counts, better);
        Map<Period, Map<UUID, RunRow>> periodBests = new EnumMap<>(Period.class);
        Map<Period, List<Entry>> top = new EnumMap<>(Period.class);
        for (Period period : Period.values()) {
            long start = periodStarts.get(period);
            Map<UUID, RunRow> bests = bestPerPlayer(rows, counts.and(row -> row.completedAt() >= start), better);
            periodBests.put(period, bests);
            top.put(period, bests.values().stream().sorted(better).limit(TOP_SIZE)
                    .map(row -> entry(row, names)).toList());
        }

        Map<UUID, RunRow> lastRuns = new HashMap<>();
        for (RunRow row : rows) { // oldest first, so the last one per player wins
            lastRuns.put(row.playerId(), row);
        }
        Map<UUID, PlayerBoard> players = new HashMap<>();
        for (UUID player : lastRuns.keySet()) {
            Map<Period, Entry> best = new EnumMap<>(Period.class);
            for (Period period : Period.values()) {
                RunRow row = periodBests.get(period).get(player);
                if (row != null) {
                    best.put(period, entry(row, names));
                }
            }
            RunRow pb = personalBests.get(player);
            players.put(player, new PlayerBoard(entry(lastRuns.get(player), names), best,
                    pb == null ? null : entry(pb, names)));
        }
        return new CourseBoard(solo ? List.of() : lastRace(rows, names), top, players);
    }

    /** The newest group/points race: finishers by placing, then DQ, then DNF. */
    private static List<Entry> lastRace(List<RunRow> rows, Map<UUID, String> names) {
        long lastRaceId = rows.stream().mapToLong(RunRow::raceId).max().orElse(-1);
        return rows.stream()
                .filter(row -> row.raceId() == lastRaceId)
                .sorted(Comparator.comparing((RunRow row) -> row.placing() == null ? Integer.MAX_VALUE : row.placing())
                        .thenComparing(row -> row.status().ordinal()))
                .limit(LAST_RACE_SIZE)
                .map(row -> entry(row, names))
                .toList();
    }

    private static Map<UUID, RunRow> bestPerPlayer(Collection<RunRow> rows, Predicate<RunRow> counts,
                                                   Comparator<RunRow> better) {
        Map<UUID, RunRow> bests = new LinkedHashMap<>();
        for (RunRow row : rows) {
            if (counts.test(row)) {
                bests.merge(row.playerId(), row, (old, candidate) -> better.compare(candidate, old) < 0 ? candidate : old);
            }
        }
        return bests;
    }

    /** Each player's most recent name, so a renamed player shows their current name everywhere. */
    private static Map<UUID, String> latestNames(List<RunRow> rows) {
        Map<UUID, String> names = new HashMap<>();
        for (RunRow row : rows) {
            names.put(row.playerId(), row.playerName());
        }
        return names;
    }

    private static Entry entry(RunRow row, Map<UUID, String> names) {
        return new Entry(names.getOrDefault(row.playerId(), row.playerName()), row.status(), row.timeMs(), row.points());
    }
}
