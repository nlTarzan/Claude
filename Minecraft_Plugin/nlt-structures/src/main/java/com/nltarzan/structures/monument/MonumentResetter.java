package com.nltarzan.structures.monument;

import com.nltarzan.structures.NltStructuresPlugin;
import com.nltarzan.structures.monument.build.BuildPlanner;
import com.nltarzan.structures.monument.config.MonumentConfig;
import com.nltarzan.structures.monument.config.MonumentConfig.ResetMode;
import com.nltarzan.structures.monument.layout.MonumentLayout;
import com.nltarzan.structures.monument.CurrentMonument.Built;
import com.nltarzan.structures.monument.StructureLibrary.MonumentVariant;
import com.nltarzan.structures.util.Weighted;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.structure.Structure;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * One check of the monument: load the area, count Elder Guardians and, when they are all dead,
 * place a new monument: a random pre-built variant or a generated maze, depending on reset.mode.
 * Replaces monument_initial, monument_checks and monument_place.
 */
public final class MonumentResetter {

    /** Use as the variant to build a generated monument, whatever reset.mode says. */
    public static final String GENERATED = "generated";

    /** Width of one quadrant structure; the other three are placed this far to the west/north. */
    private static final int QUADRANT_SIZE = 29;

    public sealed interface Result {
        record NotCleared(int eldersAlive) implements Result {
        }

        /** Cleared, but the reset was postponed because players are inside the area. */
        record PlayersInside(int players) implements Result {
        }

        /** @param description what was placed, e.g. "monument_03" or "generated (seed 123)" */
        record Placed(String description) implements Result {
        }

        record Failed(String reason) implements Result {
        }
    }

    private final Monuments monuments;
    private final MonumentBuilder builder;
    private final Random random = new Random();
    private boolean running;

