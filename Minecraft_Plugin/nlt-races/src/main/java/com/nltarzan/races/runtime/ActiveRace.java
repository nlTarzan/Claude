package com.nltarzan.races.runtime;

import com.nltarzan.races.race.PointZone;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.storage.RaceRecord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** A race that is currently being run. While one exists, its course is locked. */
public final class ActiveRace {

    enum State {
        /** Group/points: the leader pressed Prep; players are joining. */
        PREPARING,
        /** Group/points: the 3-2-1 countdown is running; everyone is frozen on the grid. */
        COUNTDOWN,
        /** The race is on. Solo races start in this state. */
        RUNNING
    }

    private final Race race;
    private State state;
    /** Wall-clock time the race began: solo when Start was pressed, group/points at GO. */
    private long startedAt = System.currentTimeMillis();
    private final long createdAt = System.currentTimeMillis();
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();

    // ---- group / points only
    private UUID leader;
    private final List<Location> spots;
    private BukkitTask countdown;
    /** Results of racers who are done (finished, DQ, DNF), saved together when the race ends. */
    private final List<RaceRecord.Run> results = new ArrayList<>();
    private final List<UUID> participants = new ArrayList<>();
    private int finishedCount;
    /** When everyone still racing becomes DNF (after the first finisher), or -1. */
    private long dnfDeadline = -1;

    // ---- points only
    /** Index of the active point zone in the race's list, or -1. */
    private int zoneIndex = -1;
    /** When the points game ends (time-limit after GO), or -1. */
    private long endsAt = -1;
    /** Where the fake end gateway (the zone's beam) is shown to the racers, or null. */
    private Location beamBlock;

    /** A solo race, running straight away. */
    ActiveRace(Race race) {
        this.race = race;
        this.state = State.RUNNING;
        this.spots = List.of();
    }

    /** A group or points race, preparing, led by {@code leader}. */
    ActiveRace(Race race, Player leader, List<Location> spots) {
        this.race = race;
        this.state = State.PREPARING;
        this.leader = leader.getUniqueId();
        this.spots = spots;
    }

    Racer add(Player player) {
        Racer racer = new Racer(player);
        racers.put(player.getUniqueId(), racer);
        if (!participants.contains(player.getUniqueId())) {
            participants.add(player.getUniqueId());
        }
        return racer;
    }

    void remove(Racer racer) {
        racers.remove(racer.player().getUniqueId());
    }

    public Race race() {
        return race;
    }

    State state() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    boolean isSolo() {
        return leader == null;
    }

    boolean isLeader(Player player) {
        return player.getUniqueId().equals(leader);
    }

    long startedAt() {
        return startedAt;
    }

    void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    long createdAt() {
        return createdAt;
    }

    Collection<Racer> racers() {
        return Collections.unmodifiableCollection(racers.values());
    }

    Racer racer(Player player) {
        return racers.get(player.getUniqueId());
    }

    boolean isEmpty() {
        return racers.isEmpty();
    }

    /** Spot 1 (index 0) is kept for the leader; joiners take the next free one. Null when full. */
    Location nextJoinSpot() {
        int index = racers.size(); // the leader is already counted, so this skips spot 1
        return index < spots.size() ? spots.get(index).clone() : null;
    }

    Location leaderSpot() {
        return spots.isEmpty() ? null : spots.getFirst().clone();
    }

    int maxPlayers() {
        return spots.size();
    }

    BukkitTask countdown() {
        return countdown;
    }

    void setCountdown(BukkitTask countdown) {
        this.countdown = countdown;
    }

    void addResult(RaceRecord.Run run) {
        results.add(run);
    }

    List<RaceRecord.Run> results() {
        return results;
    }

    /** Everyone who took part, for announcing the results. */
    List<UUID> participants() {
        return participants;
    }

    /** Counts a finisher and returns their place (1 = first). */
    int nextPlacing() {
        return ++finishedCount;
    }

    long dnfDeadline() {
        return dnfDeadline;
    }

    void setDnfDeadline(long dnfDeadline) {
        this.dnfDeadline = dnfDeadline;
    }

    /** The active point zone, or null. */
    PointZone zone() {
        return zoneIndex < 0 ? null : race.getPointZones().get(zoneIndex);
    }

    /** Moves to a random zone, never the same one twice in a row. */
    void nextZone() {
        int count = race.getPointZones().size();
        if (count == 0) {
            zoneIndex = -1;
            return;
        }
        int next = ThreadLocalRandom.current().nextInt(count);
        if (count > 1 && next == zoneIndex) {
            next = (next + 1 + ThreadLocalRandom.current().nextInt(count - 1)) % count;
        }
        zoneIndex = next;
    }

    long endsAt() {
        return endsAt;
    }

    Location beamBlock() {
        return beamBlock;
    }

    void setBeamBlock(Location beamBlock) {
        this.beamBlock = beamBlock;
    }

    void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }
}
