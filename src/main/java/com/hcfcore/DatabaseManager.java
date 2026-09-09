package com.hcfcore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final HCFCore plugin;

    private Connection connection;

    public DatabaseManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {

        try {

            File folder = plugin.getDataFolder();

            if (!folder.exists() && !folder.mkdirs()) {

                plugin.getLogger().severe(
                        "No se pudo crear la carpeta de HCFCore."
                );

                return false;
            }

            File databaseFile =
                    new File(folder, "database.db");

            String url =
                    "jdbc:sqlite:"
                            + databaseFile.getAbsolutePath();

            connection =
                    DriverManager.getConnection(url);

            /*
             * =========================
             * SQLITE SETTINGS
             * =========================
             */

            try (Statement statement =
                         connection.createStatement()) {

                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA busy_timeout = 5000");
            }

            plugin.getLogger().info(
                    "Base de datos SQLite conectada."
            );

            if (!createTables()) {

                plugin.getLogger().severe(
                        "No se pudieron crear/verificar las tablas SQLite."
                );

                disconnect();

                return false;
            }

            return true;

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "No se pudo conectar a SQLite."
            );

            e.printStackTrace();

            return false;
        }
    }

    private boolean createTables() {

        if (connection == null) {
            return false;
        }

        try (Statement statement =
                     connection.createStatement()) {

            /*
             * =========================
             * FACTIONS
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS factions (
                        name TEXT PRIMARY KEY,
                        leader TEXT NOT NULL,
                        dtr REAL NOT NULL DEFAULT 0,
                        balance REAL NOT NULL DEFAULT 0
                    )
                    """);

            /*
             * =========================
             * FACTION MEMBERS
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_members (
                        faction TEXT NOT NULL,
                        uuid TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'MEMBER',
                        PRIMARY KEY (faction, uuid),
                        FOREIGN KEY (faction)
                            REFERENCES factions(name)
                            ON DELETE CASCADE
                    )
                    """);

            /*
             * =========================
             * ALLIES
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_allies (
                        faction TEXT NOT NULL,
                        target TEXT NOT NULL,
                        PRIMARY KEY (faction, target)
                    )
                    """);

            /*
             * =========================
             * ENEMIES
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_enemies (
                        faction TEXT NOT NULL,
                        target TEXT NOT NULL,
                        PRIMARY KEY (faction, target)
                    )
                    """);

            /*
             * =========================
             * FACTION HOMES
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_homes (
                        faction TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        FOREIGN KEY (faction)
                            REFERENCES factions(name)
                            ON DELETE CASCADE
                    )
                    """);

            /*
             * =========================
             * CLAIMS
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS claims (
                        world TEXT NOT NULL,
                        chunk_x INTEGER NOT NULL,
                        chunk_z INTEGER NOT NULL,
                        faction TEXT NOT NULL,
                        PRIMARY KEY (world, chunk_x, chunk_z),
                        FOREIGN KEY (faction)
                            REFERENCES factions(name)
                            ON DELETE CASCADE
                    )
                    """);

            /*
             * =========================
             * PLAYER STATS
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid TEXT PRIMARY KEY,
                        kills INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            /*
             * =========================
             * DEATHBANS
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS deathbans (
                        uuid TEXT PRIMARY KEY,
                        expires_at INTEGER NOT NULL
                    )
                    """);

            /*
             * =========================
             * LIVES
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lives (
                        uuid TEXT PRIMARY KEY,
                        amount INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            /*
             * =========================
             * ECONOMY
             * =========================
             */

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS economy (
                        uuid TEXT PRIMARY KEY,
                        balance REAL NOT NULL DEFAULT 0
                    )
                    """);

            /*
             * =========================
             * INDEXES
             * =========================
             */

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_faction_members_faction
                    ON faction_members(faction)
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_faction_members_uuid
                    ON faction_members(uuid)
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_claims_faction
                    ON claims(faction)
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_claims_world
                    ON claims(world)
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_deathbans_expires
                    ON deathbans(expires_at)
                    """);

            plugin.getLogger().info(
                    "Tablas SQLite verificadas correctamente."
            );

            return true;

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error creando las tablas SQLite."
            );

            e.printStackTrace();

            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnected() {

        try {

            return connection != null
                    && !connection.isClosed();

        } catch (SQLException e) {

            return false;
        }
    }

    public void disconnect() {

        if (connection == null) {
            return;
        }

        try {

            if (!connection.isClosed()) {
                connection.close();
            }

            plugin.getLogger().info(
                    "Base de datos SQLite cerrada."
            );

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudo cerrar SQLite correctamente."
            );

            e.printStackTrace();

        } finally {

            connection = null;
        }
    }
}