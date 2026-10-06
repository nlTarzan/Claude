package com.nltarzan.structures.monument;

import com.nltarzan.structures.monument.layout.MazeSettings;
import com.nltarzan.structures.monument.layout.MonumentLayout;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Remembers which monument is built right now (saved in current.yml): a pre-built variant or a
 * generated layout, and how many sponges it had. For a generated one it keeps the seed and the maze
 * settings used, which is all it takes to generate exactly the same layout again.
 */
public final class CurrentMonument {

    /** What's built: a pre-built variant name, or the seed and settings of a generated layout. */
    public sealed interface Built {
        record PreBuilt(String variant) implements Built {
        }

        record Generated(long seed, MazeSettings settings) implements Built {
            public MonumentLayout layout() {
                return MonumentLayout.generate(seed, settings);
            }
        }
    }

    private final Logger logger;
    private final File file;
    private Built built;
    private int sponges = -1;
    private long id;

    /** @param folder where current.yml is kept */
    public CurrentMonument(Logger logger, File folder) {
        this.logger = logger;
        this.file = new File(folder, "current.yml");
        load();
    }

    /**
     * A random number that changes every time a new monument is placed. Monument Hoes carry the id
     * of the monument they came from, so a hoe kept from an earlier monument can be recognised.
     */
    public long id() {
        return id;
    }

    /** Empty until the plugin has placed its first monument. */
    public Optional<Built> get() {
        return Optional.ofNullable(built);
    }

    /** How many sponges the monument had when it was placed, or empty when unknown. */
    public OptionalInt sponges() {
        return sponges < 0 ? OptionalInt.empty() : OptionalInt.of(sponges);
    }

    /**
     * @param sponges the number of sponges in the monument right after placing it
     */
    public void set(Built built, int sponges) {
        this.built = built;
        this.sponges = sponges;
        this.id = newId();
        save();
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", id);
        yaml.set("sponges", sponges);
        switch (built) {
            case null -> {
            }
            case Built.PreBuilt(String variant) -> {
                yaml.set("type", "pre-built");
                yaml.set("variant", variant);
            }
            case Built.Generated(long seed, MazeSettings s) -> {
                yaml.set("type", "generated");
                yaml.set("seed", seed);
                yaml.set("peak", s.peak());
                yaml.set("spread", s.spread());
                yaml.set("min", s.min());
                yaml.set("max", s.max());
                yaml.set("min-sponge-rooms", s.minSpongeRooms());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save " + file.getName(), e);
        }
    }

    private static long newId() {
        return ThreadLocalRandom.current().nextLong();
    }

    private void load() {
        if (!file.exists()) {
            // No monument placed by the plugin yet; give the one in the world an id that survives restarts.
            id = newId();
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        sponges = yaml.getInt("sponges", -1);
        id = yaml.contains("id") ? yaml.getLong("id") : newId();
        try {
            built = switch (yaml.getString("type", "")) {
                case "pre-built" -> new Built.PreBuilt(yaml.getString("variant", "?"));
                case "generated" -> new Built.Generated(yaml.getLong("seed"), new MazeSettings(
                        yaml.getDouble("peak"), yaml.getDouble("spread"), yaml.getDouble("min"),
                        yaml.getDouble("max"), yaml.getInt("min-sponge-rooms")));
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            logger.warning("Ignoring " + file.getName() + ": " + e.getMessage());
        }
        if (!yaml.contains("id")) {
            save();
        }
    }
}
