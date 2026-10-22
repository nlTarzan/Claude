package com.nltarzan.races;

import com.nltarzan.races.command.NltRacesCommand;
import com.nltarzan.races.config.ConfigUpdater;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.leaderboard.LeaderboardService;
import com.nltarzan.races.leaderboard.PlaceholderHook;
import com.nltarzan.races.race.RaceRegistry;
import com.nltarzan.races.runtime.RaceListener;
import com.nltarzan.races.runtime.RaceService;
import com.nltarzan.races.runtime.RuleListener;
import com.nltarzan.races.setup.SetupListener;
import com.nltarzan.races.setup.SetupService;
import com.nltarzan.races.storage.Database;
import com.nltarzan.races.storage.RunStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class NltRacesPlugin extends JavaPlugin {

    private Messages messages;
    private Database database;
    private RaceRegistry races;
    private RunStore runStore;
    private RaceService raceService;
    private SetupService setupService;
    private LeaderboardService leaderboards;
    /** Set when PlaceholderAPI is installed and accepted our placeholders. */
    private PlaceholderHook placeholders;

    @Override
    public void onEnable() {
        String configProblem = ConfigUpdater.update(this);
        if (configProblem != null) {
            // Running on the defaults instead could, for example, save times to a new SQLite file
            // instead of the admin's MySQL database.
            getLogger().severe("config.yml " + configProblem + ". Fix it and restart the server. Disabling nlT-Races.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();
        messages = new Messages(this);

        try {
            database = new Database(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not connect to the database. Check the database section of "
                    + "config.yml. Disabling nlT-Races.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        races = new RaceRegistry(this);
        races.load();

        runStore = new RunStore(database);
        leaderboards = new LeaderboardService(this, runStore);
        leaderboards.refreshAll();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholders = PlaceholderHook.register(this);
            getLogger().info(placeholders != null ? "Registered the %nltraces_...% placeholders."
                    : "PlaceholderAPI refused the %nltraces_...% placeholders.");
        }
        raceService = new RaceService(this, runStore);
        getServer().getPluginManager().registerEvents(new RaceListener(this, raceService), this);
        getServer().getPluginManager().registerEvents(new RuleListener(this, raceService), this);

        setupService = new SetupService(this);
        getServer().getPluginManager().registerEvents(new SetupListener(setupService), this);

        PluginCommand command = getCommand("nltraces");
        NltRacesCommand executor = new NltRacesCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("nlT-Races " + getPluginMeta().getVersion() + " enabled ("
                + database.getType() + " database, " + races.all().size() + " race(s)).");
    }

    @Override
    public void onDisable() {
        if (setupService != null) {
            setupService.shutdown();
        }
        if (raceService != null) {
            raceService.shutdown();
        }
        if (placeholders != null) {
            placeholders.unregister();
        }
        if (leaderboards != null) {
            leaderboards.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    /**
     * Re-reads config.yml, messages.yml and the race files. Database settings need a restart.
     *
     * @return null, or what's wrong with config.yml; its previous settings then stay in use
     */
    public String reload() {
        String configProblem = ConfigUpdater.update(this);
        if (configProblem == null) {
            reloadConfig();
        } else {
            getLogger().severe("config.yml " + configProblem + ". Still using the previous settings; fix it and "
                    + "run /nltraces reload again.");
        }
        messages.reload();
        races.load();
        leaderboards.refreshAll();
        return configProblem;
    }

    /** Runs a task on the main server thread, if the plugin is still enabled. */
    public void sync(Runnable task) {
        if (isEnabled()) {
            getServer().getScheduler().runTask(this, task);
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public Database getDatabase() {
        return database;
    }

    public RaceRegistry getRaces() {
        return races;
    }

    public RunStore getRunStore() {
        return runStore;
    }

    public RaceService getRaceService() {
        return raceService;
    }

    public SetupService getSetupService() {
        return setupService;
    }

    public LeaderboardService getLeaderboards() {
        return leaderboards;
    }
}