    public MonumentResetter(Monuments monuments) {
        this.monuments = monuments;
        this.builder = new MonumentBuilder(monuments);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * @param force   place a new monument even if Elder Guardians are alive or players are inside
     * @param variant a pre-built variant, {@link #GENERATED}, or {@code null} to follow reset.mode
     * @param seed    the layout seed for a generated monument, or {@code null} for a random one
     */
    public CompletableFuture<Result> run(boolean force, String variant, Long seed) {
        if (running) {
            return CompletableFuture.completedFuture(new Result.Failed("A check is already running."));
        }
        if (monuments.isPaused()) {
            return CompletableFuture.completedFuture(new Result.Failed("Paused: monuments.yml "
                    + monuments.configProblem() + ". Fix it and run /nltstructures monument reload."));
        }
        if (!monuments.isActive()) {
            return CompletableFuture.completedFuture(new Result.Failed("The monument is inactive. Check the location,"
                    + " then run /nltstructures monument active on."));
        }
        MonumentArena arena = monuments.arena();
        if (arena == null) {
            return CompletableFuture.completedFuture(new Result.Failed(
                    "World '" + monuments.settings().worldName() + "' is not loaded."));
        }

        running = true;
        return arena.loadChunks()
                .thenCompose(ignored -> evaluate(arena, force, variant, seed))
                .exceptionally(e -> {
                    Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
                    monuments.getLogger().log(Level.WARNING, "Monument check failed", cause);
                    return new Result.Failed(String.valueOf(cause.getMessage()));
                })
                .whenComplete((result, error) -> {
                    arena.releaseChunks();
                    running = false;
                });
    }

    private CompletableFuture<Result> evaluate(MonumentArena arena, boolean force, String requested, Long seed) {
        MonumentConfig settings = monuments.settings();

        int alive = arena.entities(ElderGuardian.class).size();
        if (alive > 0 && !force) {
            announce("not-cleared");
            return done(new Result.NotCleared(alive));
        }
        if (!force) {
            int players = arena.entities(Player.class).size();
            if (players > 0) {
                announce("cleared-occupied");
                return done(new Result.PlayersInside(players));
            }
            announce("cleared");
        }

        boolean generated = requested == null ? settings.mode() == ResetMode.GENERATED : requested.equals(GENERATED);
        return generated ? placeGenerated(arena, seed) : placePreBuilt(arena, requested);
    }

    private CompletableFuture<Result> placePreBuilt(MonumentArena arena, String requested) {
        MonumentConfig settings = monuments.settings();
        Optional<MonumentVariant> variant = requested != null
                ? monuments.structures().get(requested)
                : Weighted.pick(monuments.structures().weighted(), random);
        if (variant.isEmpty()) {
            return done(new Result.Failed(requested != null
                    ? "Unknown variant '" + requested + "'."
                    : "No monument variants with a weight above 0 are loaded."));
        }

        clearGuardians(arena);
        // One block below the center, like the generated monument's empty shell (maze/reset.mcfunction
        // places it at ~-1). The datapack placed these at ~, which put them one block too high.
        Location center = arena.center().add(0, -1, 0);
        placeQuadrant(variant.get().se(), center);
        placeQuadrant(variant.get().sw(), center.clone().add(-QUADRANT_SIZE, 0, 0));
        placeQuadrant(variant.get().ne(), center.clone().add(0, 0, -QUADRANT_SIZE));
        placeQuadrant(variant.get().nw(), center.clone().add(-QUADRANT_SIZE, 0, -QUADRANT_SIZE));
        // The quadrant structures hold the two wing Elder Guardians; the third one in the top room is
        // summoned separately, because saving it in a quadrant gave monuments with 2, 3 or 4 of them.
        spawnElderGuardian(arena.center().add(0, settings.elderYOffset(), 0));

        monuments.currentMonument().set(new Built.PreBuilt(variant.get().name()), arena.countSponges());
        monuments.getLogger().info("Placed monument variant " + variant.get().name() + ".");
        announce("reset");
        return done(new Result.Placed(variant.get().name()));
    }

    private CompletableFuture<Result> placeGenerated(MonumentArena arena, Long requestedSeed) {
        if (!monuments.structures().mazeReady()) {
            return done(new Result.Failed("The templates for generated monuments didn't load, see the console."));
        }
        long seed = requestedSeed != null ? requestedSeed : ThreadLocalRandom.current().nextLong();
        var mazeSettings = monuments.settings().maze();
        MonumentLayout layout = MonumentLayout.generate(seed, mazeSettings);

        clearGuardians(arena);
        return builder.build(arena, BuildPlanner.plan(layout), seed, this::spawnElderGuardian).thenApply(ignored -> {
            monuments.currentMonument().set(new Built.Generated(seed, mazeSettings), arena.countSponges());
            String description = "generated monument (seed " + seed + ", "
                    + Math.round(layout.newestChance() * 100) + "% newest)";
            monuments.getLogger().info("Built " + description + ".");
            announce("reset");
            return new Result.Placed(description);
        });
    }

    private void clearGuardians(MonumentArena arena) {
        if (monuments.settings().clearGuardians()) {
            // ElderGuardian extends Guardian, so this also clears any Elder Guardians left by a forced reset.
            arena.entities(Guardian.class).forEach(Entity::remove);
        }
    }

    private void placeQuadrant(Structure structure, Location origin) {
        // Same as /place template: entities included, no rotation, random palette, full integrity.
        structure.place(origin, true, StructureRotation.NONE, Mirror.NONE, -1, 1f, random);
    }

    /** Spawns an Elder Guardian with the configured health and attack damage. */
    private void spawnElderGuardian(Location location) {
        MonumentConfig settings = monuments.settings();
        location.getWorld().spawn(location, ElderGuardian.class, guardian -> {
            setBase(guardian, Attribute.MAX_HEALTH, settings.elderHealth());
            setBase(guardian, Attribute.ATTACK_DAMAGE, settings.elderAttackDamage());
            guardian.setHealth(settings.elderHealth());
            guardian.setRemoveWhenFarAway(false);
        });
    }

    private static void setBase(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static CompletableFuture<Result> done(Result result) {
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Sends monument.broadcast.{@code key} from messages.yml to everyone with the admin permission (and
     * the console), when broadcasts are on.
     */
    private void announce(String key) {
        if (monuments.settings().announce()) {
            monuments.messages().broadcast(NltStructuresPlugin.ADMIN_PERMISSION, "monument.broadcast." + key);
        }
    }
}
