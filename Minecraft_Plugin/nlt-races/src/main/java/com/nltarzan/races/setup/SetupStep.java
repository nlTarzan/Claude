package com.nltarzan.races.setup;

import com.nltarzan.races.race.Race;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One thing to set on a race by right-clicking.
 *
 * @param id        names the step in messages.yml: setup.names.&lt;id&gt;
 * @param repeating stays on this step after each click (grid spots, middle checkpoints, point zones)
 *                  until the admin moves on with "next"
 * @param isSet     whether the race already has this; continuing a setup jumps to the first unset step
 * @param current   the current value shown to the admin, or null when not set
 * @param applier   stores the clicked value (Block, Location or Region, depending on the kind) on the race
 */
public record SetupStep(String id, Kind kind, boolean repeating,
                        Predicate<Race> isSet, Function<Race, String> current, BiConsumer<Race, Object> applier) {

    public enum Kind {
        /** Click a block to link it as a button. Value: Block. */
        BUTTON,
        /** Click a block; the spot on top of it is used. Value: Location. */
        LOCATION,
        /** Click two corner blocks. Value: Region. */
        REGION
    }
}
