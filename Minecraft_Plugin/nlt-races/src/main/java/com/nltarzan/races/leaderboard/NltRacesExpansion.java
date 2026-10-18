package com.nltarzan.races.leaderboard;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.leaderboard.CourseBoard.Entry;
import com.nltarzan.races.leaderboard.CourseBoard.PlayerBoard;
import com.nltarzan.races.util.Times;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

/**
 * %nltraces_&lt;race&gt;_...% placeholders, answered from {@link LeaderboardService}'s cache:
 * <ul>
 *   <li>{@code last_<1-10>}: the last group/points race</li>
 *   <li>{@code day_<1-5>}, {@code week_<1-5>}, {@code month_<1-5>}: best per player in the period</li>
 *   <li>{@code cycle_<1-5>}: rotates through day, week and month (see {@link #cyclePeriod()})</li>
 *   <li>{@code player_last}, {@code player_day}, {@code player_week}, {@code player_month}, {@code player_cycle}, {@code player_pb}:
 *       the viewing player's own results</li>
 * </ul>
 * Each can end in {@code _name} or {@code _value} to get just that part.
 * Plus {@code %nltraces_cycle_title%} (no race): the title of the period the cycle placeholders show now.
 */
public final class NltRacesExpansion extends PlaceholderExpansion {

    private final NltRacesPlugin plugin;

    public NltRacesExpansion(NltRacesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nltraces";
    }

    @Override
    public String getAuthor() {
        return "nlTarzan";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /** Keep the expansion when PlaceholderAPI reloads; it belongs to this plugin, not to a download. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String[] parts = params.toLowerCase(Locale.ROOT).split("_");
        if (parts.length == 2 && parts[0].equals("cycle") && parts[1].equals("title")) {
            // One title for every race: all cycle placeholders switch at the same moment.
            Period period = cyclePeriod();
            return config().getString("placeholders.cycle.titles." + period.key(), period.key());
        }
        if (parts.length < 3) {
            return null;
        }
        CourseBoard board = plugin.getLeaderboards().board(parts[0]);
        if (board == null) {
            return plugin.getRaces().get(parts[0]) == null ? null : empty(); // unknown race / not loaded yet
        }
        String part = parts.length > 3 ? parts[3] : null;
        if (parts[1].equals("player")) {
            if (player == null) {
                return empty();
            }
            PlayerBoard own = board.players().get(player.getUniqueId());
            Entry entry;
            if (own == null) {
                entry = null;
            } else if (parts[2].equals("last")) {
                entry = own.last();
            } else if (parts[2].equals("pb")) {
                entry = own.personalBest();
            } else {
                Period period = parts[2].equals("cycle") ? cyclePeriod() : Period.byKey(parts[2]);
                if (period == null) {
                    return null;
                }
                entry = own.best().get(period);
            }
            return format(entry, part);
        }

        List<Entry> list;
        if (parts[1].equals("last")) {
            list = board.lastRace();
        } else if (parts[1].equals("cycle")) {
            list = board.top().get(cyclePeriod());
        } else {
            Period period = Period.byKey(parts[1]);
            if (period == null) {
                return null;
            }
            list = board.top().get(period);
        }
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        return format(position >= 1 && position <= list.size() ? list.get(position - 1) : null, part);
    }

    /**
     * The period the cycle placeholders show right now: day, week, month, then day again, each for
     * {@code placeholders.cycle.seconds}. Based on the clock, so every hologram switches at the same moment.
     */
    private Period cyclePeriod() {
        long millis = Math.max(1, config().getLong("placeholders.cycle.seconds", 20)) * 1000;
        Period[] periods = Period.values();
        return periods[(int) ((System.currentTimeMillis() / millis) % periods.length)];
    }

    /**
     * @param part null for "name - value", "name" or "value" for just that part
     */
    private String format(Entry entry, String part) {
        if (entry == null) {
            return empty();
        }
        String value = value(entry);
        if ("name".equals(part)) {
            return entry.name();
        }
        if ("value".equals(part)) {
            return value;
        }
        return config().getString("placeholders.entry", "{name} - {value}")
                .replace("{name}", entry.name()).replace("{value}", value);
    }

    private String value(Entry entry) {
        return switch (entry.status()) {
            case DQ -> config().getString("placeholders.dq", "DQ");
            case DNF -> config().getString("placeholders.dnf", "DNF");
            case FINISHED -> entry.points() != null
                    ? config().getString("placeholders.points", "{points} pts")
                    .replace("{points}", String.valueOf(entry.points()))
                    : entry.timeMs() == null ? empty() : Times.formatCompact(entry.timeMs());
        };
    }

    private String empty() {
        return config().getString("placeholders.empty", "---");
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }
}
