package com.nltarzan.races.runtime;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.race.BlockPos;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.PointZone;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.RaceSetting;
import com.nltarzan.races.race.Region;
import com.nltarzan.races.race.Rules;
import com.nltarzan.races.race.TravelMode;
import com.nltarzan.races.runtime.ActiveRace.State;
import com.nltarzan.races.setup.RaceInfo;
import com.nltarzan.races.storage.RaceRecord;
import com.nltarzan.races.storage.RunStatus;
import com.nltarzan.races.storage.RunStore;
import com.nltarzan.races.util.Enums;
import com.nltarzan.races.util.Times;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.EndGateway;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Runs races: reacts to buttons, tracks racers every tick, applies the rules and saves results. */
public final class RaceService {

    /** Minimum time between two "checkpoint doesn't count" warnings for the same racer. */
    private static final long CHECKPOINT_WARN_INTERVAL_NANOS = 2_000_000_000L;

    private final NltRacesPlugin plugin;
    private final RunStore runStore;
    private final RaceItems raceItems;
    private final Map<String, ActiveRace> activeRaces = new HashMap<>();
    private final Map<UUID, ActiveRace> racesByPlayer = new HashMap<>();
    /** Spawned race vehicles, mapped to the racer they belong to. */
    private final Map<UUID, UUID> vehicleOwners = new HashMap<>();
    /** Players being teleported by this plugin, so the teleport rule ignores it. */
    private final Set<UUID> internalTeleports = new HashSet<>();
    /** Players disqualified by dying, who respawn at this lobby. */
    private final Map<UUID, Location> pendingRespawns = new HashMap<>();
    private final BukkitTask ticker;
    private long tick;

    public RaceService(NltRacesPlugin plugin, RunStore runStore) {
        this.plugin = plugin;
        this.runStore = runStore;
        this.raceItems = new RaceItems(plugin);
        this.ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private Messages msg() {
        return plugin.getMessages();
    }

    /** True while the race is being run or prepared; its course is locked and shouldn't be edited. */
    public boolean isActive(Race race) {
        return activeRaces.containsKey(race.getName());
    }

    public boolean hasActiveRaces() {
        return !activeRaces.isEmpty();
    }

    RaceItems raceItems() {
        return raceItems;
    }

    /** The race the player is on the course of, or null (a leader who hasn't started yet isn't on it). */
    Race raceOf(Player player) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        return active == null || !active.racer(player).onCourse() ? null : active.race();
    }

