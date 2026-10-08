package com.nltarzan.races.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Writes finished races to the races / runs / run_splits tables. */
public final class RunStore {

    private final Database database;

    public RunStore(Database database) {
        this.database = database;
    }

    /** Saves the race and all its runs in one transaction. Completes with the new race id. */
    public CompletableFuture<Long> save(RaceRecord race) {
        return database.submit(c -> {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                long raceId = insertRace(c, race);
                for (RaceRecord.Run run : race.runs()) {
                    long runId = insertRun(c, raceId, race.course(), run);
                    insertSplits(c, runId, run);
                }
                c.commit();
                return raceId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        });
    }

    /** One saved run, as read back for the leaderboards. */
    public record RunRow(long raceId, UUID playerId, String playerName, RunStatus status, Long timeMs, Integer points,
                         Integer placing, long completedAt) {
    }

    /** Every run ever recorded on a course, oldest first. */
    public CompletableFuture<List<RunRow>> runsForCourse(String course) {
        return database.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT race_id, player_uuid, player_name, status, time_ms, points, placing, completed_at
                    FROM runs WHERE course = ? ORDER BY id""")) {
                ps.setString(1, course);
                List<RunRow> rows = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RunStatus status;
                        try {
                            status = RunStatus.valueOf(rs.getString("status"));
                        } catch (IllegalArgumentException e) {
                            continue; // a status this version doesn't know
                        }
                        rows.add(new RunRow(rs.getLong("race_id"), UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("player_name"), status, nullableLong(rs, "time_ms"),
                                nullableInt(rs, "points"), nullableInt(rs, "placing"), rs.getLong("completed_at")));
                    }
                }
                return rows;
            }
        });
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Deletes every recorded race, run and split for a course. Completes with the number of runs removed. */
    public CompletableFuture<Integer> deleteCourse(String course) {
        return database.submit(c -> {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // Explicit deletes rather than ON DELETE CASCADE: SQLite only cascades with foreign keys switched on.
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM run_splits WHERE run_id IN (SELECT id FROM runs WHERE course = ?)")) {
                    ps.setString(1, course);
                    ps.executeUpdate();
                }
                int runs;
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM runs WHERE course = ?")) {
                    ps.setString(1, course);
                    runs = ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM races WHERE course = ?")) {
                    ps.setString(1, course);
                    ps.executeUpdate();
                }
                c.commit();
                return runs;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        });
    }

    private static long insertRace(Connection c, RaceRecord race) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO races (course, format, travel, vehicle, started_at, ended_at) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, race.course());
            ps.setString(2, race.format().name());
            ps.setString(3, race.travel().name());
            ps.setString(4, race.vehicle() == null ? null : race.vehicle().name());
            ps.setLong(5, race.startedAt());
            ps.setLong(6, race.endedAt());
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private static long insertRun(Connection c, long raceId, String course, RaceRecord.Run run) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO runs (race_id, course, player_uuid, player_name, status, dq_reason, time_ms, points,
                                  placing, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, raceId);
            ps.setString(2, course);
            ps.setString(3, run.playerId().toString());
            ps.setString(4, run.playerName());
            ps.setString(5, run.status().name());
            ps.setString(6, run.reason());
            setNullableLong(ps, 7, run.timeMs());
            setNullableLong(ps, 8, run.points() == null ? null : run.points().longValue());
            setNullableLong(ps, 9, run.placing() == null ? null : run.placing().longValue());
            ps.setLong(10, run.completedAt());
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private static void insertSplits(Connection c, long runId, RaceRecord.Run run) throws SQLException {
        if (run.splits().isEmpty()) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO run_splits (run_id, checkpoint, time_ms) VALUES (?, ?, ?)")) {
            for (int i = 0; i < run.splits().size(); i++) {
                ps.setLong(1, runId);
                ps.setInt(2, i + 1);
                ps.setLong(3, run.splits().get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static long generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("No id returned for inserted row");
            }
            return keys.getLong(1);
        }
    }
}
