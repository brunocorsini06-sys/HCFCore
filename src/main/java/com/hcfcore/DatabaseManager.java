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

            plugin.getLogger().info(
                    "Base de datos SQLite conectada."
            );

            createTables();

            return true;

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "No se pudo conectar a SQLite."
            );

            e.printStackTrace();

            return false;
        }
    }

    private void createTables() {

        if (connection == null) {
            return;
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

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_members (
                        faction TEXT NOT NULL,
                        uuid TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'MEMBER',
                        PRIMARY KEY (faction, uuid)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_allies (
                        faction TEXT NOT NULL,
                        target TEXT NOT NULL,
                        PRIMARY KEY (faction, target)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_enemies (
                        faction TEXT NOT NULL,
                        target TEXT NOT NULL,
                        PRIMARY KEY (faction, target)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_homes (
                        faction TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL
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
                        PRIMARY KEY (world, chunk_x, chunk_z)
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

            plugin.getLogger().info(
                    "Tablas SQLite verificadas correctamente."
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error creando las tablas SQLite."
            );

            e.printStackTrace();
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