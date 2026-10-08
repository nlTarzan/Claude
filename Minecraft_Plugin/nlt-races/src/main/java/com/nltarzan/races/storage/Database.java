package com.nltarzan.races.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Connection pool plus a single background thread for all queries, so the server never waits on
 * the database. Results come back as futures; hop back to the main thread before touching the world.
 */
public final class Database {

    /** A piece of database work that runs on the database thread with an open connection. */
    @FunctionalInterface
    public interface SqlTask<T> {
        T run(Connection connection) throws SQLException;
    }

    private final HikariDataSource dataSource;
    private final boolean mysql;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nlT-Races-DB");
        thread.setDaemon(true);
        return thread;
    });

    public Database(JavaPlugin plugin) throws SQLException {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("database");
        String type = cfg == null ? "sqlite" : cfg.getString("type", "sqlite").toLowerCase(Locale.ROOT);
        mysql = type.equals("mysql") || type.equals("mariadb");

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("nlT-Races");
        if (mysql) {
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setJdbcUrl("jdbc:mysql://" + cfg.getString("mysql.host") + ":" + cfg.getInt("mysql.port")
                    + "/" + cfg.getString("mysql.database")
                    + "?useSSL=" + cfg.getBoolean("mysql.use-ssl")
                    + "&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikari.setUsername(cfg.getString("mysql.username"));
            hikari.setPassword(cfg.getString("mysql.password"));
            hikari.setMaximumPoolSize(cfg.getInt("mysql.pool-size", 4));
        } else {
            String fileName = cfg == null ? "races.db" : cfg.getString("sqlite-file", "races.db");
            File file = new File(plugin.getDataFolder(), fileName);
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setMaximumPoolSize(1); // SQLite allows a single writer
        }
        dataSource = new HikariDataSource(hikari);
        createSchema();
    }

    private void createSchema() throws SQLException {
        String id = mysql ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            // One row per race that was run (a solo run is a race with one player).
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS races (
                        id %s,
                        course VARCHAR(64) NOT NULL,
                        format VARCHAR(16) NOT NULL,
                        travel VARCHAR(16) NOT NULL,
                        vehicle VARCHAR(32),
                        started_at BIGINT NOT NULL,
                        ended_at BIGINT
                    )""".formatted(id));
            // One row per player per race. time_ms is null for DQ/DNF; points is only used in points races.
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS runs (
                        id %s,
                        race_id BIGINT NOT NULL,
                        course VARCHAR(64) NOT NULL,
                        player_uuid CHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        status VARCHAR(8) NOT NULL,
                        dq_reason VARCHAR(32),
                        time_ms BIGINT,
                        points INT,
                        placing INT,
                        completed_at BIGINT NOT NULL,
                        FOREIGN KEY (race_id) REFERENCES races (id) ON DELETE CASCADE
                    )""".formatted(id));
            // Time at each checkpoint, for comparing a run against a personal best.
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS run_splits (
                        run_id BIGINT NOT NULL,
                        checkpoint INT NOT NULL,
                        time_ms BIGINT NOT NULL,
                        PRIMARY KEY (run_id, checkpoint),
                        FOREIGN KEY (run_id) REFERENCES runs (id) ON DELETE CASCADE
                    )""");
            createIndex(st, "idx_runs_course_time", "runs (course, completed_at)");
            createIndex(st, "idx_runs_course_player", "runs (course, player_uuid)");
            createIndex(st, "idx_runs_race", "runs (race_id)");
        }
    }

    private void createIndex(Statement st, String name, String target) throws SQLException {
        if (!mysql) {
            st.executeUpdate("CREATE INDEX IF NOT EXISTS " + name + " ON " + target);
            return;
        }
        // MySQL has no CREATE INDEX IF NOT EXISTS; error 1061 means the index is already there.
        try {
            st.executeUpdate("CREATE INDEX " + name + " ON " + target);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1061) {
                throw e;
            }
        }
    }

    /** Runs {@code task} on the database thread. */
    public <T> CompletableFuture<T> submit(SqlTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection()) {
                return task.run(c);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public String getType() {
        return mysql ? "MySQL" : "SQLite";
    }

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS); // let pending saves finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        dataSource.close();
    }
}
