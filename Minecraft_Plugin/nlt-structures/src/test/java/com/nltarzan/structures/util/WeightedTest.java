package com.nltarzan.structures.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedTest {

    @Test
    void zeroWeightIsNeverPicked() {
        List<Weighted<String>> entries = List.of(new Weighted<>("empty", 0), new Weighted<>("full", 10));
        Random random = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            assertEquals(Optional.of("full"), Weighted.pick(entries, random));
        }
    }

    @Test
    void picksRoughlyByWeight() {
        List<Weighted<String>> entries = List.of(new Weighted<>("a", 30), new Weighted<>("b", 70));
        Random random = new Random(42);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            counts.merge(Weighted.pick(entries, random).orElseThrow(), 1, Integer::sum);
        }
        int a = counts.getOrDefault("a", 0);
        assertTrue(a > 2_700 && a < 3_300, "expected ~3000 picks of a, got " + a);
    }

    @Test
    void emptyWhenNothingHasWeight() {
        assertFalse(Weighted.pick(List.<Weighted<String>>of(), new Random()).isPresent());
        assertFalse(Weighted.pick(List.of(new Weighted<>("x", 0)), new Random()).isPresent());
    }

    @Test
    void rejectsNegativeWeight() {
        assertThrows(IllegalArgumentException.class, () -> new Weighted<>("x", -1));
    }
}
