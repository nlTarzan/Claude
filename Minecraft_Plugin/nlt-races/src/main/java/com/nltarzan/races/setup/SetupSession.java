package com.nltarzan.races.setup;

import com.nltarzan.races.race.Race;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An admin working through setup steps for one race: the guided setup, or a single /nltraces set. */
final class SetupSession {

    final Race race;
    final String label;
    final List<SetupStep> steps;
    /** False for a single /nltraces set command: no step numbers or buttons, ends after its step. */
    final boolean guided;
    /** Continuing a part-finished setup: steps that are already set are passed over. */
    final boolean onlyMissing;

    int index;
    /** First corner of a region step, while waiting for the second. */
    Location firstCorner;

    /** The step index the per-visit counters below belong to; reset when a step is (re)entered. */
    int visitIndex = -1;
    /** Grid spots or point zones placed since arriving at that step; these are what Undo removes. */
    int createdThisVisit;

    /** Marker blocks shown to the admin, per step id (see {@link SetupMarkers}). */
    final Map<String, List<Location>> markers = new LinkedHashMap<>();
    final Map<String, BlockData> markerData = new HashMap<>();

    SetupSession(Race race, String label, List<SetupStep> steps, boolean guided, boolean onlyMissing) {
        this.race = race;
        this.label = label;
        this.steps = steps;
        this.guided = guided;
        this.onlyMissing = onlyMissing;
    }

    SetupStep current() {
        return index < steps.size() ? steps.get(index) : null;
    }

    /**
     * The guided grid-spot or point-zone step: the first click replaces the ones already saved (Next keeps
     * them), and Undo removes the ones placed during this visit.
     */
    boolean isRebuildStep() {
        SetupStep step = current();
        return guided && step != null && SetupSteps.isSetOfSpots(step);
    }
}
