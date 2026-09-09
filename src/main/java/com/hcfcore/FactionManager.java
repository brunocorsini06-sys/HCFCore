package com.hcfcore;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class FactionManager {

    private final HCFCore plugin;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();
    private final Map<UUID, Map<String, Long>> invites = new HashMap<>();

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    // =========================================================
    // FACTIONS
    // =========================================================

    public Map<String, Faction> getFactions() {
        return Collections.unmodifiableMap(factions);
    }

    public Faction getFaction(String name) {
        if (name == null) return null;
        return factions.get(name.toLowerCase(Locale.ROOT));
    }

    public Faction getFaction(Player player) {
        return player == null ? null : getFaction(player.getUniqueId());
    }

    public Faction getFaction(UUID uuid) {
        if (uuid == null) return null;

        String name = playerFactions.get(uuid);
        if (name == null) return null;

        return getFaction(name);
    }

    public String getFactionName(UUID uuid) {
        Faction faction = getFaction(uuid);
        return faction == null ? null : faction.getName();
    }

    public boolean hasFaction(Player player) {
        return player != null && hasFaction(player.getUniqueId());
    }

    public boolean hasFaction(UUID uuid) {
        return getFaction(uuid) != null;
    }

    public int getFactionCount() {
        return factions.size();
    }

    public boolean isValidFactionName(String name) {
        if (name == null) return false;

        name = name.trim();

        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        if (!name.matches("[A-Za-z0-9_]+")) {
            return false;
        }

        return getFaction(name) == null;
    }

    // =========================================================
    // CREATE / DISBAND
    // =========================================================

    public Faction createFaction(Player player, String name) {
        if (player == null || name == null) {
            return null;
        }

        name = name.trim();

        if (!isValidFactionName(name)) {
            return null;
        }

        if (hasFaction(player)) {
            return null;
        }

        double startingDtr = plugin.getConfig()
                .getDouble("factions.starting-dtr", 1.0);

        double maxDtr = plugin.getConfig()
                .getDouble("factions.max-dtr", 5.0);

        Faction faction = new Faction(
                name,
                player.getUniqueId(),
                startingDtr,
                maxDtr
        );

        factions.put(name.toLowerCase(Locale.ROOT), faction);
        playerFactions.put(player.getUniqueId(), name);

        if (!saveFaction(faction)) {
            factions.remove(name.toLowerCase(Locale.ROOT));
            playerFactions.remove(player.getUniqueId());
            return null;
        }

        return faction;
    }

    public boolean disband(Faction faction) {
        if (faction == null) {
            return false;
        }

        String key = faction.getName().toLowerCase(Locale.ROOT);

        factions.remove(key);

        for (UUID uuid : new HashSet<>(faction.getMembers())) {
            playerFactions.remove(uuid);
            invites.remove(uuid);
        }

        // Primero eliminar claims para respetar las FK de SQLite.
        plugin.getClaimManager().removeFactionClaims(faction);

        DatabaseManager db = plugin.getDatabaseManager();

        if (db != null && db.isConnected()) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM factions WHERE name = ?")) {

                ps.setString(1, faction.getName());
                ps.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().warning(
                        "No se pudo eliminar la faction " + faction.getName()
                                + ": " + e.getMessage()
                );
                return false;
            }
        }

        return true;
    }

    public boolean disband(Player player) {
        Faction faction = getFaction(player);
        if (faction == null || !faction.isLeader(player.getUniqueId())) {
            return false;
        }

        return disband(faction);
    }

    // =========================================================
    // INVITES
    // =========================================================

    public boolean invite(Player target, Faction faction) {
        if (target == null || faction == null) {
            return false;
        }

        if (hasFaction(target)) {
            return false;
        }

        long seconds = plugin.getConfig()
                .getLong("factions.invite-expire-seconds", 60);

        long expires = System.currentTimeMillis()
                + (seconds * 1000L);

        invites
                .computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(faction.getName().toLowerCase(Locale.ROOT), expires);

        return true;
    }

    public boolean hasInvite(Player player, String factionName) {
        if (player == null || factionName == null) {
            return false;
        }

        Map<String, Long> playerInvites =
                invites.get(player.getUniqueId());

        if (playerInvites == null) {
            return false;
        }

        String key = factionName.toLowerCase(Locale.ROOT);
        Long expires = playerInvites.get(key);

        if (expires == null) {
            return false;
        }

        if (expires < System.currentTimeMillis()) {
            playerInvites.remove(key);
            return false;
        }

        return true;
    }

    public void removeInvite(Player player, String factionName) {
        if (player == null || factionName == null) {
            return;
        }

        Map<String, Long> playerInvites =
                invites.get(player.getUniqueId());

        if (playerInvites == null) {
            return;
        }

        playerInvites.remove(factionName.toLowerCase(Locale.ROOT));

        if (playerInvites.isEmpty()) {
            invites.remove(player.getUniqueId());
        }
    }

    public Set<String> getInvites(Player player) {
        if (player == null) {
            return Collections.emptySet();
        }

        Map<String, Long> map = invites.get(player.getUniqueId());

        if (map == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(map.keySet());
    }

    // =========================================================
    // JOIN / LEAVE
    // =========================================================

    public boolean joinFaction(Player player, String factionName) {
        if (player == null || factionName == null) {
            return false;
        }

        if (hasFaction(player)) {
            return false;
        }

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        if (!hasInvite(player, faction.getName())) {
            return false;
        }

        int maxMembers = plugin.getConfig()
                .getInt("factions.max-members", 20);

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        if (!faction.addMember(player.getUniqueId())) {
            return false;
        }

        playerFactions.put(player.getUniqueId(), faction.getName());
        removeInvite(player, faction.getName());

        saveFaction(faction);

        return true;
    }

    public boolean leaveFaction(Player player) {
        if (player == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (faction.isLeader(player.getUniqueId())) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        faction.removeMember(uuid);
        playerFactions.remove(uuid);

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // KICK
    // =========================================================

    public boolean kick(Faction faction, UUID target) {
        if (faction == null || target == null) {
            return false;
        }

        if (faction.isLeader(target)) {
            return false;
        }

        if (!faction.isMember(target)) {
            return false;
        }

        faction.removeMember(target);
        playerFactions.remove(target);

        saveFaction(faction);

        return true;
    }

    public boolean kick(Player player, UUID target) {
        Faction faction = getFaction(player);

        if (faction == null || !hasPermission(
                player,
                Faction.FactionPermission.KICK
        )) {
            return false;
        }

        return kick(faction, target);
    }

    // =========================================================
    // PROMOTE / DEMOTE
    // =========================================================

    public boolean promote(Faction faction, UUID target) {
        if (faction == null || target == null) {
            return false;
        }

        if (!faction.isMember(target) || faction.isLeader(target)) {
            return false;
        }

        if (faction.isCaptain(target)) {
            return faction.promoteToCoLeader(target)
                    && saveFaction(faction);
        }

        if (faction.isCoLeader(target)) {
            return false;
        }

        return faction.promoteToCaptain(target)
                && saveFaction(faction);
    }

    public boolean promote(Player player, UUID target) {
        Faction faction = getFaction(player);

        if (faction == null ||
                !hasPermission(
                        player,
                        Faction.FactionPermission.PROMOTE
                )) {
            return false;
        }

        return promote(faction, target);
    }

    public boolean demote(Faction faction, UUID target) {
        if (faction == null || target == null) {
            return false;
        }

        if (faction.isCoLeader(target)) {
            return faction.demoteFromCoLeader(target)
                    && saveFaction(faction);
        }

        if (faction.isCaptain(target)) {
            return faction.demoteFromCaptain(target)
                    && saveFaction(faction);
        }

        return false;
    }

    public boolean demote(Player player, UUID target) {
        Faction faction = getFaction(player);

        if (faction == null ||
                !hasPermission(
                        player,
                        Faction.FactionPermission.DEMOTE
                )) {
            return false;
        }

        return demote(faction, target);
    }

    // =========================================================
    // ROLES
    // =========================================================

    public boolean isLeader(Player player) {
        return player != null && isLeader(player.getUniqueId());
    }

    public boolean isLeader(UUID uuid) {
        Faction faction = getFaction(uuid);
        return faction != null && faction.isLeader(uuid);
    }

    public Faction.FactionRole getRole(Player player) {
        return player == null
                ? Faction.FactionRole.MEMBER
                : getRole(player.getUniqueId());
    }

    public Faction.FactionRole getRole(UUID uuid) {
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return Faction.FactionRole.MEMBER;
        }

        return faction.getRole(uuid);
    }

    /**
     * Compatibilidad antigua:
     * Officer = Captain.
     */
    public boolean isOfficer(Player player) {
        return player != null && isOfficer(player.getUniqueId());
    }

    public boolean isOfficer(UUID uuid) {
        Faction faction = getFaction(uuid);
        return faction != null && faction.isCaptain(uuid);
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    public boolean hasPermission(
            Player player,
            Faction.FactionPermission permission
    ) {
        if (player == null) return false;

        return hasPermission(player.getUniqueId(), permission);
    }

    public boolean hasPermission(
            UUID uuid,
            Faction.FactionPermission permission
    ) {
        Faction faction = getFaction(uuid);

        if (faction == null || permission == null) {
            return false;
        }

        return faction.hasPermission(uuid, permission);
    }

    public boolean canManageFaction(Player player) {
        return player != null &&
                canManageFaction(player.getUniqueId());
    }

    public boolean canManageFaction(UUID uuid) {
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        return faction.isLeader(uuid)
                || faction.isCoLeader(uuid);
    }

    // =========================================================
    // HOME
    // =========================================================

    public boolean setHome(Player player) {
        if (player == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (!hasPermission(
                player,
                Faction.FactionPermission.SET_HOME
        )) {
            return false;
        }

        faction.setHome(player.getLocation());

        return saveFaction(faction);
    }

    public Location getHome(Player player) {
        return player == null ? null : getHome(player.getUniqueId());
    }

    public Location getHome(UUID uuid) {
        Faction faction = getFaction(uuid);
        return faction == null ? null : faction.getHome();
    }

    // =========================================================
    // ALLIES / ENEMIES
    // =========================================================

    public boolean addAlly(Faction faction, Faction target) {
        if (faction == null || target == null || faction == target) {
            return false;
        }

        faction.addAlly(target.getName());
        target.addAlly(faction.getName());

        saveFaction(faction);
        saveFaction(target);

        return true;
    }

    public boolean removeAlly(Faction faction, Faction target) {
        if (faction == null || target == null) {
            return false;
        }

        faction.removeAlly(target.getName());
        target.removeAlly(faction.getName());

        saveFaction(faction);
        saveFaction(target);

        return true;
    }

    public boolean addEnemy(Faction faction, Faction target) {
        if (faction == null || target == null || faction == target) {
            return false;
        }

        faction.addEnemy(target.getName());
        target.addEnemy(faction.getName());

        saveFaction(faction);
        saveFaction(target);

        return true;
    }

    public boolean removeEnemy(Faction faction, Faction target) {
        if (faction == null || target == null) {
            return false;
        }

        faction.removeEnemy(target.getName());
        target.removeEnemy(faction.getName());

        saveFaction(faction);
        saveFaction(target);

        return true;
    }

    // =========================================================
    // DTR
    // =========================================================

    public double getDtr(Player player) {
        return player == null ? 0.0 : getDtr(player.getUniqueId());
    }

    public double getDtr(UUID uuid) {
        Faction faction = getFaction(uuid);
        return faction == null ? 0.0 : faction.getDtr();
    }

    public void setDtr(Player player, double amount) {
        if (player != null) {
            setDtr(player.getUniqueId(), amount);
        }
    }

    public void setDtr(UUID uuid, double amount) {
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return;
        }

        faction.setDtr(amount);
        saveFaction(faction);
    }

    public void addDtr(Faction faction, double amount) {
        if (faction == null) return;

        faction.addDtr(amount);
        saveFaction(faction);
    }

    public void removeDtr(Faction faction, double amount) {
        if (faction == null) return;

        faction.removeDtr(amount);
        saveFaction(faction);
    }

    public boolean isRaidable(Faction faction) {
        return faction != null && faction.isRaidable();
    }

    public boolean isRaidable(Player player) {
        Faction faction = getFaction(player);
        return faction != null && faction.isRaidable();
    }

    // =========================================================
    // DEATH / KILLS
    // =========================================================

    public int getKills(Player player) {
        return player == null ? 0 : getKills(player.getUniqueId());
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getDeaths(Player player) {
        return player == null ? 0 : getDeaths(player.getUniqueId());
    }

    public int getDeaths(UUID uuid) {
        return deaths.getOrDefault(uuid, 0);
    }

    public void addKill(Player player) {
        if (player != null) {
            addKill(player.getUniqueId());
        }
    }

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
        saveStats(uuid);
    }

    public void addDeath(Player player) {
        if (player != null) {
            addDeath(player.getUniqueId());
        }
    }

    public void addDeath(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
        saveStats(uuid);
    }

    public void handleDeath(Player player) {
        if (player != null) {
            handleDeath(player.getUniqueId());
        }
    }

    public void handleDeath(UUID uuid) {
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return;
        }

        double loss = plugin.getConfig()
                .getDouble("factions.dtr-loss-on-death", 1.0);

        faction.removeDtr(loss);
        saveFaction(faction);
    }

    // =========================================================
    // SAVE ALL
    // =========================================================

    public void saveAll() {
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null || !db.isConnected()) {
            return;
        }

        Connection connection = db.getConnection();

        try {
            connection.setAutoCommit(false);

            /*
             * IMPORTANTE:
             * No borramos la tabla factions.
             *
             * Claims tiene FK hacia factions.
             * Si borramos/reinsertamos factions, SQLite puede
             * eliminar los claims asociados.
             */

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("DELETE FROM faction_members");
                st.executeUpdate("DELETE FROM faction_allies");
                st.executeUpdate("DELETE FROM faction_enemies");
                st.executeUpdate("DELETE FROM faction_homes");
            }

            for (Faction faction : factions.values()) {
                saveFaction(connection, faction);
            }

            connection.commit();

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            plugin.getLogger().warning(
                    "Error guardando factions: " + e.getMessage()
            );

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    // =========================================================
    // SAVE FACTION
    // =========================================================

    private boolean saveFaction(Faction faction) {
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null || !db.isConnected()) {
            return false;
        }

        try {
            saveFaction(db.getConnection(), faction);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error guardando faction "
                            + faction.getName()
                            + ": "
                            + e.getMessage()
            );
            return false;
        }
    }

    private void saveFaction(
            Connection connection,
            Faction faction
    ) throws SQLException {

        String sql =
                "INSERT INTO factions(name, leader, dtr, balance) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET " +
                "leader = excluded.leader, " +
                "dtr = excluded.dtr, " +
                "balance = excluded.balance";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, faction.getName());
            ps.setString(2, faction.getLeader().toString());
            ps.setDouble(3, faction.getDtr());
            ps.setDouble(4, faction.getBalance());

            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM faction_members WHERE faction = ?")) {

            ps.setString(1, faction.getName());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO faction_members(faction, uuid, role) VALUES (?, ?, ?)")) {

            for (UUID uuid : faction.getMembers()) {

                ps.setString(1, faction.getName());
                ps.setString(2, uuid.toString());

                Faction.FactionRole role = faction.getRole(uuid);

                ps.setString(3, role.name());

                ps.addBatch();

                playerFactions.put(uuid, faction.getName());
            }

            ps.executeBatch();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM faction_allies WHERE faction = ?")) {

            ps.setString(1, faction.getName());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO faction_allies(faction, ally) VALUES (?, ?)")) {

            for (String ally : faction.getAllies()) {
                ps.setString(1, faction.getName());
                ps.setString(2, ally);
                ps.addBatch();
            }

            ps.executeBatch();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM faction_enemies WHERE faction = ?")) {

            ps.setString(1, faction.getName());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO faction_enemies(faction, enemy) VALUES (?, ?)")) {

            for (String enemy : faction.getEnemies()) {
                ps.setString(1, faction.getName());
                ps.setString(2, enemy);
                ps.addBatch();
            }

            ps.executeBatch();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM faction_homes WHERE faction = ?")) {

            ps.setString(1, faction.getName());
            ps.executeUpdate();
        }

        Location home = faction.getHome();

        if (home != null && home.getWorld() != null) {

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO faction_homes(" +
                            "faction, world, x, y, z, yaw, pitch" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                ps.setString(1, faction.getName());
                ps.setString(2, home.getWorld().getName());
                ps.setDouble(3, home.getX());
                ps.setDouble(4, home.getY());
                ps.setDouble(5, home.getZ());
                ps.setFloat(6, home.getYaw());
                ps.setFloat(7, home.getPitch());

                ps.executeUpdate();
            }
        }
    }

    // =========================================================
    // LOAD ALL
    // =========================================================

    public void loadAll() {
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null || !db.isConnected()) {
            return;
        }

        factions.clear();
        playerFactions.clear();
        kills.clear();
        deaths.clear();

        Connection connection = db.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, leader, dtr, balance FROM factions");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("name");
                UUID leader = UUID.fromString(rs.getString("leader"));
                double dtr = rs.getDouble("dtr");
                double balance = rs.getDouble("balance");

                double maxDtr = plugin.getConfig()
                        .getDouble("factions.max-dtr", 5.0);

                Faction faction = new Faction(
                        name,
                        leader,
                        dtr,
                        maxDtr
                );

                faction.setBalance(balance);

                factions.put(
                        name.toLowerCase(Locale.ROOT),
                        faction
                );

                playerFactions.put(
                        leader,
                        name
                );
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando factions: " + e.getMessage()
            );
            return;
        }

        loadMembers(connection);
        loadAllies(connection);
        loadEnemies(connection);
        loadHomes(connection);
        loadStats(connection);
    }

    // =========================================================
    // LOAD MEMBERS
    // =========================================================

    private void loadMembers(Connection connection) {

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT faction, uuid, role FROM faction_members");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(rs.getString("faction"));

                if (faction == null) {
                    continue;
                }

                UUID uuid =
                        UUID.fromString(rs.getString("uuid"));

                String role =
                        rs.getString("role");

                if (!faction.isMember(uuid)) {
                    faction.addMember(uuid);
                }

                switch (role.toUpperCase(Locale.ROOT)) {

                    case "CO_LEADER":
                    case "CO-LEADER":
                        faction.promoteToCaptain(uuid);
                        faction.promoteToCoLeader(uuid);
                        break;

                    case "CAPTAIN":
                    case "OFFICER":
                        faction.promoteToCaptain(uuid);
                        break;

                    case "LEADER":
                        break;

                    default:
                        break;
                }

                playerFactions.put(
                        uuid,
                        faction.getName()
                );
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando miembros: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD ALLIES
    // =========================================================

    private void loadAllies(Connection connection) {

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT faction, ally FROM faction_allies");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(rs.getString("faction"));

                if (faction == null) {
                    continue;
                }

                faction.addAlly(rs.getString("ally"));
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando aliados: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD ENEMIES
    // =========================================================

    private void loadEnemies(Connection connection) {

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT faction, enemy FROM faction_enemies");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(rs.getString("faction"));

                if (faction == null) {
                    continue;
                }

                faction.addEnemy(rs.getString("enemy"));
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando enemigos: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD HOMES
    // =========================================================

    private void loadHomes(Connection connection) {

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT faction, world, x, y, z, yaw, pitch " +
                        "FROM faction_homes");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(rs.getString("faction"));

                if (faction == null) {
                    continue;
                }

                org.bukkit.World world =
                        plugin.getServer().getWorld(
                                rs.getString("world")
                        );

                if (world == null) {
                    continue;
                }

                Location location = new Location(
                        world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );

                faction.setHome(location);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando homes: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD STATS
    // =========================================================

    private void loadStats(Connection connection) {

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, kills, deaths FROM player_stats");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                UUID uuid =
                        UUID.fromString(rs.getString("uuid"));

                kills.put(
                        uuid,
                        rs.getInt("kills")
                );

                deaths.put(
                        uuid,
                        rs.getInt("deaths")
                );
            }

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error cargando estadísticas: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // SAVE STATS
    // =========================================================

    private void saveStats(UUID uuid) {

        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null || !db.isConnected()) {
            return;
        }

        try (PreparedStatement ps =
                     db.getConnection().prepareStatement(
                             "INSERT INTO player_stats(uuid, kills, deaths) " +
                                     "VALUES (?, ?, ?) " +
                                     "ON CONFLICT(uuid) DO UPDATE SET " +
                                     "kills = excluded.kills, " +
                                     "deaths = excluded.deaths"
                     )) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, getKills(uuid));
            ps.setInt(3, getDeaths(uuid));

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Error guardando estadísticas: "
                            + e.getMessage()
            );
        }
    }
}