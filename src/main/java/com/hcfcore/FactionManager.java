package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class FactionManager {

    private final HCFCore plugin;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();

    /*
     * Estadísticas individuales.
     */
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    /*
     * Última regeneración de DTR.
     */
    private final Map<String, Long> lastDtrRegeneration = new HashMap<>();

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;

        createPersistenceTables();
        loadAll();
    }

    // =========================================================
    // CREAR FACTION
    // =========================================================

    public Faction createFaction(Player player, String name) {

        if (player == null || name == null) {
            return null;
        }

        name = name.trim();

        if (!isValidFactionName(name)) {
            return null;
        }

        if (getFaction(player) != null) {
            return null;
        }

        String key = normalize(name);

        if (factions.containsKey(key)) {
            return null;
        }

        double startingDtr = getStartingDtr();

        Faction faction = new Faction(
                name,
                player.getUniqueId(),
                startingDtr
        );

        factions.put(key, faction);

        playerFactions.put(
                player.getUniqueId(),
                key
        );

        lastDtrRegeneration.put(
                key,
                System.currentTimeMillis()
        );

        saveAll();

        return faction;
    }

    // =========================================================
    // VALIDAR NOMBRE
    // =========================================================

    public boolean isValidFactionName(String name) {

        if (name == null) {
            return false;
        }

        name = name.trim();

        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        return name.matches("[A-Za-z0-9_]+");
    }

    // =========================================================
    // DISBAND
    // =========================================================

    public boolean disband(Faction faction) {

        if (faction == null) {
            return false;
        }

        String key = normalize(faction.getName());

        for (UUID uuid : new HashSet<>(faction.getMembers())) {
            playerFactions.remove(uuid);
        }

        plugin.getClaimManager()
                .removeFactionClaims(faction);

        factions.remove(key);

        lastDtrRegeneration.remove(key);

        invites.entrySet().removeIf(
                entry -> entry.getValue().equalsIgnoreCase(key)
        );

        saveAll();

        return true;
    }

    // =========================================================
    // OBTENER FACTION
    // =========================================================

    public Faction getFaction(Player player) {

        if (player == null) {
            return null;
        }

        return getFaction(player.getUniqueId());
    }

    public Faction getFaction(UUID uuid) {

        if (uuid == null) {
            return null;
        }

        String factionName =
                playerFactions.get(uuid);

        if (factionName == null) {
            return null;
        }

        return factions.get(
                normalize(factionName)
        );
    }

    public Faction getFaction(String name) {

        if (name == null) {
            return null;
        }

        return factions.get(
                normalize(name)
        );
    }

    // =========================================================
    // UNIRSE
    // =========================================================

    public boolean joinFaction(
            Player player,
            String name
    ) {

        if (player == null || name == null) {
            return false;
        }

        Faction faction =
                getFaction(name);

        if (faction == null) {
            return false;
        }

        if (getFaction(player) != null) {
            return false;
        }

        if (!hasInvite(
                player,
                faction.getName()
        )) {
            return false;
        }

        int maxMembers =
                plugin.getConfig()
                        .getInt(
                                "factions.max-members",
                                20
                        );

        if (maxMembers < 1) {
            maxMembers = 20;
        }

        if (faction.getMembers().size()
                >= maxMembers) {

            return false;
        }

        faction.addMember(
                player.getUniqueId()
        );

        playerFactions.put(
                player.getUniqueId(),
                normalize(faction.getName())
        );

        removeInvite(player);

        saveAll();

        return true;
    }

    // =========================================================
    // SALIR
    // =========================================================

    public boolean leaveFaction(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        if (faction == null) {
            return false;
        }

        if (faction.isLeader(
                player.getUniqueId()
        )) {

            return false;
        }

        faction.removeMember(
                player.getUniqueId()
        );

        playerFactions.remove(
                player.getUniqueId()
        );

        saveAll();

        return true;
    }

    // =========================================================
    // INVITAR
    // =========================================================

    public boolean invite(
            Player target,
            Faction faction
    ) {

        if (target == null || faction == null) {
            return false;
        }

        if (getFaction(target) != null) {
            return false;
        }

        int maxMembers =
                plugin.getConfig()
                        .getInt(
                                "factions.max-members",
                                20
                        );

        if (faction.getMembers().size()
                >= maxMembers) {

            return false;
        }

        invites.put(
                target.getUniqueId(),
                normalize(faction.getName())
        );

        return true;
    }

    public boolean hasInvite(
            Player player,
            String factionName
    ) {

        if (player == null || factionName == null) {
            return false;
        }

        String invite =
                invites.get(
                        player.getUniqueId()
                );

        return invite != null
                && invite.equalsIgnoreCase(
                normalize(factionName)
        );
    }

    public void removeInvite(Player player) {

        if (player == null) {
            return;
        }

        invites.remove(
                player.getUniqueId()
        );
    }

    public String getInvite(Player player) {

        if (player == null) {
            return null;
        }

        return invites.get(
                player.getUniqueId()
        );
    }

    // =========================================================
    // KICK
    // =========================================================

    public boolean kick(
            Faction faction,
            UUID target
    ) {

        if (faction == null || target == null) {
            return false;
        }

        if (!faction.isMember(target)) {
            return false;
        }

        if (faction.isLeader(target)) {
            return false;
        }

        faction.removeMember(target);

        playerFactions.remove(target);

        saveAll();

        return true;
    }

    // =========================================================
    // RANKS
    // =========================================================

    public boolean promote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null || uuid == null) {
            return false;
        }

        if (!faction.isMember(uuid)) {
            return false;
        }

        if (faction.isLeader(uuid)) {
            return false;
        }

        if (faction.isOfficer(uuid)) {
            return false;
        }

        faction.promote(uuid);

        saveAll();

        return true;
    }

    public boolean demote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null || uuid == null) {
            return false;
        }

        if (!faction.isOfficer(uuid)) {
            return false;
        }

        faction.demote(uuid);

        saveAll();

        return true;
    }

    // =========================================================
    // RANGOS
    // =========================================================

    public boolean isLeader(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        return faction != null
                && faction.isLeader(
                player.getUniqueId()
        );
    }

    public boolean isOfficer(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        return faction != null
                && faction.isOfficer(
                player.getUniqueId()
        );
    }

    public boolean canManageFaction(Player player) {

        return isLeader(player)
                || isOfficer(player);
    }

    // =========================================================
    // FACTION HOME
    // =========================================================

    public boolean setHome(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        if (faction == null) {
            return false;
        }

        if (!isLeader(player)
                && !isOfficer(player)) {

            return false;
        }

        Location location =
                player.getLocation();

        Faction claimedFaction =
                plugin.getClaimManager()
                        .getFactionAt(location);

        if (claimedFaction == null) {
            return false;
        }

        if (!claimedFaction.getName()
                .equalsIgnoreCase(
                        faction.getName()
                )) {

            return false;
        }

        faction.setHome(location);

        saveAll();

        return true;
    }

    public boolean hasHome(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        return faction != null
                && faction.hasHome();
    }

    public Location getHome(Player player) {

        if (player == null) {
            return null;
        }

        Faction faction =
                getFaction(player);

        if (faction == null) {
            return null;
        }

        return faction.getHome();
    }

    // =========================================================
    // ALLIES
    // =========================================================

    public boolean addAlly(
            Faction faction,
            Faction target
    ) {

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        faction.addAlly(
                target.getName()
        );

        target.addAlly(
                faction.getName()
        );

        saveAll();

        return true;
    }

    public boolean removeAlly(
            Faction faction,
            Faction target
    ) {

        if (faction == null || target == null) {
            return false;
        }

        faction.removeAlly(
                target.getName()
        );

        target.removeAlly(
                faction.getName()
        );

        saveAll();

        return true;
    }

    // =========================================================
    // ENEMIES
    // =========================================================

    public boolean addEnemy(
            Faction faction,
            Faction target
    ) {

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        faction.addEnemy(
                target.getName()
        );

        target.addEnemy(
                faction.getName()
        );

        saveAll();

        return true;
    }

    public boolean removeEnemy(
            Faction faction,
            Faction target
    ) {

        if (faction == null || target == null) {
            return false;
        }

        faction.removeEnemy(
                target.getName()
        );

        target.removeEnemy(
                faction.getName()
        );

        saveAll();

        return true;
    }

    // =========================================================
    // KILLS
    // =========================================================

    public void addKill(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        kills.put(
                uuid,
                getKills(player) + 1
        );

        savePlayerStats(uuid);
    }

    public void addDeath(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        deaths.put(
                uuid,
                getDeaths(player) + 1
        );

        savePlayerStats(uuid);
    }

    public int getKills(Player player) {

        if (player == null) {
            return 0;
        }

        return kills.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    public int getDeaths(Player player) {

        if (player == null) {
            return 0;
        }

        return deaths.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    // =========================================================
    // MUERTE / PÉRDIDA DE DTR
    // =========================================================

    public boolean handleDeath(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        if (faction == null) {
            return false;
        }

        /*
         * Una faction ya raidable
         * no pierde más DTR.
         */
        if (faction.isRaidable()) {
            return false;
        }

        double loss =
                plugin.getConfig()
                        .getDouble(
                                "factions.dtr-loss-on-death",
                                1.0
                        );

        if (Double.isNaN(loss)
                || Double.isInfinite(loss)
                || loss <= 0.0) {

            return false;
        }

        double oldDtr =
                faction.getDtr();

        faction.removeDtr(loss);

        saveAll();

        return faction.getDtr() < oldDtr;
    }

    public boolean isRaidable(Faction faction) {

        return faction != null
                && faction.isRaidable();
    }

    public boolean isRaidable(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        return isRaidable(faction);
    }

    // =========================================================
    // DTR - CONFIGURACIÓN
    // =========================================================

    public double getMaxDtr() {

        double maxDtr =
                plugin.getConfig()
                        .getDouble(
                                "factions.max-dtr",
                                5.0
                        );

        if (Double.isNaN(maxDtr)
                || Double.isInfinite(maxDtr)
                || maxDtr <= 0.0) {

            return 5.0;
        }

        return maxDtr;
    }

    public double getStartingDtr() {

        double startingDtr =
                plugin.getConfig()
                        .getDouble(
                                "factions.starting-dtr",
                                1.0
                        );

        if (Double.isNaN(startingDtr)
                || Double.isInfinite(startingDtr)
                || startingDtr < 0.0) {

            return 1.0;
        }

        return Math.min(
                startingDtr,
                getMaxDtr()
        );
    }

    public double getDtrLossOnDeath() {

        double loss =
                plugin.getConfig()
                        .getDouble(
                                "factions.dtr-loss-on-death",
                                1.0
                        );

        if (Double.isNaN(loss)
                || Double.isInfinite(loss)
                || loss < 0.0) {

            return 1.0;
        }

        return loss;
    }

    // =========================================================
    // DTR - REGENERACIÓN
    // =========================================================

    public void regenerateDtr() {

        if (!plugin.getConfig().getBoolean(
                "factions.dtr-regeneration",
                true
        )) {
            return;
        }

        int regenerationMinutes =
                plugin.getConfig()
                        .getInt(
                                "factions.dtr-regeneration-minutes",
                                10
                        );

        if (regenerationMinutes <= 0) {
            regenerationMinutes = 10;
        }

        long regenerationInterval =
                regenerationMinutes * 60L * 1000L;

        double regenerationAmount =
                plugin.getConfig()
                        .getDouble(
                                "factions.dtr-regeneration-amount",
                                0.05
                        );

        if (Double.isNaN(regenerationAmount)
                || Double.isInfinite(regenerationAmount)
                || regenerationAmount <= 0.0) {

            regenerationAmount = 0.05;
        }

        double maxDtr =
                getMaxDtr();

        long now =
                System.currentTimeMillis();

        for (Faction faction :
                new ArrayList<>(factions.values())) {

            if (faction == null) {
                continue;
            }

            if (faction.isRaidable()) {
                continue;
            }

            String key =
                    normalize(faction.getName());

            long last =
                    lastDtrRegeneration
                            .getOrDefault(
                                    key,
                                    now
                            );

            if (now - last <
                    regenerationInterval) {

                continue;
            }

            lastDtrRegeneration.put(
                    key,
                    now
            );

            double currentDtr =
                    faction.getDtr();

            if (currentDtr >= maxDtr) {
                continue;
            }

            double newDtr =
                    Math.min(
                            maxDtr,
                            currentDtr
                                    + regenerationAmount
                    );

            faction.setDtr(newDtr);
        }
    }

    // =========================================================
    // DTR MANUAL
    // =========================================================

    public void setDtr(
            Faction faction,
            double dtr
    ) {

        if (faction == null) {
            return;
        }

        if (Double.isNaN(dtr)
                || Double.isInfinite(dtr)) {

            return;
        }

        double maxDtr =
                getMaxDtr();

        dtr = Math.max(
                0.0,
                Math.min(dtr, maxDtr)
        );

        faction.setDtr(dtr);

        saveAll();
    }

    public void addDtr(
            Faction faction,
            double amount
    ) {

        if (faction == null) {
            return;
        }

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {

            return;
        }

        double maxDtr =
                getMaxDtr();

        double newDtr =
                Math.min(
                        maxDtr,
                        faction.getDtr() + amount
                );

        faction.setDtr(newDtr);

        if (faction.getDtr() > 0.0) {

            lastDtrRegeneration.put(
                    normalize(faction.getName()),
                    System.currentTimeMillis()
            );
        }

        saveAll();
    }

    public void removeDtr(
            Faction faction,
            double amount
    ) {

        if (faction == null) {
            return;
        }

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {

            return;
        }

        faction.removeDtr(amount);

        saveAll();
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    private String normalize(String text) {

        return text
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public Collection<Faction> getFactions() {
        return factions.values();
    }

    public Map<String, Faction> getFactionMap() {
        return factions;
    }

    public Map<UUID, String> getPlayerFactions() {
        return playerFactions;
    }

    public Map<UUID, String> getInvites() {
        return invites;
    }

    public Map<UUID, Integer> getKillsMap() {
        return kills;
    }

    public Map<UUID, Integer> getDeathsMap() {
        return deaths;
    }

    // =========================================================
    // SQLITE - TABLAS
    // =========================================================

    private void createPersistenceTables() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            plugin.getLogger().warning(
                    "SQLite no está disponible. " +
                    "FactionManager funcionará sin persistencia."
            );

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        try (Statement statement =
                     connection.createStatement()) {

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

            plugin.getLogger().info(
                    "Tablas de persistencia de Factions verificadas."
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error creando tablas de Factions."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // SQLITE - CARGAR TODO
    // =========================================================

    public void loadAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        factions.clear();
        playerFactions.clear();
        kills.clear();
        deaths.clear();
        lastDtrRegeneration.clear();

        loadFactions(connection);
        loadFactionMembers(connection);
        loadFactionRelations(connection);
        loadFactionHomes(connection);
        loadPlayerStats(connection);

        plugin.getLogger().info(
                "Factions cargadas desde SQLite: "
                        + factions.size()
        );
    }

    // =========================================================
    // CARGAR FACTIONS
    // =========================================================

    private void loadFactions(Connection connection) {

        String sql = """
                SELECT name, leader, dtr, balance
                FROM factions
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                String name =
                        result.getString("name");

                String leaderString =
                        result.getString("leader");

                if (name == null
                        || leaderString == null) {
                    continue;
                }

                UUID leader;

                try {
                    leader =
                            UUID.fromString(
                                    leaderString
                            );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                double dtr =
                        result.getDouble("dtr");

                double balance =
                        result.getDouble("balance");

                Faction faction =
                        new Faction(
                                name,
                                leader,
                                Math.max(
                                        0.0,
                                        Math.min(
                                                dtr,
                                                getMaxDtr()
                                        )
                                )
                        );

                faction.setBalance(
                        Math.max(
                                0.0,
                                balance
                        )
                );

                String key =
                        normalize(name);

                factions.put(
                        key,
                        faction
                );

                playerFactions.put(
                        leader,
                        key
                );

                lastDtrRegeneration.put(
                        key,
                        System.currentTimeMillis()
                );
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando Factions desde SQLite."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CARGAR MIEMBROS
    // =========================================================

    private void loadFactionMembers(
            Connection connection
    ) {

        String sql = """
                SELECT faction, uuid, role
                FROM faction_members
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                String factionName =
                        result.getString("faction");

                String uuidString =
                        result.getString("uuid");

                String role =
                        result.getString("role");

                Faction faction =
                        getFaction(factionName);

                if (faction == null
                        || uuidString == null) {
                    continue;
                }

                UUID uuid;

                try {
                    uuid =
                            UUID.fromString(
                                    uuidString
                            );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                faction.addMember(uuid);

                playerFactions.put(
                        uuid,
                        normalize(faction.getName())
                );

                if ("OFFICER".equalsIgnoreCase(role)) {
                    faction.promote(uuid);
                }
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando miembros de Factions."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CARGAR ALLIES / ENEMIES
    // =========================================================

    private void loadFactionRelations(
            Connection connection
    ) {

        String alliesSql = """
                SELECT faction, target
                FROM faction_allies
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(alliesSql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                Faction faction =
                        getFaction(
                                result.getString("faction")
                        );

                String target =
                        result.getString("target");

                if (faction != null
                        && target != null) {

                    faction.addAlly(target);
                }
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando aliados."
            );

            e.printStackTrace();
        }

        String enemiesSql = """
                SELECT faction, target
                FROM faction_enemies
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(enemiesSql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                Faction faction =
                        getFaction(
                                result.getString("faction")
                        );

                String target =
                        result.getString("target");

                if (faction != null
                        && target != null) {

                    faction.addEnemy(target);
                }
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando enemigos."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CARGAR HOMES
    // =========================================================

    private void loadFactionHomes(
            Connection connection
    ) {

        String sql = """
                SELECT faction, world, x, y, z, yaw, pitch
                FROM faction_homes
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                Faction faction =
                        getFaction(
                                result.getString("faction")
                        );

                if (faction == null) {
                    continue;
                }

                String worldName =
                        result.getString("world");

                World world =
                        Bukkit.getWorld(worldName);

                if (world == null) {
                    plugin.getLogger().warning(
                            "No se pudo cargar el home de "
                                    + faction.getName()
                                    + ": mundo no encontrado."
                    );
                    continue;
                }

                Location location =
                        new Location(
                                world,
                                result.getDouble("x"),
                                result.getDouble("y"),
                                result.getDouble("z"),
                                result.getFloat("yaw"),
                                result.getFloat("pitch")
                        );

                faction.setHome(location);
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando homes de Factions."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CARGAR STATS
    // =========================================================

    private void loadPlayerStats(
            Connection connection
    ) {

        String sql = """
                SELECT uuid, kills, deaths
                FROM player_stats
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                String uuidString =
                        result.getString("uuid");

                if (uuidString == null) {
                    continue;
                }

                UUID uuid;

                try {
                    uuid =
                            UUID.fromString(
                                    uuidString
                            );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                kills.put(
                        uuid,
                        Math.max(
                                0,
                                result.getInt("kills")
                        )
                );

                deaths.put(
                        uuid,
                        Math.max(
                                0,
                                result.getInt("deaths")
                        )
                );
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando estadísticas."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // SQLITE - GUARDAR TODO
    // =========================================================

    public void saveAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        try {

            boolean previousAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {

                clearFactionData(connection);

                saveFactions(connection);
                saveMembers(connection);
                saveRelations(connection);
                saveHomes(connection);
                saveAllPlayerStats(connection);

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
                    "Error guardando Factions en SQLite."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // LIMPIAR DATOS ANTES DE GUARDAR
    // =========================================================

    private void clearFactionData(
            Connection connection
    ) throws SQLException {

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(
                    "DELETE FROM faction_members"
            );

            statement.executeUpdate(
                    "DELETE FROM faction_allies"
            );

            statement.executeUpdate(
                    "DELETE FROM faction_enemies"
            );

            statement.executeUpdate(
                    "DELETE FROM faction_homes"
            );

            statement.executeUpdate(
                    "DELETE FROM factions"
            );
        }
    }

    // =========================================================
    // GUARDAR FACTIONS
    // =========================================================

    private void saveFactions(
            Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO factions
                (name, leader, dtr, balance)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (Faction faction :
                    factions.values()) {

                if (faction == null
                        || faction.getLeader() == null) {
                    continue;
                }

                statement.setString(
                        1,
                        faction.getName()
                );

                statement.setString(
                        2,
                        faction.getLeader().toString()
                );

                statement.setDouble(
                        3,
                        faction.getDtr()
                );

                statement.setDouble(
                        4,
                        faction.getBalance()
                );

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // GUARDAR MIEMBROS
    // =========================================================

    private void saveMembers(
            Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO faction_members
                (faction, uuid, role)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (Faction faction :
                    factions.values()) {

                if (faction == null) {
                    continue;
                }

                for (UUID uuid :
                        faction.getMembers()) {

                    if (uuid == null) {
                        continue;
                    }

                    String role;

                    if (faction.isLeader(uuid)) {
                        role = "LEADER";
                    } else if (faction.isOfficer(uuid)) {
                        role = "OFFICER";
                    } else {
                        role = "MEMBER";
                    }

                    statement.setString(
                            1,
                            normalize(
                                    faction.getName()
                            )
                    );

                    statement.setString(
                            2,
                            uuid.toString()
                    );

                    statement.setString(
                            3,
                            role
                    );

                    statement.addBatch();
                }
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // GUARDAR ALLIES / ENEMIES
    // =========================================================

    private void saveRelations(
            Connection connection
    ) throws SQLException {

        String allySql = """
                INSERT INTO faction_allies
                (faction, target)
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(allySql)) {

            for (Faction faction :
                    factions.values()) {

                if (faction == null) {
                    continue;
                }

                for (String ally :
                        faction.getAllies()) {

                    if (ally == null
                            || ally.isBlank()) {
                        continue;
                    }

                    statement.setString(
                            1,
                            normalize(
                                    faction.getName()
                            )
                    );

                    statement.setString(
                            2,
                            normalize(ally)
                    );

                    statement.addBatch();
                }
            }

            statement.executeBatch();
        }

        String enemySql = """
                INSERT INTO faction_enemies
                (faction, target)
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(enemySql)) {

            for (Faction faction :
                    factions.values()) {

                if (faction == null) {
                    continue;
                }

                for (String enemy :
                        faction.getEnemies()) {

                    if (enemy == null
                            || enemy.isBlank()) {
                        continue;
                    }

                    statement.setString(
                            1,
                            normalize(
                                    faction.getName()
                            )
                    );

                    statement.setString(
                            2,
                            normalize(enemy)
                    );

                    statement.addBatch();
                }
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // GUARDAR HOMES
    // =========================================================

    private void saveHomes(
            Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO faction_homes
                (faction, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (Faction faction :
                    factions.values()) {

                if (faction == null
                        || !faction.hasHome()) {
                    continue;
                }

                Location home =
                        faction.getHome();

                if (home == null
                        || home.getWorld() == null) {
                    continue;
                }

                statement.setString(
                        1,
                        normalize(
                                faction.getName()
                        )
                );

                statement.setString(
                        2,
                        home.getWorld().getName()
                );

                statement.setDouble(
                        3,
                        home.getX()
                );

                statement.setDouble(
                        4,
                        home.getY()
                );

                statement.setDouble(
                        5,
                        home.getZ()
                );

                statement.setFloat(
                        6,
                        home.getYaw()
                );

                statement.setFloat(
                        7,
                        home.getPitch()
                );

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // GUARDAR STATS
    // =========================================================

    private void saveAllPlayerStats(
            Connection connection
    ) throws SQLException {

        Set<UUID> players =
                new HashSet<>();

        players.addAll(kills.keySet());
        players.addAll(deaths.keySet());

        String sql = """
                INSERT INTO player_stats
                (uuid, kills, deaths)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET
                    kills = excluded.kills,
                    deaths = excluded.deaths
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (UUID uuid : players) {

                if (uuid == null) {
                    continue;
                }

                statement.setString(
                        1,
                        uuid.toString()
                );

                statement.setInt(
                        2,
                        kills.getOrDefault(
                                uuid,
                                0
                        )
                );

                statement.setInt(
                        3,
                        deaths.getOrDefault(
                                uuid,
                                0
                        )
                );

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // GUARDAR STATS DE UN JUGADOR
    // =========================================================

    private void savePlayerStats(UUID uuid) {

        if (uuid == null) {
            return;
        }

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {
            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        String sql = """
                INSERT INTO player_stats
                (uuid, kills, deaths)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET
                    kills = excluded.kills,
                    deaths = excluded.deaths
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setInt(
                    2,
                    kills.getOrDefault(
                            uuid,
                            0
                    )
            );

            statement.setInt(
                    3,
                    deaths.getOrDefault(
                            uuid,
                            0
                    )
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudieron guardar estadísticas de "
                            + uuid
            );
        }
    }
}