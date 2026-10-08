package com.nltarzan.races.leaderboard;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.storage.RunStore;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Keeps every race's leaderboard in memory so placeholders never wait for the database. A race's board is
 * rebuilt (off the main thread) after each saved race, on startup and reload, and when a day, week or month
 * rolls over.
 */
public final class LeaderboardService {

    private final NltRacesPlugin plugin;
    private final RunStore runStore;
    private final Map<String, CourseBoard> boards = new ConcurrentHashMap<>();
    private final BukkitTask rolloverCheck;
    private long currentDayStart;

    public LeaderboardService(NltRacesPlugin plugin, RunStore runStore) {
        this.plugin = plugin;
        this.runStore = runStore;
        this.currentDayStart = Period.DAY.start(zone(), weekStart());
        this.rolloverCheck = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkRollover, 1200L, 1200L);
    }

    /** The board for a race, or null if it hasn't been loaded (yet). Safe to call from any thread. */
    public CourseBoard board(String race) {
        return boards.get(race.toLowerCase(Locale.ROOT));
    }

    /** Rebuilds one race's board. Call on the main thread (it reads the race's format). */
    public void refresh(Race race) {
        String name = race.getName();
        boolean points = race.getFormat() == RaceFormat.POINTS;
        boolean solo = race.getFormat() == RaceFormat.SOLO;
        Map<Period, Long> starts = new EnumMap<>(Period.class);
        for (Period period : Period.values()) {
            starts.put(period, period.start(zone(), weekStart()));
        }
        runStore.runsForCourse(name)
                .thenApply(rows -> CourseBoard.compute(rows, points, solo, starts))
                .whenComplete((board, error) -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Could not load the leaderboard of " + name, error);
                    } else {
                        boards.put(name, board);
                    }
                });
    }

    public void refreshAll() {
        boards.keySet().retainAll(plugin.getRaces().names());
        plugin.getRaces().all().forEach(this::refresh);
    }

    public void remove(String race) {
        boards.remove(race.toLowerCase(Locale.ROOT));
    }

    /** Every minute: when a new day starts (and so maybe a new week or month), rebuild all boards. */
    private void checkRollover() {
        long dayStart = Period.DAY.start(zone(), weekStart());
        if (dayStart != currentDayStart) {
            currentDayStart = dayStart;
            refreshAll();
        }
    }

    public void shutdown() {
        rolloverCheck.cancel();
    }

    private ZoneId zone() {
        String zone = plugin.getConfig().getString("leaderboards.time-zone", "system");
        try {
            return zone.equalsIgnoreCase("system") ? ZoneId.systemDefault() : ZoneId.of(zone);
        } catch (Exception e) {
            plugin.getLogger().warning("leaderboards.time-zone '" + zone + "' isn't a valid time zone; using the "
                    + "server's own.");
            return ZoneId.systemDefault();
        }
    }

    private DayOfWeek weekStart() {
        String day = plugin.getConfig().getString("leaderboards.week-start", "MONDAY");
        try {
            return DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DayOfWeek.MONDAY;
        }
    }
}
