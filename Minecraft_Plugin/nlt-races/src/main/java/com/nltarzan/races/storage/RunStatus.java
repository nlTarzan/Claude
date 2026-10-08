package com.nltarzan.races.storage;

public enum RunStatus {
    FINISHED,
    /** Disqualified by a rule (reason stored in dq_reason). */
    DQ,
    /** Did not finish: ran out of time or left the race. */
    DNF
}
