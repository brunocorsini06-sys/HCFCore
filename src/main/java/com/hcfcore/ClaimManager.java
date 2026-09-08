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
    /**
     * Formato:
     * world:chunkX:chunkZ -> faction
     */
    private final Map<String, String> claims = new HashMap<>();
    public ClaimManager(HCFCore plugin) {
        this.plugin = plugin;
        loadAll();
    }
    /**
     * Genera una clave única para un chunk.
     */
    private String getKey(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) {
            return "";
        }
        return chunk.getWorld().getName()
                + ":"
                + chunk.getX()
                + ":"
                + chunk.getZ();
    }
    /**
     * Obtiene la faction propietaria de una ubicación.
     */
    public Faction getFactionAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Chunk chunk = location.getChunk();
        String factionName = claims.get(getKey(chunk));
        if (factionName == null || factionName.isBlank()) {
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
        // Wilderness
        if (faction == null) {
            return true;
        }
        // Staff bypass
        if (player.hasPermission("hcf.bypass")) {
            return true;
        }
        Faction playerFaction =
                plugin.getFactionManager()
                        .getFaction(player);
        // Jugador sin faction
        if (playerFaction == null) {
            return false;
        }
        return playerFaction.getName()
                .equalsIgnoreCase(faction.getName());
    }
    /**
     * Comprueba si el jugador tiene permiso para administrar claims.
     *
     * Actualmente:
     * LEADER
     * CO-LEADER
     */
    private boolean canManageClaims(Player player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermission("hcf.bypass")) {
            return true;
        }
        return plugin.getFactionManager()
                .hasPermission(
                        player,
                        Faction.FactionPermission.MANAGE_CLAIMS
                );
    }
    /**
     * Reclama el chunk donde está el jugador.
     */
    public boolean claim(Player player) {
        if (player == null) {
            return false;
        }
        FactionManager factionManager =
                plugin.getFactionManager();
        Faction faction =
                factionManager.getFaction(player);
        if (faction == null) {
            return false;
        }
        // Solo rangos autorizados
        if (!canManageClaims(player)) {
            return false;
        }
        Chunk chunk =
                player.getLocation().getChunk();
        String key = getKey(chunk);
        if (key.isEmpty()) {
            return false;
        }
        // Ya existe un claim en este chunk
        if (claims.containsKey(key)) {
            return false;
        }
        int maxClaims =
                plugin.getConfig()
                        .getInt(
                                "factions.max-claim-chunks",
                                20
                        );
        if (maxClaims < 1) {
            maxClaims = 1;
        }
        int currentClaims =
                getClaimCount(faction);
        if (currentClaims >= maxClaims) {
            return false;
        }
        /*
         * Los claims nuevos deben tocar
         * territorio existente de la faction.
         */
        boolean requireAdjacent =
                plugin.getConfig()
                        .getBoolean(
                                "factions.require-adjacent-claims",
                                true
                        );
        if (requireAdjacent
                && currentClaims > 0
                && !hasAdjacentClaim(faction, chunk)) {
            return false;
        }
        claims.put(
                key,
                faction.getName().toLowerCase()
        );
        saveAll();
        return true;
    }
    /**
     * Abandona el claim donde está el jugador.
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
        if (!canManageClaims(player)) {
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
        /*
         * Evita dejar un territorio
         * dividido si hay más de un claim.
         *
         * Se puede desactivar mediante config.
         */
        boolean preventSplitting =
                plugin.getConfig()
                        .getBoolean(
                                "factions.prevent-split-claims",
                                false
                        );
        if (preventSplitting
                && getClaimCount(faction) > 1
                && wouldSplitClaims(faction, chunk)) {
            return false;
        }
        claims.remove(key);
        saveAll();
        return true;
    }
    /**
     * Cuenta los claims de una faction.
     */
    public int getClaimCount(Faction faction) {
        if (faction == null) {
            return 0;
        }
        String factionName =
                faction.getName();
        int count = 0;
        for (String owner : claims.values()) {
            if (owner != null
                    && owner.equalsIgnoreCase(factionName)) {
                count++;
            }
        }
        return count;
    }
    /**
     * Comprueba si el chunk toca
     * territorio de la misma faction.
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
     * Comprueba si quitar un claim
     * separaría el territorio.
     */
    private boolean wouldSplitClaims(
            Faction faction,
            Chunk removedChunk
    ) {
        if (faction == null || removedChunk == null) {
            return false;
        }
        String removedKey =
                getKey(removedChunk);
        String originalOwner =
                claims.remove(removedKey);
        if (originalOwner == null) {
            return false;
        }
        try {
            int remaining = getClaimCount(faction);
            if (remaining <= 1) {
                return false;
            }
            /*
             * Buscar un claim cualquiera
             * para comenzar el flood fill.
             */
            Chunk start = null;
            for (Map.Entry<String, String> entry : claims.entrySet()) {
                if (!entry.getValue()
                        .equalsIgnoreCase(faction.getName())) {
                    continue;
                }
                Chunk parsed =
                        parseChunkKey(entry.getKey());
                if (parsed != null) {
                    start = parsed;
                    break;
                }
            }
            if (start == null) {
                return false;
            }
            java.util.Set<String> visited =
                    new java.util.HashSet<>();
            java.util.ArrayDeque<Chunk> queue =
                    new java.util.ArrayDeque<>();
            queue.add(start);
            visited.add(getKey(start));
            while (!queue.isEmpty()) {
                Chunk current = queue.poll();
                int x = current.getX();
                int z = current.getZ();
                Chunk[] neighbors = {
                        current.getWorld().getChunkAt(x + 1, z),
                        current.getWorld().getChunkAt(x - 1, z),
                        current.getWorld().getChunkAt(x, z + 1),
                        current.getWorld().getChunkAt(x, z - 1)
                };
                for (Chunk neighbor : neighbors) {
                    String neighborKey =
                            getKey(neighbor);
                    if (visited.contains(neighborKey)) {
                        continue;
                    }
                    if (!ownsChunk(faction, neighbor)) {
                        continue;
                    }
                    visited.add(neighborKey);
                    queue.add(neighbor);
                }
            }
            /*
             * Si no visitamos todos los claims,
             * el territorio se divide.
             */
            return visited.size() < remaining;
        } finally {
            claims.put(
                    removedKey,
                    originalOwner
            );
        }
    }
    /**
     * Convierte:
     *
     * world:x:z
     *
     * en Chunk.
     */
    private Chunk parseChunkKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            int lastSeparator =
                    key.lastIndexOf(':');
            if (lastSeparator <= 0) {
                return null;
            }
            String coordinates =
                    key.substring(
                            0,
                            lastSeparator
                    );
            String chunkZString =
                    key.substring(
                            lastSeparator + 1
                    );
            int secondSeparator =
                    coordinates.lastIndexOf(':');
            if (secondSeparator <= 0) {
                return null;
            }
            String worldName =
                    coordinates.substring(
                            0,
                            secondSeparator
                    );
            String chunkXString =
                    coordinates.substring(
                            secondSeparator + 1
                    );
            int chunkX =
                    Integer.parseInt(chunkXString);
            int chunkZ =
                    Integer.parseInt(chunkZString);
            org.bukkit.World world =
                    plugin.getServer()
                            .getWorld(worldName);
            if (world == null) {
                return null;
            }
            return world.getChunkAt(
                    chunkX,
                    chunkZ
            );
        } catch (Exception ignored) {
            return null;
        }
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
     * Obtiene el nombre de la faction
     * propietaria de la ubicación.
     *
     * Wilderness si no hay claim.
     */
    public String getClaimName(Player player) {
        if (player == null) {
            return "Wilderness";
        }
        Faction faction =
                getFactionAt(
                        player.getLocation()
                );
        if (faction == null) {
            return "Wilderness";
        }
        return faction.getName();
    }
    /**
     * Elimina todos los claims
     * de una faction.
     *
     * Se utiliza al hacer /f disband.
     */
    public void removeFactionClaims(
            Faction faction
    ) {
        if (faction == null) {
            return;
        }
        String name =
                faction.getName();
        boolean removed =
                claims.entrySet()
                        .removeIf(entry ->
                                entry.getValue() != null
                                        && entry.getValue()
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
        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result =
                        statement.executeQuery()
        ) {
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
                        || world.isBlank()
                        || faction == null
                        || faction.isBlank()) {
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
                try (
                        PreparedStatement delete =
                                connection.prepareStatement(
                                        deleteSql
                                )
                ) {
                    delete.executeUpdate();
                }
                try (
                        PreparedStatement insert =
                                connection.prepareStatement(
                                        insertSql
                                )
                ) {
                    for (
                            Map.Entry<String, String> entry
                            : claims.entrySet()
                    ) {
                        String key =
                                entry.getKey();
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