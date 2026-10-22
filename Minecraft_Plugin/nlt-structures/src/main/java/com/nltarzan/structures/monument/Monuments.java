package com.nltarzan.structures.monument;

import com.nltarzan.structures.StructureModule;
import com.nltarzan.structures.config.ConfigUpdater;
import com.nltarzan.structures.config.Messages;
import com.nltarzan.structures.info.CommandHelp;
import com.nltarzan.structures.info.StatusLine;
import com.nltarzan.structures.monument.CurrentMonument.Built;
import com.nltarzan.structures.monument.MonumentResetter.Result;
import com.nltarzan.structures.monument.command.MonumentCommand;
import com.nltarzan.structures.monument.config.MonumentConfig;
import com.nltarzan.structures.monument.config.MonumentConfig.ResetMode;
import com.nltarzan.structures.monument.listener.ElderGuardianListener;
import com.nltarzan.structures.monument.listener.MonumentHoe;
import com.nltarzan.structures.monument.listener.MonumentHoeListener;
import com.nltarzan.structures.util.Durations;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Everything for the Ocean Monument ("monuments" for short): its config file and saved state (both
 * in the monuments/ folder), the reset timer, the hoe rules and /nltstructures monument.
 * Other structures get a module like this of their own.
 *
 * <p>A monuments.yml that can't be read or has no valid location: section never leads to a
 * monument being built somewhere else: on startup the monument is paused (no checks, no resets, no
 * hoe rules) until the file is fixed and reloaded; on a reload the previous settings stay in use.
 *
 * <p>The same goes while /nltstructures monument active is off, which it is on a new server: whoever
 * installs the plugin first checks the location instead of getting a monument at the default one.
 */
public final class Monuments implements StructureModule {

    private static final String CONFIG_FILE = "monuments.yml";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** The outcome of the last check, for the status. */
    private record LastCheck(LocalDateTime time, Result result) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final MonumentCommand command;
    private final Runnable statusChanged;
    private final File configFile;
    private final File folder;
    private YamlConfiguration config;

    private MonumentConfig settings;
    /** What's wrong with monuments.yml, or null when it's fine. */
    private String configProblem;
    /** True when no valid monuments.yml has been loaded since the server started. */
    private boolean paused;
    private LastCheck lastCheck;

    private StructureLibrary structures;
    private MonumentResetter resetter;
    private ResetScheduler scheduler;
    private CurrentMonument currentMonument;

    /**
     * @param statusChanged called whenever the monument's status changes, to rewrite status.txt
     */
    public Monuments(JavaPlugin plugin, Messages messages, Runnable statusChanged) {
        this.plugin = plugin;
        this.messages = messages;
        this.command = new MonumentCommand(this);
        this.statusChanged = statusChanged;
        this.folder = new File(plugin.getDataFolder(), "monuments");
        this.configFile = new File(folder, CONFIG_FILE);
    }

    @Override
    public String name() {
        return "Ocean Monument";
    }

    @Override
    public String commandName() {
        return "monument";
    }

    @Override
    public void enable() {
        folder.mkdirs();
        File oldConfig = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (oldConfig.exists()) {
            moveOldConfig(oldConfig);
        }
        // Never copy the default over a config that couldn't be moved: without a config the monument
        // is paused, while the default's location could be the wrong place.
        if (!configFile.exists() && !oldConfig.exists()) {
            plugin.saveResource(folder.getName() + "/" + CONFIG_FILE, false);
        }
        updateConfig();
        loadConfig();
        structures = new StructureLibrary(plugin);
        structures.load(settings.variants());
        currentMonument = new CurrentMonument(plugin.getLogger(), folder);
        resetter = new MonumentResetter(this);
        scheduler = new ResetScheduler(this);
        if (!paused) {
            scheduler.start();
            if (!scheduler.isActive()) {
                getLogger().warning("The monument is inactive, so nothing is checked or built. On a new server,"
                        + " check the location in " + CONFIG_FILE + " (or stand on the center and run /nltstructures"
                        + " monument setcenter), then run /nltstructures monument active on.");
            }
        }

        MonumentHoe hoe = new MonumentHoe(plugin);
        plugin.getServer().getPluginManager().registerEvents(new ElderGuardianListener(this, hoe), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MonumentHoeListener(this, hoe), plugin);
    }

    @Override
    public void disable() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    @Override
    public MonumentCommand command() {
        return command;
    }

    @Override
    public List<CommandHelp> commandHelp() {
        return MonumentCommand.HELP;
    }

    /**
     * Re-reads monuments.yml and the structures, and restarts the timer (keeping the saved
     * countdown). When the file has a problem the previous settings stay in use.
     *
     * @return whether monuments.yml was fine
     */
    public boolean reloadSettings() {
        updateConfig();
        boolean ok = loadConfig();
        if (ok) {
            structures.load(settings.variants());
            scheduler.start();
        }
        statusChanged.run();
        return ok;
    }

