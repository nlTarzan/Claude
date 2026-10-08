package com.nltarzan.races.race;

public enum RaceFormat {
    /** One player at a time, time trial. */
    SOLO,
    /** Several players start together; ranked by finish time. */
    GROUP,
    /** Several players race to random point zones; ranked by points. */
    POINTS;

    /** Group and points races are started by a leader and have a grid and podium. */
    public boolean isMultiplayer() {
        return this != SOLO;
    }
}