    /**
     * Group/points racers on foot or with an elytra waiting on the grid (before GO) can't move, only look
     * around. Vehicle racers are held on their spot differently (see {@link VehicleHold}).
     */
    boolean isFrozen(Player player) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        return active != null && active.state() != State.RUNNING && active.racer(player).onCourse()
                && active.race().getTravel() != TravelMode.VEHICLE;
    }

    /** The racer a spawned race vehicle belongs to, or null if it isn't a race vehicle. */
    UUID vehicleOwner(Entity entity) {
        return vehicleOwners.get(entity.getUniqueId());
    }

    /** The vehicle spawned for this racer, or null. */
    Entity vehicleOf(Player player) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        return active == null ? null : active.racer(player).vehicle();
    }

    boolean isInternalTeleport(Player player) {
        return internalTeleports.contains(player.getUniqueId());
    }

    /**
     * Teleports without triggering the teleport DQ rule. The player gets off anything they're riding first:
     * since Paper 1.21.10 a teleport would otherwise take the mount along.
     */
    private void teleport(Player player, Location to) {
        internalTeleports.add(player.getUniqueId());
        try {
            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
            player.teleport(to);
        } finally {
            internalTeleports.remove(player.getUniqueId());
        }
    }

    /** Where a player disqualified by dying should respawn (once), or null. */
    Location takeRespawn(Player player) {
        return pendingRespawns.remove(player.getUniqueId());
    }

    // ---------------------------------------------------------------- buttons

    public void handleButton(Player player, Race race, ButtonAction action) {
        if (action == ButtonAction.GATE) {
            return; // gate levers are switched by the race, not by players
        }
        if (action == ButtonAction.LOBBY) {
            returnToLobby(player, race);
            return;
        }
        if (race.getFormat() == RaceFormat.SOLO) {
            if (action == ButtonAction.START) {
                startSolo(player, race);
            } else {
                msg().send(player, "play.solo-uses-start", "race", race.getName());
            }
            return;
        }
        switch (action) {
            case PREP -> prep(player, race);
            case JOIN -> join(player, race);
            case START -> startGroup(player, race);
            default -> {
            }
        }
    }

    /** Checks shared by starting, prepping and joining. Tells the player what's wrong. */
    private boolean canTakePart(Player player, Race race, boolean needsHand) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            msg().send(player, "play.spectator");
            return false;
        }
        if (race.getTravel() == TravelMode.ELYTRA && !wearsElytra(player)) {
            msg().send(player, "play.no-elytra", "race", race.getName());
            return false;
        }
        if (needsHand && needsSteeringItem(race) && !player.getInventory().getItemInMainHand().getType().isAir()) {
            msg().send(player, "play.empty-hand", "race", race.getName(), "item", steeringItemName(race));
            return false;
        }
        return true;
    }

    private static boolean wearsElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    /** Pig and strider races hand out a steering item, which goes into the racer's (empty) main hand. */
    private static boolean needsSteeringItem(Race race) {
        return race.getTravel() == TravelMode.VEHICLE && race.getVehicle() != null
                && race.getVehicle().steeringItem() != null;
    }

    private static String steeringItemName(Race race) {
        return race.getVehicle().steeringItem().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void giveSteeringItem(Player player, Race race) {
        if (needsSteeringItem(race)) {
            player.getInventory().setItemInMainHand(raceItems.create(race.getVehicle().steeringItem()));
        }
    }

    // ---------------------------------------------------------------- solo

    private void startSolo(Player player, Race race) {
        if (racesByPlayer.containsKey(player.getUniqueId())) {
            msg().send(player, "play.already-racing");
            return;
        }
        if (isActive(race)) {
            msg().send(player, "play.course-in-use");
            return;
        }
        if (!race.isReady()) {
            msg().send(player, "play.not-ready", "race", race.getName());
            return;
        }
        if (!canTakePart(player, race, true)) {
            return;
        }

        player.setFlying(false);
        teleport(player, race.getSoloStart());
        // Register after the teleport: the rules then don't see it, and the racer's first tick
        // checks movement from the start spot.
        ActiveRace active = new ActiveRace(race);
        activeRaces.put(race.getName(), active);
        racesByPlayer.put(player.getUniqueId(), active);
        Racer racer = active.add(player);
        racer.setOnCourse(true);

        if (race.getTravel() == TravelMode.VEHICLE) {
            giveSteeringItem(player, race);
            if (!mountVehicle(racer, race, race.getSoloStart())) {
                removeRacer(active, racer);
                msg().send(player, "play.vehicle-failed", "race", race.getName());
                return;
            }
        }
        setGates(race, true);
        msg().send(player, "play.solo-ready", "race", race.getName());
    }

    /**
     * Switches the race's gate levers (if any were linked): on when the countdown starts (solo: at Start),
     * off when the race ends, so redstone gates open for the race and close again afterwards.
     */
    private void setGates(Race race, boolean on) {
        for (Map.Entry<BlockPos, ButtonAction> entry : race.getButtons().entrySet()) {
            if (entry.getValue() != ButtonAction.GATE) {
                continue;
            }
            BlockPos pos = entry.getKey();
            World world = plugin.getServer().getWorld(pos.world());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (block.getBlockData() instanceof Powerable lever && lever.isPowered() != on) {
                lever.setPowered(on);
                block.setBlockData(lever, true);
            }
        }
    }

    /** Mounts a group racer on their grid spot and holds the vehicle there until GO. */
    private boolean mountOnGrid(Racer racer, Race race) {
        if (race.getTravel() != TravelMode.VEHICLE) {
            return true;
        }
        if (!mountVehicle(racer, race, racer.spot())) {
            return false;
        }
        racer.setHold(VehicleHold.hold(racer.vehicle()));
        return true;
    }

    /** Spawns the race's vehicle at {@code at} and puts the racer on it. */
    private boolean mountVehicle(Racer racer, Race race, Location at) {
        Player player = racer.player();
        Entity vehicle = VehicleSpawner.spawn(race.vehicleEntityId(), at, player);
        if (vehicle == null) {
            plugin.getLogger().warning("Could not spawn a " + race.vehicleEntityId() + " for race "
                    + race.getName() + ".");
            return false;
        }
        racer.setVehicle(vehicle);
        vehicleOwners.put(vehicle.getUniqueId(), player.getUniqueId());
        vehicle.addPassenger(player);
        return true;
    }

    // ---------------------------------------------------------------- group: prep, join, start

    /**
     * The presser becomes the leader and players in the same world are invited to join. The leader pressing
     * it again cancels their race; for everyone else it stays locked while the race exists.
     */
    private void prep(Player player, Race race) {
        ActiveRace current = racesByPlayer.get(player.getUniqueId());
        if (current != null && current.race() == race && current.isLeader(player)) {
            cancelRace(current, "play.cancelled-by-leader");
            return;
        }
        if (racesByPlayer.containsKey(player.getUniqueId())) {
            msg().send(player, "play.already-racing");
            return;
        }
        if (isActive(race)) {
            msg().send(player, "play.course-in-use");
            return;
        }
        List<Location> spots = GridSpots.of(race);
        if (!race.isReady() || spots.isEmpty()) {
            msg().send(player, "play.not-ready", "race", race.getName());
            return;
        }
        if (!canTakePart(player, race, false)) {
            return;
        }
        ActiveRace active = new ActiveRace(race, player, spots);
        activeRaces.put(race.getName(), active);
        racesByPlayer.put(player.getUniqueId(), active);
        active.add(player); // not on the course until they press Start

        Component announcement = msg().get("play.prep-announce", "leader", player.getName(),
                "race", race.getName(), "travel", RaceInfo.travelText(race),
                "max", String.valueOf(active.maxPlayers()));
        for (Player other : player.getWorld().getPlayers()) {
            other.sendMessage(msg().get("prefix").append(announcement));
        }
        msg().send(player, "play.prep-leader", "race", race.getName(),
                "min", String.valueOf(race.getSetting(RaceSetting.MIN_PLAYERS)));
    }

    private void join(Player player, Race race) {
        ActiveRace active = activeRaces.get(race.getName());
        if (active == null) {
            msg().send(player, "play.no-race-prepared", "race", race.getName());
            return;
        }
        if (active.state() != State.PREPARING) {
            msg().send(player, "play.course-in-use");
            return;
        }
        ActiveRace current = racesByPlayer.get(player.getUniqueId());
        if (current != null) {
            msg().send(player, current == active ? "play.already-joined" : "play.already-racing",
                    "race", race.getName());
            return;
        }
        if (!canTakePart(player, race, true)) {
            return;
        }
        Location spot = active.nextJoinSpot();
        if (spot == null) {
            msg().send(player, "play.race-full", "race", race.getName(), "max", String.valueOf(active.maxPlayers()));
            return;
        }
        player.setFlying(false);
        teleport(player, spot);
        racesByPlayer.put(player.getUniqueId(), active);
        Racer racer = active.add(player);
        racer.setSpot(spot);
        racer.setOnCourse(true);
        giveSteeringItem(player, race);
        if (!mountOnGrid(racer, race)) {
            msg().send(player, "play.vehicle-failed", "race", race.getName());
            withdraw(active, racer, null, true, true);
            return;
        }

        msg().send(player, "play.joined-self", "race", race.getName());
        notifyRacers(active, player, "play.joined", "player", player.getName(), "race", race.getName(),
                "count", String.valueOf(active.racers().size()), "max", String.valueOf(active.maxPlayers()));
    }

    /** Leader only: takes spot 1 and starts the countdown for everyone. */
    private void startGroup(Player player, Race race) {
        ActiveRace active = activeRaces.get(race.getName());
        if (active == null) {
            msg().send(player, "play.no-race-prepared", "race", race.getName());
            return;
        }
        if (active.state() != State.PREPARING) {
            msg().send(player, "play.course-in-use");
            return;
        }
        if (!active.isLeader(player)) {
            String leader = active.racers().stream().filter(r -> active.isLeader(r.player()))
                    .map(r -> r.player().getName()).findFirst().orElse("?");
            msg().send(player, "play.only-leader", "leader", leader);
            return;
        }
        int min = race.getSetting(RaceSetting.MIN_PLAYERS);
        if (active.racers().size() < min) {
            msg().send(player, "play.not-enough-players", "min", String.valueOf(min),
                    "count", String.valueOf(active.racers().size()));
            return;
        }
        if (!canTakePart(player, race, true)) {
            return;
        }
        Location spot = active.leaderSpot();
        player.setFlying(false);
        teleport(player, spot);
        Racer racer = active.racer(player);
        racer.setSpot(spot);
        racer.setOnCourse(true);
        racer.moved(player.getLocation(), System.nanoTime());
        giveSteeringItem(player, race);
        if (!mountOnGrid(racer, race)) {
            msg().send(player, "play.vehicle-failed", "race", race.getName());
            withdraw(active, racer, null, true, true);
            return;
        }
        startCountdown(active);
    }

    private void startCountdown(ActiveRace active) {
        active.setState(State.COUNTDOWN);
        setGates(active.race(), true); // gates get the whole countdown to open
        int seconds = active.race().getSetting(RaceSetting.COUNTDOWN);
        active.setCountdown(new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (left > 0) {
                    for (Racer racer : active.racers()) {
                        msg().title(racer.player(), "play.countdown-title", null, 1, "seconds", String.valueOf(left));
                        racer.player().playSound(racer.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    left--;
                    return;
                }
                cancel();
                go(active);
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    /** GO: everyone's timer starts at the same moment and held vehicles get their speed back. */
    private void go(ActiveRace active) {
        active.setCountdown(null);
        active.setState(State.RUNNING);
        active.setStartedAt(System.currentTimeMillis());
        Race race = active.race();
        boolean points = race.getFormat() == RaceFormat.POINTS;
        long nanos = System.nanoTime();
        for (Racer racer : List.copyOf(active.racers())) {
            Player player = racer.player();
            if (racer.hold() != null && racer.vehicle() != null) {
                racer.hold().release(racer.vehicle());
                racer.setHold(null);
            }
            racer.startTimer(nanos);
            racer.moved(player.getLocation(), nanos);
            if (points) {
                racer.passStart(nanos); // no start checkpoint: elytra rules (and take-off window) from GO
            }
            msg().title(player, "play.go-title", null, 1);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
        if (points) {
            int limit = race.getSetting(RaceSetting.TIME_LIMIT);
            active.setEndsAt(active.startedAt() + limit * 1000L);
            active.nextZone();
            showGatewayBeam(active);
            notifyRacers(active, null, "play.points-start",
                    "target", String.valueOf(race.getSetting(RaceSetting.POINTS_TO_WIN)),
                    "time", Times.formatSeconds(limit));
        }
    }

    private void notifyRacers(ActiveRace active, Player except, String key, Object... placeholders) {
        for (Racer racer : active.racers()) {
            if (racer.player() != except) {
                msg().send(racer.player(), key, placeholders);
            }
        }
    }

    /**
     * /nltraces lobby: leaves the player's race and sends them to its lobby. The race goes on without them,
     * except when the leader leaves before GO, which cancels it.
     *
     * @return false if the player wasn't in a race
     */
    public boolean leave(Player player) {
        ActiveRace current = racesByPlayer.get(player.getUniqueId());
        if (current == null) {
            return false;
        }
        returnToLobby(player, current.race());
        return true;
    }

    private void returnToLobby(Player player, Race race) {
        ActiveRace current = racesByPlayer.get(player.getUniqueId());
        if (current != null) {
            Racer racer = current.racer(player);
            if (current.state() != State.RUNNING) {
                withdraw(current, racer, "play.left-before-start", false, true);
                teleport(player, current.race().getLobby());
            } else {
                endRun(current, racer, RunStatus.DNF, "left", "play.left-race", true);
            }
            return;
        }
        Location lobby = race.getLobby();
        if (lobby == null) {
            msg().send(player, "play.not-ready", "race", race.getName());
            return;
        }
        teleport(player, lobby);
        msg().send(player, "play.lobby-teleported", "race", race.getName());
    }

    // ---------------------------------------------------------------- per-tick tracking

    private void tick() {
        tick++;
        long nowMillis = System.currentTimeMillis();
        for (ActiveRace active : List.copyOf(activeRaces.values())) {
            Race race = active.race();
            if (active.state() == State.PREPARING
                    && nowMillis - active.createdAt() > race.getSetting(RaceSetting.PREP_TIMEOUT) * 1000L) {
                cancelRace(active, "play.prep-timeout");
                continue;
            }
            if (active.state() != State.RUNNING) {
                waitOnGrid(active);
                continue;
            }
            if (!active.isSolo() && active.dnfDeadline() >= 0 && nowMillis >= active.dnfDeadline()) {
                endMultiplayerRace(active);
                continue;
            }
            boolean points = race.getFormat() == RaceFormat.POINTS;
            if (points) {
                if (nowMillis >= active.endsAt()) {
                    notifyRacers(active, null, "play.points-time-up");
                    endMultiplayerRace(active);
                    continue;
                }
                if (tick % 5 == 0) {
                    drawRing(active.zone());
                }
                if (tick % 100 == 0) {
                    showGatewayBeam(active); // the gateway beam fades after ~10 s, so keep it fresh
                }
            }
            for (Racer racer : List.copyOf(active.racers())) {
                if (!active.racers().contains(racer)) {
                    break; // the race ended while handling an earlier racer
                }
                if (active.isSolo()
                        && nowMillis - active.startedAt() > race.getSetting(RaceSetting.MAX_TIME) * 1000L) {
                    endRun(active, racer, RunStatus.DNF, "max-time", "play.dnf-max-time", true);
                    continue;
                }
                if (racer.player().isFlying()) {
                    disqualify(racer.player(), DqReason.FLYING);
                    continue;
                }
                long nowNanos = System.nanoTime();
                if (!checkTravelRules(race, racer, nowNanos)) {
                    continue; // disqualified
                }
                Location now = racer.player().getLocation();
                if (points ? checkZone(active, racer, now, nowNanos) : checkProgress(active, racer, now, nowNanos)) {
                    continue; // finished, or the points game ended
                }
                racer.moved(now, nowNanos);
                if (tick % 2 == 0) {
                    if (points && racer.offSinceNanos() < 0 && takeoffSecondsLeft(race, racer, nowNanos) < 0) {
                        showPointsBar(active, racer, nowMillis);
                    } else {
                        showActionBar(race, racer, nowNanos);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- points races

    /**
     * Scores a point when the racer enters the active zone: a sphere of the zone's radius around its center.
     * The path moved since the last tick is checked, so fast racers can't fly through it unnoticed.
     * Returns true if this point ended the game.
     */
    private boolean checkZone(ActiveRace active, Racer racer, Location now, long nowNanos) {
        PointZone zone = active.zone();
        if (zone == null || !touchesZone(zone, racer.lastLocation(), now)
                || !canPassCheckpoint(active.race(), racer, nowNanos, true)) {
            return false;
        }
        Race race = active.race();
        Player player = racer.player();
        racer.addPoint(nowNanos);
        String points = String.valueOf(racer.points());
        msg().title(player, "play.points-scored-title", "play.points-scored-subtitle", 1, "points", points);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        notifyRacers(active, player, "play.points-scored", "player", player.getName(), "points", points);

        if (racer.points() >= race.getSetting(RaceSetting.POINTS_TO_WIN)) {
            notifyRacers(active, null, "play.points-won", "player", player.getName(), "points", points);
            endMultiplayerRace(active);
            return true;
        }
        active.nextZone();
        showGatewayBeam(active);
        for (Racer other : active.racers()) {
            other.player().playSound(other.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        }
        return false;
    }

    /**
     * Shows every racer a fake end gateway at the active zone, set to "just created" so it shows its tall
     * beam. Nothing in the world changes. The beam fades after ~10 s, so this is repeated every few seconds;
     * when the zone moves, the old gateway is taken away first.
     */
    private void showGatewayBeam(ActiveRace active) {
        PointZone zone = active.zone();
        // On the block that was clicked when the zone was made (the zone's center sits on top of it).
        Location at = zone == null ? null : zone.center().getBlock().getLocation().subtract(0, 1, 0);
        Location previous = active.beamBlock();
        if (previous != null && !previous.equals(at)) {
            for (Racer racer : active.racers()) {
                hideBlock(racer.player(), previous);
            }
        }
        active.setBeamBlock(at);
        if (at == null) {
            return;
        }
        BlockData gateway = Material.END_GATEWAY.createBlockData();
        EndGateway state = (EndGateway) gateway.createBlockState();
        state.setAge(0);
        for (Racer racer : active.racers()) {
            Player player = racer.player();
            if (player.getWorld().equals(at.getWorld())) {
                player.sendBlockChange(at, gateway);
                player.sendBlockUpdate(at, state);
            }
        }
    }

    /** Shows the player the real block again where a fake one was shown. */
    private static void hideBlock(Player player, Location block) {
        if (player.isOnline() && player.getWorld().equals(block.getWorld())) {
            player.sendBlockChange(block, block.getBlock().getBlockData());
        }
    }

    /** Whether the path from {@code from} to {@code to} comes within the zone's radius of its center. */
    private static boolean touchesZone(PointZone zone, Location from, Location to) {
        Location center = zone.center();
        if (to.getWorld() == null || !to.getWorld().equals(center.getWorld())
                || !to.getWorld().equals(from.getWorld())) {
            return false;
        }
        double radiusSquared = zone.radius() * zone.radius();
        int steps = Math.max(1, (int) Math.ceil(from.distance(to) / 0.5));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double dx = from.getX() + (to.getX() - from.getX()) * t - center.getX();
            double dy = from.getY() + (to.getY() - from.getY()) * t - center.getY();
            double dz = from.getZ() + (to.getZ() - from.getZ()) * t - center.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    /** A ring of particles showing the zone's radius (the beam itself is a fake end gateway). */
    private void drawRing(PointZone zone) {
        if (zone == null || zone.center().getWorld() == null) {
            return;
        }
        Location center = zone.center();
        World world = center.getWorld();
        Particle particle = ringParticle();
        int points = Math.max(8, (int) (zone.radius() * 6));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            world.spawnParticle(particle, center.getX() + Math.cos(angle) * zone.radius(), center.getY() + 0.2,
                    center.getZ() + Math.sin(angle) * zone.radius(), 1, 0, 0, 0, 0);
        }
    }

    private Particle ringParticle() {
        String name = String.valueOf(plugin.getConfig().getString("points-races.ring-particle"));
        try {
            Particle particle = Particle.valueOf(name.toUpperCase(Locale.ROOT));
            if (particle.getDataType() == Void.class) {
                return particle;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to the default
        }
        return Particle.END_ROD;
    }

    private void showPointsBar(ActiveRace active, Racer racer, long nowMillis) {
        PointZone zone = active.zone();
        Location at = racer.player().getLocation();
        String distance = zone == null || !at.getWorld().equals(zone.center().getWorld()) ? "?"
                : String.valueOf((int) Math.round(Math.hypot(at.getX() - zone.center().getX(),
                at.getZ() - zone.center().getZ())));
        long left = Math.max(0, (active.endsAt() - nowMillis) / 1000);
        msg().actionBar(racer.player(), msg().get("play.actionbar-points", "points", String.valueOf(racer.points()),
                "distance", distance, "time", Times.formatSeconds(left)));
    }

    /**
     * Before GO, vehicle racers are held on their spot, and the getting-off rules already apply
     * (strict: DQ, lenient: remount countdown). A DQ before GO takes them out of the race.
     */
    private void waitOnGrid(ActiveRace active) {
        Race race = active.race();
        if (race.getTravel() != TravelMode.VEHICLE) {
            return; // foot and elytra racers are frozen by the move listener instead
        }
        long nowNanos = System.nanoTime();
        for (Racer racer : List.copyOf(active.racers())) {
            if (!racer.onCourse() || racer.vehicle() == null) {
                continue;
            }
            internalTeleports.add(racer.player().getUniqueId());
            try {
                VehicleHold.keepOnSpot(racer.vehicle(), racer.spot());
            } finally {
                internalTeleports.remove(racer.player().getUniqueId());
            }
            if (checkTravelRules(race, racer, nowNanos) && tick % 2 == 0 && racer.offSinceNanos() >= 0) {
                showActionBar(race, racer, nowNanos);
            }
        }
    }

    /**
     * Vehicle and elytra rules. Strict: getting off / landing is an instant DQ. Lenient: the racer gets
     * remount-time seconds to get back on / take off again. Elytra rules start at the start checkpoint.
     *
     * @return false if the racer was disqualified
     */
    private boolean checkTravelRules(Race race, Racer racer, long nowNanos) {
        Player player = racer.player();
        boolean ok;
        DqReason strictReason;
        DqReason timeoutReason;
        switch (race.getTravel()) {
            case VEHICLE -> {
                Entity vehicle = racer.vehicle();
                if (vehicle == null || !vehicle.isValid()) {
                    disqualify(player, DqReason.VEHICLE_LOST);
                    return false;
                }
                ok = vehicle.getPassengers().contains(player);
                strictReason = DqReason.DISMOUNT;
                timeoutReason = DqReason.REMOUNT_TIMEOUT;
            }
            case ELYTRA -> {
                if (!racer.startPassed()) {
                    return true; // standing on the launch platform is fine
                }
                if (!racer.hasTakenOff()) {
                    if (player.isGliding()) {
                        racer.setTakenOff();
                    } else if (nowNanos - racer.startPassedNanos() <= takeoffWindowNanos(race)) {
                        return true; // still inside the take-off window: the ground is allowed
                    } else {
                        disqualify(player, DqReason.NO_TAKEOFF);
                        return false;
                    }
                }
                ok = player.isGliding();
                strictReason = DqReason.LANDED;
                timeoutReason = DqReason.TAKEOFF_TIMEOUT;
            }
            default -> {
                return true;
            }
        }
        if (ok) {
            racer.setOffSinceNanos(-1);
            return true;
        }
        if (race.getRules() == Rules.STRICT) {
            disqualify(player, strictReason);
            return false;
        }
        if (racer.offSinceNanos() < 0) {
            racer.setOffSinceNanos(nowNanos);
        }
        if (nowNanos - racer.offSinceNanos() > race.getSetting(RaceSetting.REMOUNT_TIME) * 1_000_000_000L) {
            disqualify(player, timeoutReason);
            return false;
        }
        return true;
    }

    /**
     * Whether a checkpoint (or point zone) counts right now: elytra racers must glide, vehicle racers must ride.
     *
     * @param zone true for a point zone, so the warning names it correctly
     */
    private boolean canPassCheckpoint(Race race, Racer racer, long nowNanos, boolean zone) {
        boolean allowed = switch (race.getTravel()) {
            case ELYTRA -> racer.player().isGliding();
            case VEHICLE -> racer.vehicle() != null && racer.vehicle().getPassengers().contains(racer.player());
            default -> true;
        };
        if (!allowed && nowNanos - racer.lastCheckpointWarnNanos() > CHECKPOINT_WARN_INTERVAL_NANOS) {
            racer.setLastCheckpointWarnNanos(nowNanos);
            String what = zone ? "zone" : "checkpoint";
            msg().send(racer.player(), race.getTravel() == TravelMode.ELYTRA
                    ? "play." + what + "-needs-gliding" : "play." + what + "-needs-vehicle",
                    "vehicle", vehicleName(race));
        }
        return allowed;
    }

    /**
     * Detects checkpoint crossings since the last tick. The start checkpoint comes first (solo: it starts
     * the timer; group: the timer already runs from GO). Returns true if the racer finished.
     */
    private boolean checkProgress(ActiveRace active, Racer racer, Location now, long nowNanos) {
        Race race = active.race();
        Player player = racer.player();
        Location from = racer.lastLocation();

        if (!racer.startPassed()) {
            double t = race.getStartCheckpoint().firstEntry(from, now);
            if (t < 0 || !canPassCheckpoint(race, racer, nowNanos, false)) {
                return false;
            }
            racer.passStart(nowNanos);
            if (active.isSolo()) {
                racer.startTimer(crossingNanos(racer, t, nowNanos));
                msg().title(player, "play.go-title", null, 1);
                player.playSound(now, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            }
            return false;
        }

        List<Region> checkpoints = race.getCheckpoints();
        while (racer.nextCheckpoint() < checkpoints.size()) {
            double t = checkpoints.get(racer.nextCheckpoint()).firstEntry(from, now);
            if (t < 0 || !canPassCheckpoint(race, racer, nowNanos, false)) {
                break;
            }
            long splitMs = (crossingNanos(racer, t, nowNanos) - racer.startNanos()) / 1_000_000L;
            racer.passCheckpoint(splitMs);
            player.playSound(now, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            msg().send(player, "play.checkpoint", "number", String.valueOf(racer.nextCheckpoint()),
                    "total", String.valueOf(checkpoints.size()), "time", Times.format(splitMs));
        }

        Region finish = race.getFinishCheckpoint();
        if (racer.nextCheckpoint() < checkpoints.size()) {
            boolean inFinish = finish.contains(now);
            if (inFinish && !racer.warnedAtFinish()) {
                msg().send(player, "play.missed-checkpoint", "number", String.valueOf(racer.nextCheckpoint() + 1));
            }
            racer.setWarnedAtFinish(inFinish);
            return false;
        }
        double t = finish.firstEntry(from, now);
        if (t < 0 || !canPassCheckpoint(race, racer, nowNanos, false)) {
            return false;
        }
        finish(active, racer, (crossingNanos(racer, t, nowNanos) - racer.startNanos()) / 1_000_000L);
        return true;
    }

    /** Interpolates when, between the last tick and now, the racer crossed a line. */
    private static long crossingNanos(Racer racer, double fraction, long nowNanos) {
        return racer.lastNanos() + Math.round(fraction * (nowNanos - racer.lastNanos()));
    }

    /** The timer, or (lenient rules) a countdown while the racer is off their vehicle or not gliding. */
    private static long takeoffWindowNanos(Race race) {
        return race.getSetting(RaceSetting.TAKEOFF_TIME) * 1_000_000_000L;
    }

    /** Elytra racers who haven't taken off yet: seconds left in the take-off window, or -1. */
    private static double takeoffSecondsLeft(Race race, Racer racer, long nowNanos) {
        if (race.getTravel() != TravelMode.ELYTRA || !racer.startPassed() || racer.hasTakenOff()) {
            return -1;
        }
        return Math.max(0, (takeoffWindowNanos(race) - (nowNanos - racer.startPassedNanos())) / 1e9);
    }

    private void showActionBar(Race race, Racer racer, long nowNanos) {
        double takeoffLeft = takeoffSecondsLeft(race, racer, nowNanos);
        if (takeoffLeft >= 0) {
            msg().actionBar(racer.player(), msg().get("play.actionbar-takeoff-window",
                    "seconds", String.format("%.1f", takeoffLeft)));
            return;
        }
        if (racer.offSinceNanos() >= 0) {
            double left = race.getSetting(RaceSetting.REMOUNT_TIME) - (nowNanos - racer.offSinceNanos()) / 1e9;
            msg().actionBar(racer.player(), msg().get(race.getTravel() == TravelMode.ELYTRA
                            ? "play.actionbar-takeoff" : "play.actionbar-remount",
                    "seconds", String.format("%.1f", Math.max(0, left)), "vehicle", vehicleName(race)));
            return;
        }
        if (!racer.isTiming()) {
            msg().actionBar(racer.player(), msg().get("play.actionbar-waiting"));
            return;
        }
        int total = race.getCheckpoints().size();
        Component checkpoints = total == 0 ? Component.empty() : msg().get("play.actionbar-checkpoints",
                "passed", String.valueOf(racer.nextCheckpoint()), "total", String.valueOf(total));
        msg().actionBar(racer.player(), msg().get("play.actionbar-timer",
                "time", Times.format(racer.elapsedMillis(nowNanos)), "checkpoints", checkpoints));
    }

    private static String vehicleName(Race race) {
        return race.getVehicle() == null ? "vehicle" : Enums.display(race.getVehicle()).replace('-', ' ');
    }

    // ---------------------------------------------------------------- finishing and ending runs

    private void finish(ActiveRace active, Racer racer, long timeMs) {
        Player player = racer.player();
        Race race = active.race();
        String time = Times.format(timeMs);
        removeRacer(active, racer);
        teleport(player, race.getLobby());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        if (active.isSolo()) {
            msg().title(player, "play.finished-title", "play.finished-subtitle", 3, "time", time);
            msg().send(player, "play.finished", "race", race.getName(), "time", time);
            saveSolo(active, racer, RunStatus.FINISHED, null, timeMs);
            return;
        }

        int placing = active.nextPlacing();
        String place = ordinal(placing);
        msg().title(player, "play.finished-title", "play.finished-place-subtitle", 3, "place", place, "time", time);
        msg().send(player, "play.finished-place", "race", race.getName(), "place", place, "time", time);
        active.addResult(run(racer, RunStatus.FINISHED, null, timeMs, placing, null));

        if (placing == 1 && !active.isEmpty()) {
            int seconds = race.getSetting(RaceSetting.DNF_AFTER_FIRST_FINISH);
            active.setDnfDeadline(System.currentTimeMillis() + seconds * 1000L);
            notifyRacers(active, null, "play.first-finished", "player", player.getName(),
                    "seconds", String.valueOf(seconds));
        }
        if (active.isEmpty()) {
            endMultiplayerRace(active);
        }
    }

    /**
     * Ends a run without a finish time.
     *
     * @param messageKey message shown to the racer, or null for none (e.g. they logged out)
     * @param toLobby    teleport the racer to the lobby now (false when they're dead)
     */
    private void endRun(ActiveRace active, Racer racer, RunStatus status, String reason, String messageKey,
                        boolean toLobby) {
        Player player = racer.player();
        Race race = active.race();
        removeRacer(active, racer);
        if (toLobby) {
            teleport(player, race.getLobby());
        }
        msg().actionBar(player, Component.empty());
        if (messageKey != null) {
            msg().send(player, messageKey, "race", race.getName(),
                    "max", Times.formatSeconds(race.getSetting(RaceSetting.MAX_TIME)));
        }
        if (active.isSolo()) {
            saveSolo(active, racer, status, reason, null);
        } else {
            // Points racers who leave or are DQ'd keep the points they scored, but get no place.
            Integer points = race.getFormat() == RaceFormat.POINTS ? racer.points() : null;
            active.addResult(run(racer, status, reason, null, null, points));
            if (active.isEmpty()) {
                endMultiplayerRace(active);
            }
        }
    }

    /** Disqualifies the player if they're racing: message, run recorded as DQ, back to the lobby. */
    public void disqualify(Player player, DqReason reason) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        if (active == null) {
            return;
        }
        Racer racer = active.racer(player);
        Race race = active.race();
        boolean running = active.state() == State.RUNNING;
        // A leader who hasn't pressed Start isn't on the course yet: only logging out counts for them.
        if (!running && !racer.onCourse() && reason != DqReason.LOGOUT) {
            return;
        }
        boolean dead = reason == DqReason.DEATH;
        if (dead) {
            pendingRespawns.put(player.getUniqueId(), race.getLobby());
        }
        Component reasonText = msg().get("play.dq." + reason.key(), "vehicle", vehicleName(race));
        if (reason != DqReason.LOGOUT) {
            msg().title(player, "play.dq-title", "play.dq-subtitle", 3, "reason", reasonText);
            msg().send(player, "play.dq-chat", "race", race.getName(), "reason", reasonText);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        if (running) {
            endRun(active, racer, RunStatus.DQ, reason.key(), null, !dead);
        } else {
            // Before GO there's nothing to record: the player is just taken out of the race.
            withdraw(active, racer, null, !dead, false);
        }
    }

    /**
     * Leaves a group race before GO. The race is cancelled if the leader leaves while it's still being
     * prepared, or leaves on purpose (lobby) before GO; a leader DQ'd during the countdown doesn't stop it.
     *
     * @param toLobby   teleport the player to the lobby if they were on the course (false when they're dead)
     * @param voluntary the player chose to leave (lobby button or command)
     */
    private void withdraw(ActiveRace active, Racer racer, String messageKey, boolean toLobby, boolean voluntary) {
        Player player = racer.player();
        boolean wasOnCourse = racer.onCourse();
        removeRacer(active, racer);
        if (wasOnCourse && toLobby) {
            teleport(player, active.race().getLobby());
        }
        if (messageKey != null) {
            msg().send(player, messageKey, "race", active.race().getName());
        }
        if (active.isLeader(player) && (voluntary || active.state() == State.PREPARING)) {
            cancelRace(active, "play.leader-left");
            return;
        }
        notifyRacers(active, null, "play.player-left", "player", player.getName(), "race", active.race().getName());
        if (active.isEmpty()) {
            cancelRace(active, "play.cancelled");
        }
    }

    /**
     * Ends a group or points race, then podiums the top 3, announces the results and saves them.
     * Group: everyone still racing is DNF (all finished, or the DNF timer ran out).
     * Points: everyone still in is ranked by points; ties go to whoever reached that score first.
     */
    private void endMultiplayerRace(ActiveRace active) {
        Race race = active.race();
        activeRaces.remove(race.getName(), active);
        setGates(race, false);
        boolean points = race.getFormat() == RaceFormat.POINTS;
        List<Racer> remaining = new ArrayList<>(active.racers());
        if (points) {
            remaining.sort(Comparator.comparingInt(Racer::points).reversed()
                    .thenComparingLong(Racer::lastScoreNanos));
        }
        int place = 0;
        for (Racer racer : remaining) {
            Player player = racer.player();
            removeRacer(active, racer);
            teleport(player, race.getLobby());
            msg().actionBar(player, Component.empty());
            if (points) {
                place++;
                // time_ms: when they reached their final score (shown on leaderboards as a tie-breaker)
                Long scoredAt = racer.points() == 0 ? null : (racer.lastScoreNanos() - racer.startNanos()) / 1_000_000L;
                active.addResult(run(racer, RunStatus.FINISHED, null, scoredAt, place, racer.points()));
                msg().send(player, "play.points-final", "race", race.getName(), "place", ordinal(place),
                        "points", String.valueOf(racer.points()));
            } else {
                msg().send(player, "play.dnf-time-up", "race", race.getName());
                active.addResult(run(racer, RunStatus.DNF, "time-up", null, null, null));
            }
        }

        List<RaceRecord.Run> results = active.results();
        results.sort(Comparator.comparing((RaceRecord.Run run) -> run.placing() == null ? Integer.MAX_VALUE
                : run.placing()).thenComparing(run -> run.status().ordinal()));
        for (RaceRecord.Run run : results) {
            Player player = plugin.getServer().getPlayer(run.playerId());
            if (run.placing() != null && run.placing() <= 3 && player != null) {
                teleport(player, race.getPodium(run.placing()));
                msg().title(player, "play.podium-title", null, 4, "place", ordinal(run.placing()));
            }
        }
        announceResults(active, results);

        RaceRecord record = new RaceRecord(race.getName(), race.getFormat(), race.getTravel(), race.getVehicle(),
                active.startedAt(), System.currentTimeMillis(), List.copyOf(results));
        runStore.save(record).whenComplete((id, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not save race " + race.getName(), error);
            } else {
                plugin.sync(() -> plugin.getLeaderboards().refresh(race));
            }
        });
    }

    private void announceResults(ActiveRace active, List<RaceRecord.Run> results) {
        Component header = msg().get("play.results-header", "race", active.race().getName());
        List<Component> lines = results.stream().map(run -> {
            if (run.status() != RunStatus.FINISHED) {
                return msg().get("play.results-other", "player", run.playerName(), "status", run.status().name());
            }
            if (run.points() != null) {
                return msg().get("play.results-points", "place", String.valueOf(run.placing()),
                        "player", run.playerName(), "points", String.valueOf(run.points()));
            }
            return msg().get("play.results-finished", "place", String.valueOf(run.placing()),
                    "player", run.playerName(), "time", Times.format(run.timeMs()));
        }).toList();
        for (UUID id : active.participants()) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                player.sendMessage(header);
                lines.forEach(player::sendMessage);
            }
        }
    }

    /** Ends a race without recording anything and sends everyone on the course to the lobby. */
    private void cancelRace(ActiveRace active, String messageKey) {
        if (active.countdown() != null) {
            active.countdown().cancel();
            active.setCountdown(null);
        }
        activeRaces.remove(active.race().getName(), active);
        setGates(active.race(), false);
        for (Racer racer : List.copyOf(active.racers())) {
            Player player = racer.player();
            boolean onCourse = racer.onCourse();
            removeRacer(active, racer);
            if (onCourse) {
                teleport(player, active.race().getLobby());
            }
            msg().actionBar(player, Component.empty());
            msg().send(player, messageKey, "race", active.race().getName());
        }
    }

    /**
     * A racer died: their steering item mustn't drop. Call before {@link #disqualify}, while the
     * drops can still be changed.
     */
    void handleDeathDrops(List<ItemStack> drops) {
        drops.removeIf(raceItems::isRaceItem);
    }

    /** Takes the racer out of the race and removes everything the race gave them (vehicle, items). */
    private void removeRacer(ActiveRace active, Racer racer) {
        active.remove(racer);
        Player player = racer.player();
        racesByPlayer.remove(player.getUniqueId());
        if (active.beamBlock() != null) {
            hideBlock(player, active.beamBlock()); // the fake end gateway of a points race
        }
        if (active.isEmpty() && active.isSolo() && activeRaces.remove(active.race().getName(), active)) {
            setGates(active.race(), false);
        }

        Entity vehicle = racer.vehicle();
        if (vehicle != null) {
            vehicleOwners.remove(vehicle.getUniqueId());
            vehicle.eject();
            vehicle.remove();
            racer.setVehicle(null);
        }
        raceItems.removeAll(player);
    }

    private static RaceRecord.Run run(Racer racer, RunStatus status, String reason, Long timeMs, Integer placing,
                                      Integer points) {
        Player player = racer.player();
        return new RaceRecord.Run(player.getUniqueId(), player.getName(), status, reason, timeMs, points, placing,
                System.currentTimeMillis(), racer.splits());
    }

    private void saveSolo(ActiveRace active, Racer racer, RunStatus status, String reason, Long timeMs) {
        Race race = active.race();
        Player player = racer.player();
        RaceRecord record = new RaceRecord(race.getName(), race.getFormat(), race.getTravel(), race.getVehicle(),
                active.startedAt(), System.currentTimeMillis(), List.of(run(racer, status, reason, timeMs, null, null)));
        runStore.save(record).whenComplete((id, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not save a run on " + race.getName(), error);
                plugin.sync(() -> {
                    if (player.isOnline()) {
                        msg().send(player, "play.save-failed");
                    }
                });
            } else {
                plugin.sync(() -> plugin.getLeaderboards().refresh(race));
            }
        });
    }

    /** 1 → "1st", 2 → "2nd", 11 → "11th", 22 → "22nd". */
    static String ordinal(int number) {
        int lastTwo = number % 100;
        String suffix = lastTwo >= 11 && lastTwo <= 13 ? "th" : switch (number % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return number + suffix;
    }

    // ---------------------------------------------------------------- player leaving / cancel / shutdown

    /** A racer logged out: disqualify them (before GO: they just leave) so the course unlocks. */
    public void handleQuit(Player player) {
        disqualify(player, DqReason.LOGOUT);
        pendingRespawns.remove(player.getUniqueId());
    }

    /**
     * /nltraces cancel &lt;race&gt;: ends a running (or preparing) race and sends everyone to the lobby
     * without recording anything.
     *
     * @return how many racers were sent back, or -1 if the race wasn't active
     */
    public int cancel(Race race) {
        ActiveRace active = activeRaces.get(race.getName());
        if (active == null) {
            return -1;
        }
        int count = active.racers().size();
        cancelRace(active, "play.cancelled");
        return count;
    }

    /** The race this player is leading (group/points), or null. */
    public Race leadingRace(Player player) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        return active != null && active.isLeader(player) ? active.race() : null;
    }

    /**
     * The leader cancels their own race (/nltraces cancel, or pressing prep again): everyone goes to the lobby
     * and nothing is recorded.
     *
     * @return false if the player isn't leading a race
     */
    public boolean cancelByLeader(Player player) {
        ActiveRace active = racesByPlayer.get(player.getUniqueId());
        if (active == null || !active.isLeader(player)) {
            return false;
        }
        cancelRace(active, "play.cancelled-by-leader");
        return true;
    }

    /** Server stopping or plugin reloading: send racers to the lobby without recording anything. */
    public void shutdown() {
        ticker.cancel();
        for (ActiveRace active : List.copyOf(activeRaces.values())) {
            cancelRace(active, "play.stopped");
        }
    }
}
