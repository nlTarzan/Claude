package com.nltarzan.structures.monument;

import com.nltarzan.structures.monument.MonumentResetter.Result;
import com.nltarzan.structures.util.Durations;
import com.nltarzan.structures.util.Weighted;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * The randomized check loop from schedule.mcfunction. Unlike the datapack, the next check time is
 * saved (in world game ticks), so a server restart doesn't restart the 1-3 day wait from zero.
 *
 * <p>On a new server nothing runs until /nltstructures monument active on, so a monument is never built
 * at a location nobody checked. Whether it's on is saved in state.yml as well.
 */
public final class ResetScheduler {

    /** Retry delay after a failed check, e.g. when the area's entities didn't load in time. */
    private static final long RETRY_TICKS = 60 * Durations.TICKS_PER_SECOND;
    /** Delay for a check that became due while the server was offline. */
    private static final long OVERDUE_TICKS = 10 * Durations.TICKS_PER_SECOND;

    private final Monuments monuments;
    private final File stateFile;
    private final Random random = new Random();

    private BukkitTask task;
    /** Set with /nltstructures monument active on|off. While off there are no checks or resets. */
    private boolean active;
    private String stateWorld;
    private long nextCheckAt = -1;

    public ResetScheduler(Monuments monuments) {
        this.monuments = monuments;
        this.stateFile = new File(monuments.folder(), "state.yml");
        loadState();
    }

    /** (Re)starts the timer, continuing the saved countdown when it belongs to the current world. */
    public void start() {
        stop();
        if (!active) {
            return;
        }
        World world = world();
        if (world == null) {
            monuments.getLogger().warning("World '" + monuments.settings().worldName()
                    + "' is not loaded, the monument won't reset until it is (then run /nltstructures monument reload).");
            return;
        }

        long now = world.getGameTime();
        if (!world.getName().equals(stateWorld) || nextCheckAt < 0) {
            schedule(pick(monuments.settings().intervals()));
        } else if (nextCheckAt > now) {
            schedule(nextCheckAt - now);
        } else {
            schedule(OVERDUE_TICKS);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Turns the checks on or off (saved in state.yml, so it lasts through restarts). Turning off
     * forgets the countdown, so turning on again starts a fresh random wait.
     */
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            saveState();
            start();
        } else {
            stop();
            nextCheckAt = -1;
            saveState();
        }
    }

    /** Runs a check right away (from a command) and then starts a fresh random interval. */
    public CompletableFuture<Result> runNow(boolean force, String variant, Long seed) {
        if (monuments.resetter().isRunning()) {
            return CompletableFuture.completedFuture(new Result.Failed("A check is already running."));
        }
        stop();
        return runCycle(force, variant, seed);
    }

    /** Ticks until the next check, or -1 when nothing is scheduled. */
    public long ticksUntilNextCheck() {
        World world = world();
        if (task == null || world == null) {
            return -1;
        }
        return Math.max(0, nextCheckAt - world.getGameTime());
    }

    private CompletableFuture<Result> runCycle(boolean force, String variant, Long seed) {
        return monuments.resetter().run(force, variant, seed).whenComplete((result, error) -> {
            if (!monuments.isEnabled() || !monuments.isActive()) {
                return;
            }
            if (result instanceof Result.Failed(String reason)) {
                monuments.getLogger().warning("Monument check failed: " + reason + " Retrying in "
                        + Durations.format(RETRY_TICKS) + ".");
                schedule(RETRY_TICKS);
            } else if (result instanceof Result.PlayersInside(int players)) {
                monuments.getLogger().info("Monument is cleared but " + players
                        + " player(s) are inside, postponing the reset.");
                schedule(pick(monuments.settings().occupiedIntervals()));
            } else {
                schedule(pick(monuments.settings().intervals()));
            }
            // After scheduling, so the status shows the new next check too.
            monuments.recordCheck(result);
        });
    }

    private void schedule(long delayTicks) {
        stop();
        World world = world();
        if (world == null) {
            return;
        }
        nextCheckAt = world.getGameTime() + delayTicks;
        stateWorld = world.getName();
        saveState();
        task = monuments.getServer().getScheduler().runTaskLater(monuments.plugin(), () -> {
            task = null;
            runCycle(false, null, null);
        }, delayTicks);
        monuments.getLogger().info("Next monument check in " + Durations.format(delayTicks)
                + " (" + delayTicks + " ticks).");
    }

    private long pick(List<Weighted<Long>> intervals) {
        return Weighted.pick(intervals, random).orElse(Durations.TICKS_PER_DAY);
    }

    private World world() {
        return monuments.getServer().getWorld(monuments.settings().worldName());
    }

    private void loadState() {
        if (!stateFile.exists()) {
            return;
        }
        YamlConfiguration state = YamlConfiguration.loadConfiguration(stateFile);
        stateWorld = state.getString("world");
        nextCheckAt = state.getLong("next-check-at", -1);
        // A state.yml from before "active" existed already had a running countdown: keep it running.
        active = state.getBoolean("active", nextCheckAt >= 0);
    }

    private void saveState() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("active", active);
        state.set("world", stateWorld);
        state.set("next-check-at", nextCheckAt);
        try {
            state.save(stateFile);
        } catch (IOException e) {
            monuments.getLogger().log(Level.WARNING, "Could not save " + stateFile.getName(), e);
        }
    }
}
