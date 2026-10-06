package com.nltarzan.structures.monument.layout;

import java.util.random.RandomGenerator;

/**
 * Layout settings from monuments.yml.
 *
 * <p>Every monument picks its own newest chance (see {@link GrowingTreeGenerator}) from a normal
 * distribution: most monuments land close to {@code peak}, fewer the further away, and never
 * outside {@code min}..{@code max}.
 *
 * @param peak           the most common newest chance (0-1)
 * @param spread         standard deviation; about 2 in 3 monuments fall within peak +/- spread
 * @param min            lowest allowed newest chance
 * @param max            highest allowed newest chance
 * @param minSpongeRooms sponge rooms every layout gets at least
 */
public record MazeSettings(double peak, double spread, double min, double max, int minSpongeRooms) {

    public static final MazeSettings DEFAULT = new MazeSettings(0.65, 0.15, 0.0, 1.0, 1);

    private static final int MAX_DRAWS = 100;

    public MazeSettings {
        if (min < 0 || max > 1 || max < min) {
            throw new IllegalArgumentException("Newest chance must satisfy 0 <= min <= max <= 1, got " + min + ".." + max);
        }
        if (peak < min || peak > max) {
            throw new IllegalArgumentException("Peak " + peak + " is outside " + min + ".." + max);
        }
        if (spread < 0) {
            throw new IllegalArgumentException("Spread can't be negative: " + spread);
        }
        if (minSpongeRooms < 0) {
            throw new IllegalArgumentException("Minimum sponge rooms can't be negative: " + minSpongeRooms);
        }
    }

    /** Settings that always use this newest chance, for previews and tests. */
    public MazeSettings withNewestChance(double chance) {
        return new MazeSettings(chance, 0, chance, chance, minSpongeRooms);
    }

    public MazeSettings withMinSpongeRooms(int min) {
        return new MazeSettings(peak, spread, this.min, max, min);
    }

    /** Draws a newest chance: normal distribution around the peak, redrawn when it falls outside min..max. */
    double drawNewestChance(RandomGenerator random) {
        // Always draw once, so a fixed chance (spread 0) uses the random numbers the same way as a drawn one.
        double value = peak + spread * random.nextGaussian();
        for (int draw = 1; (value < min || value > max) && draw < MAX_DRAWS; draw++) {
            value = peak + spread * random.nextGaussian();
        }
        return Math.clamp(value, min, max);
    }
}
