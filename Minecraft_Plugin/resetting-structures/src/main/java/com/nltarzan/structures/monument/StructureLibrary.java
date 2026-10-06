package com.nltarzan.structures.monument;

import com.nltarzan.structures.monument.layout.RoomType;
import com.nltarzan.structures.util.Weighted;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads the structure templates (.nbt files bundled in the jar) once, so placing is instant: the
 * pre-built monument quadrants and the pieces generated monuments are built from.
 */
public final class StructureLibrary {

    private static final String PRE_GEN = "structures/monument/pre-gen/";
    private static final String MAZE = "structures/monument/maze/";

    /** Every maze template, by its path below structures/monument/maze/. */
    public static final List<String> MAZE_TEMPLATES = Stream.concat(
            Stream.of("monument_empty_nw", "monument_empty_ne", "monument_empty_sw", "monument_empty_se",
                    "roof_open", "room/wing_open", "room/wing_closed", "room/penthouse"),
            Arrays.stream(RoomType.values()).map(RoomType::template)).toList();

    private final Map<String, Structure> mazeTemplates = new LinkedHashMap<>();

    /** One pre-built monument: four 29x29 quadrants placed around the center. */
    public record MonumentVariant(String name, Structure se, Structure sw, Structure ne, Structure nw) {
    }

    private final JavaPlugin plugin;
    private final Map<String, MonumentVariant> variants = new LinkedHashMap<>();
    private List<Weighted<MonumentVariant>> weighted = List.of();

    public StructureLibrary(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads every configured variant. Variants with missing files are skipped with a warning. */
    public void load(List<Weighted<String>> configured) {
        variants.clear();
        List<Weighted<MonumentVariant>> loaded = new ArrayList<>();
        for (Weighted<String> entry : configured) {
            String name = entry.value();
            try {
                MonumentVariant variant = new MonumentVariant(name,
                        read(name + "_se"), read(name + "_sw"), read(name + "_ne"), read(name + "_nw"));
                variants.put(name, variant);
                loaded.add(new Weighted<>(variant, entry.weight()));
            } catch (IOException e) {
                plugin.getLogger().warning("Skipping variant " + name + ": " + e.getMessage());
            }
        }
        weighted = List.copyOf(loaded);
        plugin.getLogger().info("Loaded " + variants.size() + " monument variants.");

        mazeTemplates.clear();
        for (String template : MAZE_TEMPLATES) {
            try {
                mazeTemplates.put(template, readFile(MAZE + template + ".nbt"));
            } catch (IOException e) {
                plugin.getLogger().warning("Generated monuments are unavailable: " + e.getMessage());
                mazeTemplates.clear();
                return;
            }
        }
        plugin.getLogger().info("Loaded " + mazeTemplates.size() + " templates for generated monuments.");
    }

    /** Whether every template for generated monuments is loaded. */
    public boolean mazeReady() {
        return mazeTemplates.size() == MAZE_TEMPLATES.size();
    }

    /** A template for generated monuments, by its path below structures/monument/maze/. */
    public Structure maze(String template) {
        Structure structure = mazeTemplates.get(template);
        if (structure == null) {
            throw new IllegalStateException("Template " + template + " is not loaded");
        }
        return structure;
    }

    public Optional<MonumentVariant> get(String name) {
        return Optional.ofNullable(variants.get(name));
    }

    public Set<String> names() {
        return variants.keySet();
    }

    public List<Weighted<MonumentVariant>> weighted() {
        return weighted;
    }

    private Structure read(String file) throws IOException {
        return readFile(PRE_GEN + file + ".nbt");
    }

    private Structure readFile(String path) throws IOException {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                throw new IOException("missing " + path);
            }
            return Bukkit.getStructureManager().loadStructure(in);
        }
    }
}