    /**
     * Turns the broadcast messages on or off and saves it in monuments.yml (reset.announce).
     *
     * @return false when monuments.yml has a problem; it's then not saved, so it can't overwrite the file
     */
    public boolean setAnnounce(boolean announce) {
        if (configProblem != null) {
            return false;
        }
        config.set("reset.announce", announce);
        saveConfig();
        settings = MonumentConfig.load(config, getLogger());
        statusChanged.run();
        return true;
    }

    /**
     * Moves the monument's center and saves it in monuments.yml.
     *
     * @return false when monuments.yml has a problem; it's then not saved, so it can't overwrite the file
     */
    public boolean setCenter(World world, int x, int y, int z) {
        if (configProblem != null) {
            return false;
        }
        config.set("location.world", world.getName());
        config.set("location.x", x);
        config.set("location.y", y);
        config.set("location.z", z);
        saveConfig();
        reloadSettings();
        return true;
    }

    /**
     * Turns the checks and resets on or off. Saved in monuments/state.yml, so it lasts through restarts.
     *
     * @return false when turning on while monuments.yml has a problem; the location isn't known then
     */
    public boolean setActive(boolean active) {
        if (active && paused) {
            return false;
        }
        scheduler.setActive(active);
        statusChanged.run();
        return true;
    }

    /** Remembers the outcome of a check for the status. Called by the scheduler after every check. */
    void recordCheck(Result result) {
        lastCheck = new LastCheck(LocalDateTime.now(), result);
        statusChanged.run();
    }

    /** The monument area, or {@code null} when its world isn't loaded or the monument isn't active. */
    public MonumentArena arena() {
        if (!isActive()) {
            return null;
        }
        World world = plugin.getServer().getWorld(settings.worldName());
        if (world == null) {
            return null;
        }
        return new MonumentArena(plugin, world, settings.centerX(), settings.centerY(), settings.centerZ(),
                settings.checkRadius());
    }

