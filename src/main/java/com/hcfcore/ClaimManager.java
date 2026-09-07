package com.hcfcore;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ClaimManager {

    private final HCFCore plugin;

    // Mundo + coordenadas de chunk → faction
    private final Map<String, String> claims = new HashMap<>();

    public ClaimManager(HCFCore plugin) {
        this.plugin = plugin;

        // Cargar claims guardados en SQLite
        loadAll();
    }

    /**
     * Genera una clave única para un chunk.
     */
    private String getKey(Chunk chunk) {
        return chunk.getWorld().getName()
                + ":"
                + chunk.getX()
                + ":"
                + chunk.getZ();
    }

    /**
     * Devuelve la faction propietaria del chunk.
     */
    public Faction getFactionAt(Location location) {

        if (location == null || location.getWorld() == null) {
            return null;
        }

        Chunk chunk = location.getChunk();

        String factionName = claims.get(getKey(chunk));

        if (factionName == null) {
            return null;
        }

        return plugin.getFactionManager()
                .getFaction(factionName);
    }

    /**
     * Comprueba si un chunk está reclamado.
     */
    public boolean isClaimed(Chunk chunk) {

        if (chunk == null) {
            return false;
        }

        return claims.containsKey(getKey(chunk));
    }

    /**
     * Comprueba si una ubicación está dentro de un claim.
     */
    public boolean isClaimed(Location location) {
        return getFactionAt(location) != null;
    }

    /**
     * Comprueba si el jugador puede construir/interactuar.
     */
    public boolean canBuild(Player player, Location location) {

        if (player == null || location == null) {
            return false;
        }

        Faction faction = getFactionAt(location);

        // Territorio libre
        if (faction == null) {
            return true;
        }

        // Bypass administrativo
        if (player.hasPermission("hcf.bypass")) {
            return true;
        }

        Faction playerFaction =
                plugin.getFactionManager()
                        .getFaction(player);

        // Sin faction
        if (playerFaction == null) {
            return false;
        }

        // Dueño del territorio
        return playerFaction.getName()
                .equalsIgnoreCase(faction.getName());
    }

    /**
     * Reclama el chunk donde está el jugador.
     */
    public boolean claim(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        if (faction == null) {
            return false;
        }

        Chunk chunk =
                player.getLocation().getChunk();

        String key = getKey(chunk);

        // Ya reclamado
        if (claims.containsKey(key)) {
            return false;
        }

        int maxClaims =
                plugin.getConfig()
                        .getInt(
                                "factions.max-claim-chunks",
                                20
                        );

        int currentClaims =
                getClaimCount(faction);

        if (currentClaims >= maxClaims) {
            return false;
        }

        // Si se exige que sean adyacentes
        boolean requireAdjacent =
                plugin.getConfig()
                        .getBoolean(
                                "factions.require-adjacent-claims",
                                true
                        );

        if (requireAdjacent
                && currentClaims > 0
                && !hasAdjacentClaim(
                        faction,
                        chunk
                )) {

            return false;
        }

        claims.put(
                key,
                faction.getName().toLowerCase()
        );

        // Guardar inmediatamente
        saveAll();

        return true;
    }

    /**
     * Abandona el claim actual.
     */
    public boolean unclaim(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        if (faction == null) {
            return false;
        }

        Chunk chunk =
                player.getLocation().getChunk();

        String key = getKey(chunk);

        String owner = claims.get(key);

        if (owner == null) {
            return false;
        }

        if (!owner.equalsIgnoreCase(
                faction.getName()
        )) {
            return false;
        }

        claims.remove(key);

        // Guardar inmediatamente
        saveAll();

        return true;
    }

    /**
     * Cuenta los chunks reclamados por una faction.
     */
    public int getClaimCount(Faction faction) {

        if (faction == null) {
            return 0;
        }

        int count = 0;

        String factionName =
                faction.getName().toLowerCase();

        for (String owner : claims.values()) {

            if (owner.equalsIgnoreCase(
                    factionName
            )) {
                count++;
            }
        }

        return count;
    }

    /**
     * Comprueba si el chunk toca
     * otro claim de la misma faction.
     */
    private boolean hasAdjacentClaim(
            Faction faction,
            Chunk chunk
    ) {

        if (faction == null || chunk == null) {
            return false;
        }

        int x = chunk.getX();
        int z = chunk.getZ();

        Chunk north =
                chunk.getWorld()
                        .getChunkAt(x, z - 1);

        Chunk south =
                chunk.getWorld()
                        .getChunkAt(x, z + 1);

        Chunk east =
                chunk.getWorld()
                        .getChunkAt(x + 1, z);

        Chunk west =
                chunk.getWorld()
                        .getChunkAt(x - 1, z);

        return ownsChunk(faction, north)
                || ownsChunk(faction, south)
                || ownsChunk(faction, east)
                || ownsChunk(faction, west);
    }

    /**
     * Comprueba si una faction posee el chunk.
     */
    private boolean ownsChunk(
            Faction faction,
            Chunk chunk
    ) {

        if (faction == null || chunk == null) {
            return false;
        }

        String owner =
                claims.get(getKey(chunk));

        return owner != null
                && owner.equalsIgnoreCase(
                        faction.getName()
                );
    }

    /**
     * Obtiene el dueño de un chunk.
     */
    public String getClaimOwner(Chunk chunk) {

        if (chunk == null) {
            return null;
        }

        return claims.get(
                getKey(chunk)
        );
    }

    /**
     * Obtiene el nombre del claim donde está el jugador.
     *
     * Usado por el scoreboard.
     */
    public String getClaimName(Player player) {

        if (player == null) {
            return "Wilderness";
        }

        Faction faction =
                getFactionAt(player.getLocation());

        if (faction == null) {
            return "Wilderness";
        }

        return faction.getName();
    }

    /**
     * Elimina todos los claims
     * pertenecientes a una faction.
     */
    public void removeFactionClaims(
            Faction faction
    ) {

        if (faction == null) {
            return;
        }

        String name =
                faction.getName().toLowerCase();

        boolean removed = claims.entrySet().removeIf(
                entry ->
                        entry.getValue()
                                .equalsIgnoreCase(name)
        );

        if (removed) {
            saveAll();
        }
    }

    /**
     * Devuelve todos los claims.
     */
    public Map<String, String> getClaims() {
        return claims;
    }

    /**
     * Carga todos los claims desde SQLite.
     */
    public void loadAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            plugin.getLogger().warning(
                    "No se pudieron cargar los claims: SQLite no está conectado."
            );

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        claims.clear();

        String sql = """
                SELECT world, chunk_x, chunk_z, faction
                FROM claims
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            int loaded = 0;

            while (result.next()) {

                String world =
                        result.getString("world");

                int chunkX =
                        result.getInt("chunk_x");

                int chunkZ =
                        result.getInt("chunk_z");

                String faction =
                        result.getString("faction");

                if (world == null
                        || faction == null) {
                    continue;
                }

                String key =
                        world
                                + ":"
                                + chunkX
                                + ":"
                                + chunkZ;

                claims.put(
                        key,
                        faction.toLowerCase()
                );

                loaded++;
            }

            plugin.getLogger().info(
                    "Claims cargados desde SQLite: "
                            + loaded
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando los claims desde SQLite."
            );

            e.printStackTrace();
        }
    }

    /**
     * Guarda todos los claims en SQLite.
     */
    public void saveAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            plugin.getLogger().warning(
                    "No se pudieron guardar los claims: SQLite no está conectado."
            );

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        String deleteSql =
                "DELETE FROM claims";

        String insertSql = """
                INSERT INTO claims
                (world, chunk_x, chunk_z, faction)
                VALUES (?, ?, ?, ?)
                """;

        try {

            boolean previousAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement delete =
                             connection.prepareStatement(
                                     deleteSql
                             )) {

                    delete.executeUpdate();
                }

                try (PreparedStatement insert =
                             connection.prepareStatement(
                                     insertSql
                             )) {

                    for (Map.Entry<String, String> entry
                            : claims.entrySet()) {

                        String key = entry.getKey();

                        int lastSeparator =
                                key.lastIndexOf(':');

                        if (lastSeparator <= 0) {
                            continue;
                        }

                        String coordinates =
                                key.substring(
                                        0,
                                        lastSeparator
                                );

                        int secondSeparator =
                                coordinates.lastIndexOf(':');

                        if (secondSeparator <= 0) {
                            continue;
                        }

                        String world =
                                coordinates.substring(
                                        0,
                                        secondSeparator
                                );

                        String chunkXString =
                                coordinates.substring(
                                        secondSeparator + 1
                                );

                        String chunkZString =
                                key.substring(
                                        lastSeparator + 1
                                );

                        int chunkX;
                        int chunkZ;

                        try {

                            chunkX =
                                    Integer.parseInt(
                                            chunkXString
                                    );

                            chunkZ =
                                    Integer.parseInt(
                                            chunkZString
                                    );

                        } catch (NumberFormatException e) {
                            continue;
                        }

                        String faction =
                                entry.getValue();

                        if (faction == null
                                || faction.isBlank()) {
                            continue;
                        }

                        insert.setString(
                                1,
                                world
                        );

                        insert.setInt(
                                2,
                                chunkX
                        );

                        insert.setInt(
                                3,
                                chunkZ
                        );

                        insert.setString(
                                4,
                                faction.toLowerCase()
                        );

                        insert.addBatch();
                    }

                    insert.executeBatch();
                }

                connection.commit();

            } catch (SQLException e) {

                connection.rollback();
                throw e;

            } finally {

                connection.setAutoCommit(
                        previousAutoCommit
                );
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error guardando los claims en SQLite."
            );

            e.printStackTrace();
        }
    }
}