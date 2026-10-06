package com.nltarzan.structures.util;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** A value with a chance weight. Replaces the datapack's {@code random value 1..100} + range checks. */
public record Weighted<T>(T value, int weight) {

    public Weighted {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight can't be negative: " + weight);
        }
    }

    /** Picks one value proportional to its weight. Empty when nothing has a weight above 0. */
    public static <T> Optional<T> pick(List<Weighted<T>> entries, RandomGenerator random) {
        int total = entries.stream().mapToInt(Weighted::weight).sum();
        if (total <= 0) {
            return Optional.empty();
        }

        int roll = random.nextInt(total);
        for (Weighted<T> entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return Optional.of(entry.value());
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