    @Override
    public List<StatusLine> status() {
        List<StatusLine> lines = new ArrayList<>();
        if (configProblem != null) {
            lines.add(new StatusLine("Problem", "monuments.yml " + configProblem + (paused
                    ? ". Paused until it's fixed and reloaded."
                    : ". Still using the previous settings."), NamedTextColor.RED));
        }
        lines.add(new StatusLine("Location", paused ? "Unknown" : settings.worldName() + " "
                + settings.centerX() + " " + settings.centerY() + " " + settings.centerZ(),
                paused ? NamedTextColor.DARK_GRAY : NamedTextColor.WHITE));
        lines.add(currentMonument.get()
                .map(built -> new StatusLine("Monument", switch (built) {
                    case Built.PreBuilt(String variant) -> "Pre-built " + variant;
                    case Built.Generated generated -> "Generated, seed " + generated.seed();
                }, NamedTextColor.AQUA))
                .orElse(new StatusLine("Monument", "Not placed by the plugin yet", NamedTextColor.DARK_GRAY)));
        currentMonument.sponges().ifPresent(sponges ->
                lines.add(new StatusLine("Sponges", String.valueOf(sponges), NamedTextColor.YELLOW)));

        MonumentArena arena = arena();
        if (arena != null && arena.world().isChunkLoaded(arena.center().getBlockX() >> 4, arena.center().getBlockZ() >> 4)) {
            int alive = arena.entities(ElderGuardian.class).size();
            lines.add(new StatusLine("Elder Guardians", alive + " alive",
                    alive > 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
        }
        if (lastCheck != null) {
            lines.add(lastCheckLine());
        }

        String mode = settings.mode() == ResetMode.GENERATED ? "Generated" : "Pre-built";
        lines.add(structures.mazeReady()
                ? new StatusLine("Reset mode", mode, NamedTextColor.AQUA)
                : new StatusLine("Reset mode", mode + " (generated templates missing)", NamedTextColor.RED));

        long ticks = scheduler.ticksUntilNextCheck();
        lines.add(scheduler.isActive()
                ? new StatusLine("Active", "On", NamedTextColor.GREEN)
                : new StatusLine("Active", "Off: nothing is checked or built. Check the location, then run"
                        + " /nltstructures monument active on", NamedTextColor.GOLD));
        // While inactive there's no next check; the Active line above already says why.
        if (paused) {
            lines.add(new StatusLine("Next check", "Paused", NamedTextColor.RED));
        } else if (scheduler.isActive()) {
            lines.add(ticks < 0
                    ? new StatusLine("Next check", "Not scheduled", NamedTextColor.RED)
                    : new StatusLine("Next check", "in " + Durations.format(ticks) + " (around "
                            + clockIn(ticks / Durations.TICKS_PER_SECOND) + ")", NamedTextColor.WHITE));
        }
        lines.add(settings.announce()
                ? new StatusLine("Broadcasts", "On", NamedTextColor.GREEN)
                : new StatusLine("Broadcasts", "Off", NamedTextColor.RED));
        return lines;
    }

    private StatusLine lastCheckLine() {
        String time = clock(lastCheck.time());
        return switch (lastCheck.result()) {
            case Result.NotCleared(int alive) -> new StatusLine("Last check",
                    time + ", not cleared: " + alive + " Elder Guardian(s) alive", NamedTextColor.RED);
            case Result.PlayersInside(int players) -> new StatusLine("Last check",
                    time + ", cleared but " + players + " player(s) inside, reset postponed", NamedTextColor.YELLOW);
            case Result.Placed(String placed) -> new StatusLine("Last check",
                    time + ", reset: " + placed, NamedTextColor.GREEN);
            case Result.Failed(String reason) -> new StatusLine("Last check",
                    time + ", failed: " + reason, NamedTextColor.RED);
        };
    }

    /** A clock time, with the date when it isn't today. */
    private static String clock(LocalDateTime time) {
        return time.toLocalDate().equals(LocalDate.now()) ? CLOCK.format(time) : DATE_CLOCK.format(time);
    }

    /** The clock time a number of seconds from now (an estimate: game time only runs while the server does). */
    private static String clockIn(long seconds) {
        return clock(LocalDateTime.now().plusSeconds(seconds));
    }

    /**
     * Adds settings a newer version introduced to monuments.yml (and drops ones it no longer uses), keeping the
     * admin's values and their own variants. A file that can't be read or has no valid location is left alone.
     */
    private void updateConfig() {
        ConfigUpdater.update(plugin, folder.getName() + "/" + CONFIG_FILE, configFile, Set.of("variants"),
                MonumentConfig::locationProblem);
    }

    /**
     * Reads monuments.yml strictly.
     *
     * @return whether it was fine; if not, the previous settings stay, or on startup the monument is paused
     */
    private boolean loadConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        String problem;
        String details = null;
        try {
            yaml.load(configFile);
            problem = MonumentConfig.locationProblem(yaml);
        } catch (IOException | InvalidConfigurationException e) {
            problem = "can't be read (" + firstLine(e.getMessage()) + ")";
            details = e.getMessage();
        }

        if (problem == null) {
            config = yaml;
            settings = MonumentConfig.load(yaml, getLogger());
            configProblem = null;
            paused = false;
            return true;
        }

        configProblem = problem;
        if (settings == null) {
            // Startup: no earlier settings to fall back on. Use the defaults for everything except the
            // location, and pause so nothing is checked or built until the file is fixed.
            config = yaml;
            settings = MonumentConfig.load(new YamlConfiguration(), Logger.getAnonymousLogger());
            paused = true;
            getLogger().severe(CONFIG_FILE + " " + problem + ". The monument is paused: no checks, resets or hoe"
                    + " rules until it's fixed and /nltstructures monument reload is run.");
        } else {
            getLogger().severe(CONFIG_FILE + " " + problem + ". Still using the previous settings; fix it and"
                    + " run /nltstructures monument reload again.");
        }
        if (details != null) {
            getLogger().severe(details);
        }
        return false;
    }

    /** Moves monuments.yml from the plugin's folder (where older versions kept it) into monuments/. */
    private void moveOldConfig(File oldConfig) {
        if (configFile.exists()) {
            getLogger().warning("There's a " + CONFIG_FILE + " in both " + plugin.getDataFolder().getName() + "/ and "
                    + folder.getName() + "/. Using " + folder.getName() + "/" + CONFIG_FILE + "; the other one is"
                    + " ignored and can be deleted.");
            return;
        }
        try {
            Files.move(oldConfig.toPath(), configFile.toPath());
            getLogger().info("Moved " + CONFIG_FILE + " into the " + folder.getName() + "/ folder.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not move " + CONFIG_FILE + " into the " + folder.getName()
                    + "/ folder. Move it there by hand and run /nltstructures monument reload.", e);
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown error";
        }
        String line = message.strip().lines().findFirst().orElse(message);
        return line.length() > 120 ? line.substring(0, 120) + "..." : line;
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not save " + CONFIG_FILE, e);
        }
    }

    /** What's wrong with monuments.yml, or {@code null} when it's fine. */
    public String configProblem() {
        return configProblem;
    }

    /** Whether the monument is paused because no valid monuments.yml was loaded. */
    public boolean isPaused() {
        return paused;
    }

    /** Whether checks, resets and the hoe rules run: monuments.yml is fine and active is on. */
    public boolean isActive() {
        return !paused && scheduler.isActive();
    }

    /** The plugin itself, for Bukkit calls that need it (scheduling tasks, chunk tickets). */
    public JavaPlugin plugin() {
        return plugin;
    }

    /** The monuments/ folder in the plugin's folder, for the saved state. */
    public File folder() {
        return folder;
    }

    /** The plugin's messages.yml, shared by every structure. */
    public Messages messages() {
        return messages;
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public Server getServer() {
        return plugin.getServer();
    }

    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    public MonumentConfig settings() {
        return settings;
    }

    public StructureLibrary structures() {
        return structures;
    }

    public MonumentResetter resetter() {
        return resetter;
    }

    public ResetScheduler scheduler() {
        return scheduler;
    }

    public CurrentMonument currentMonument() {
        return currentMonument;
    }
}
