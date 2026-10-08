package com.nltarzan.races.race;

/** What happens when a player uses a block linked to a race. */
public enum ButtonAction {
    /** Group/points: announce the race and become its leader. */
    PREP,
    /** Solo: teleport to the start. Group/points: leader starts the countdown. */
    START,
    /** Group/points: teleport to a free grid slot. */
    JOIN,
    /** Teleport to the race's lobby. */
    LOBBY,
    /**
     * Optional, any race: a lever switched on when the countdown starts (solo: when Start is pressed)
     * and off when the race ends, to open redstone gates. Players can't flip it themselves.
     */
    GATE;

    public boolean appliesTo(RaceFormat format) {
        return format.isMultiplayer() || (this != PREP && this != JOIN);
    }
}
