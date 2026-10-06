package com.nltarzan.structures.monument;

import com.nltarzan.structures.monument.build.BuildStep;
import com.nltarzan.structures.monument.build.BuildStep.Fill;
import com.nltarzan.structures.monument.build.BuildStep.Offset;
import com.nltarzan.structures.monument.build.BuildStep.Place;
import com.nltarzan.structures.monument.build.BuildStep.SpawnElder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Carries out build steps in the world, in order, spread over several ticks: every tick it runs
 * steps until its time budget is used up, so building a monument doesn't freeze the server.
 */
public final class MonumentBuilder {

    /** Time per tick spent building; the rest of the 50 ms tick is left for the server. */
    private static final long BUDGET_NANOS = 15_000_000L;

    private final Monuments monuments;

    public MonumentBuilder(Monuments monuments) {
        this.monuments = monuments;
    }

    /**
     * @param spawnElder spawns an Elder Guardian at a location (with the configured stats)
     * @return completes on the main thread once every step has run
     */
    public CompletableFuture<Void> build(MonumentArena arena, List<BuildStep> steps, long seed,
                                         Consumer<Location> spawnElder) {
        Deque<BuildStep> queue = new ArrayDeque<>(steps);
        Random random = new Random(seed);
        CompletableFuture<Void> done = new CompletableFuture<>();
        BukkitTask[] task = new BukkitTask[1];

        task[0] = monuments.getServer().getScheduler().runTaskTimer(monuments.plugin(), () -> {
            long start = System.nanoTime();
            try {
                do {
                    BuildStep step = queue.poll();
                    if (step == null) {
                        break;
                    }
                    run(step, arena, random, spawnElder);
                } while (System.nanoTime() - start < BUDGET_NANOS);
            } catch (RuntimeException e) {
                task[0].cancel();
                done.completeExceptionally(e);
                return;
            }
            if (queue.isEmpty()) {
                task[0].cancel();
                done.complete(null);
            }
        }, 1L, 1L);
        return done;
    }

    private void run(BuildStep step, MonumentArena arena, Random random, Consumer<Location> spawnElder) {
        Location center = arena.center();
        World world = arena.world();
        switch (step) {
            case Place(String template, Offset origin, BuildStep.Rotation rotation) ->
                    monuments.structures().maze(template).place(at(center, origin), true, rotation(rotation),
                            Mirror.NONE, -1, 1f, random);
            case Fill(Offset from, Offset to, BuildStep.Block block) -> {
                Material material = material(block);
                Location a = at(center, from);
                Location b = at(center, to);
                for (int x = Math.min(a.getBlockX(), b.getBlockX()); x <= Math.max(a.getBlockX(), b.getBlockX()); x++) {
                    for (int y = Math.min(a.getBlockY(), b.getBlockY()); y <= Math.max(a.getBlockY(), b.getBlockY()); y++) {
                        for (int z = Math.min(a.getBlockZ(), b.getBlockZ()); z <= Math.max(a.getBlockZ(), b.getBlockZ()); z++) {
                            world.getBlockAt(x, y, z).setType(material, false);
                        }
                    }
                }
            }
            case SpawnElder(double x, double y, double z) -> spawnElder.accept(center.clone().add(x, y, z));
        }
    }

    private static Location at(Location center, Offset offset) {
        return center.clone().add(offset.x(), offset.y(), offset.z());
    }

    private static StructureRotation rotation(BuildStep.Rotation rotation) {
        return switch (rotation) {
            case NONE -> StructureRotation.NONE;
            case CLOCKWISE_90 -> StructureRotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> StructureRotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> StructureRotation.COUNTERCLOCKWISE_90;
        };
    }

    private static Material material(BuildStep.Block block) {
        return switch (block) {
            case WATER -> Material.WATER;
            case PRISMARINE -> Material.PRISMARINE;
            case PRISMARINE_BRICKS -> Material.PRISMARINE_BRICKS;
            case WET_SPONGE -> Material.WET_SPONGE;
        };
    }
}
