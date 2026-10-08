package com.nltarzan.races.runtime;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** One player's progress through an active race. */
public final class Racer {

    private final Player player;

    /** True once the timer runs (solo: on entering the start checkpoint; group: at GO). */
    private boolean timing;
    private long startNanos;
    /** True once the start checkpoint has been entered; the other checkpoints only count after it. */
    private boolean startPassed;
    /** When the start was passed (points: GO); the elytra take-off window counts from here. */
    private long startPassedNanos;
    /** Elytra: true once the racer has been gliding since the start; before that, the ground is allowed. */
    private boolean hasTakenOff;
    /** Group/points: true once the racer is on the course (joined, or the leader after pressing Start). */
    private boolean onCourse;
    /** Group/points: the grid spot this racer starts from. */
    private Location spot;
    /** Points races: points scored, and when the last one was scored (for breaking ties). */
    private int points;
    private long lastScoreNanos = Long.MAX_VALUE;
    /** Index of the next middle checkpoint to pass. */
    private int nextCheckpoint;
    private final List<Long> splits = new ArrayList<>();

    /** Where the player was at the previous tick, to check the path they moved. */
    private Location lastLocation;
    private long lastNanos;
    /** Set while standing in the finish with checkpoints missing, so the warning shows once. */
    private boolean warnedAtFinish;

    /** Vehicle races: the vehicle spawned for this racer. */
    private Entity vehicle;
    /** Group/points vehicle races: how the vehicle is held on its grid spot until GO, or null. */
    private VehicleHold hold;
    /** When the racer got off their vehicle or stopped gliding (lenient rules), or -1 while they're fine. */
    private long offSinceNanos = -1;
    /** Last time a "checkpoint doesn't count" warning was shown, to avoid spamming it. */
    private long lastCheckpointWarnNanos;

    Racer(Player player) {
        this.player = player;
        moved(player.getLocation(), System.nanoTime());
    }

    void startTimer(long nanos) {
        timing = true;
        startNanos = nanos;
    }

    boolean startPassed() {
        return startPassed;
    }

    void passStart(long nanos) {
        startPassed = true;
        startPassedNanos = nanos;
    }

    long startPassedNanos() {
        return startPassedNanos;
    }

    boolean hasTakenOff() {
        return hasTakenOff;
    }

    void setTakenOff() {
        hasTakenOff = true;
    }

    boolean onCourse() {
        return onCourse;
    }

    void setOnCourse(boolean onCourse) {
        this.onCourse = onCourse;
    }

    Location spot() {
        return spot == null ? null : spot.clone();
    }

    int points() {
        return points;
    }

    void addPoint(long nanos) {
        points++;
        lastScoreNanos = nanos;
    }

    /** When the racer reached their current score; Long.MAX_VALUE with no points. */
    long lastScoreNanos() {
        return lastScoreNanos;
    }

    void setSpot(Location spot) {
        this.spot = spot.clone();
    }

    void moved(Location location, long nanos) {
        lastLocation = location.clone();
        lastNanos = nanos;
    }

    /** Records passing the next middle checkpoint at {@code elapsedMs}. */
    void passCheckpoint(long elapsedMs) {
        splits.add(elapsedMs);
        nextCheckpoint++;
    }

    long elapsedMillis(long nowNanos) {
        return timing ? (nowNanos - startNanos) / 1_000_000L : 0;
    }

    public Player player() {
        return player;
    }

    boolean isTiming() {
        return timing;
    }

    long startNanos() {
        return startNanos;
    }

    int nextCheckpoint() {
        return nextCheckpoint;
    }

    List<Long> splits() {
        return List.copyOf(splits);
    }

    Location lastLocation() {
        return lastLocation;
    }

    long lastNanos() {
        return lastNanos;
    }

    boolean warnedAtFinish() {
        return warnedAtFinish;
    }

    void setWarnedAtFinish(boolean warnedAtFinish) {
        this.warnedAtFinish = warnedAtFinish;
    }

    Entity vehicle() {
        return vehicle;
    }

    void setVehicle(Entity vehicle) {
        this.vehicle = vehicle;
    }

    VehicleHold hold() {
        return hold;
    }

    void setHold(VehicleHold hold) {
        this.hold = hold;
    }

    long offSinceNanos() {
        return offSinceNanos;
    }

    void setOffSinceNanos(long offSinceNanos) {
        this.offSinceNanos = offSinceNanos;
    }

    long lastCheckpointWarnNanos() {
        return lastCheckpointWarnNanos;
    }

    void setLastCheckpointWarnNanos(long nanos) {
        this.lastCheckpointWarnNanos = nanos;
    }
}
