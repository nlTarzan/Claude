package com.nltarzan.races.storage;

import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.TravelMode;
import com.nltarzan.races.race.VehicleType;

import java.util.List;
import java.util.UUID;

/** A completed race, ready to be saved: one row in {@code races} plus one row per player in {@code runs}. */
public record RaceRecord(String course, RaceFormat format, TravelMode travel, VehicleType vehicle,
                         long startedAt, long endedAt, List<Run> runs) {

    /**
     * One player's result.
     *
     * @param timeMs   finish time, null for DQ/DNF
     * @param points   points scored (points races only), otherwise null
     * @param placing  1 = first, null for solo races
     * @param reason   why the run was a DQ or DNF, e.g. "logout"; null when finished
     * @param splits   time at each middle checkpoint, in order
     */
    public record Run(UUID playerId, String playerName, RunStatus status, String reason, Long timeMs,
                      Integer points, Integer placing, long completedAt, List<Long> splits) {
    }
}
