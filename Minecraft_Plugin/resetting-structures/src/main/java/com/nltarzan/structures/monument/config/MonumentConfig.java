package com.nltarzan.structures.monument.config;

import com.nltarzan.structures.monument.layout.MazeSettings;
import com.nltarzan.structures.util.Durations;
import com.nltarzan.structures.util.Weighted;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Immutable snapshot of monuments.yml. Rebuilt on {@code /structure monument reload}. */
public record MonumentConfig(
        String worldName,
        int centerX,
        int centerY,
        int centerZ,
        double checkRadius,
        List<Weighted<Long>> intervals,
        List<Weighted<Long>> occupiedIntervals,
        ResetMode mode,
        boolean clearGuardians,
        boolean announce,
        List<Weighted<String>> variants,
        double elderYOffset,
        double elderHealth,
        double elderAttackDamage,
        boolean rewardEnabled,
        MazeSettings maze) {

    private static final long FALLBACK_INTERVAL = Durations.TICKS_PER_DAY;
    private static final long FALLBACK_OCCUPIED_INTERVAL = 300 * Durations.TICKS_PER_SECOND;

    public static MonumentConfig load(FileConfiguration config, Logger logger) {
        List<Weighted<Long>> intervals = loadIntervals(config, "reset.intervals", FALLBACK_INTERVAL, logger);
        List<Weighted<Long>> occupied = loadIntervals(config, "reset.occupied-intervals",
                FALLBACK_OCCUPIED_INTERVAL, logger);
        if (longest(occupied) >= shortest(intervals)) {
            logger.warning("reset.occupied-intervals should be shorter than reset.intervals, "
                    + "otherwise a postponed reset waits as long as a normal one.");
        }

        return new MonumentConfig(
                config.getString("location.world", "world"),
                config.getInt("location.x"),
                config.getInt("location.y"),
                config.getInt("location.z"),
                Math.max(1, config.getDouble("location.check-radius", 40)),
                intervals,
                occupied,
                loadMode(config, logger),
                config.getBoolean("reset.clear-guardians", true),
                config.getBoolean("reset.announce", true),
                loadVariants(config, logger),
                config.getDouble("elder-guardian.y-offset", 11.5),
                config.getDouble("elder-guardian.health", 100),
                config.getDouble("elder-guardian.attack-damage", 10),
                config.getBoolean("reward.enabled", true),
                loadMaze(config, logger));
    }

    /**
     * Checks the location: section, which says where the monument is. Without a valid one every
     * coordinate would quietly become 0 and a reset would build a monument at 0, 0, 0.
     *
     * @return what's wrong, or {@code null} when it's fine
     */
    public static String locationProblem(ConfigurationSection config) {
        if (!config.isConfigurationSection("location")) {
            return "has no location: section";
        }
        if (!config.isString("location.world") || config.getString("location.world").isBlank()) {
            return "has no location.world";
        }
        for (String axis : List.of("x", "y", "z")) {
            if (!config.isInt("location." + axis)) {
                return "has no whole number for location." + axis;
            }
        }
        return null;
    }

    /** What a reset places. */
    public enum ResetMode {
        PRE_BUILT,
        GENERATED
    }

    private static ResetMode loadMode(FileConfiguration config, Logger logger) {
        String mode = config.getString("reset.mode", "pre-built");
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "pre-built" -> ResetMode.PRE_BUILT;
            case "generated" -> ResetMode.GENERATED;
            default -> {
                logger.warning("Unknown reset.mode '" + mode + "', use pre-built or generated. Using pre-built.");
                yield ResetMode.PRE_BUILT;
            }
        };
    }

    private static MazeSettings loadMaze(FileConfiguration config, Logger logger) {
        MazeSettings defaults = MazeSettings.DEFAULT;
        double min = clamp(config.getDouble("maze.newest-chance.min", defaults.min()));
        double max = clamp(config.getDouble("maze.newest-chance.max", defaults.max()));
        if (max < min) {
            logger.warning("maze.newest-chance.max is below maze.newest-chance.min, using " + min + " for both.");
            max = min;
        }
        double peak = config.getDouble("maze.newest-chance.peak", defaults.peak());
        if (peak < min || peak > max) {
            logger.warning("maze.newest-chance.peak " + peak + " is outside min..max, moving it inside.");
            peak = Math.clamp(peak, min, max);
        }
        double spread = Math.max(0, config.getDouble("maze.newest-chance.spread", defaults.spread()));
        int sponges = Math.max(0, config.getInt("maze.min-sponge-rooms", defaults.minSpongeRooms()));
        return new MazeSettings(peak, spread, min, max, sponges);
    }

    private static double clamp(double chance) {
        return Math.clamp(chance, 0.0, 1.0);
    }

    private static List<Weighted<Long>> loadIntervals(FileConfiguration config, String path, long fallback,
                                                      Logger logger) {
        List<Weighted<Long>> intervals = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList(path)) {
            Object delay = entry.get("delay");
            Object weight = entry.get("weight");
            try {
                long ticks = Durations.parseTicks(String.valueOf(delay));
                int w = weight == null ? 1 : Integer.parseInt(String.valueOf(weight));
                intervals.add(new Weighted<>(ticks, w));
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping " + path + " entry " + entry + ": " + e.getMessage());
            }
        }
        if (intervals.stream().noneMatch(w -> w.weight() > 0)) {
            logger.warning("No usable " + path + ", falling back to " + Durations.format(fallback) + ".");
            intervals = List.of(new Weighted<>(fallback, 1));
        }
        return List.copyOf(intervals);
    }

    private static long shortest(List<Weighted<Long>> intervals) {
        return intervals.stream().filter(w -> w.weight() > 0).mapToLong(Weighted::value).min().orElse(0);
    }

    private static long longest(List<Weighted<Long>> intervals) {
        return intervals.stream().filter(w -> w.weight() > 0).mapToLong(Weighted::value).max().orElse(0);
    }

    private static List<Weighted<String>> loadVariants(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("variants");
        if (section == null) {
            logger.warning("No variants configured.");
            return List.of();
        }
        List<Weighted<String>> variants = new ArrayList<>();
        for (String name : section.getKeys(false)) {
            int weight = section.getInt(name);
            if (weight < 0) {
                logger.warning("Variant " + name + " has a negative weight, using 0.");
                weight = 0;
            }
            variants.add(new Weighted<>(name, weight));
        }
        return List.copyOf(variants);
    }
}
