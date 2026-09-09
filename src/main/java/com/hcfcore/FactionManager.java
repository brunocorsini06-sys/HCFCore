package com.hcfcore;

import org.bukkit.Location;
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
public FactionManager(HCFCore plugin) {
    this.plugin = plugin;
    loadAll();

}
// =========================================================
// FACTIONS
// =========================================================
public Collection<Faction> getFactions() {
    return Collections.unmodifiableCollection(factions.values());
}
public Faction getFaction(String name) {
    if (name == null) {
        return null;
    }
    for (Faction faction : factions.values()) {
        if (faction.getName().equalsIgnoreCase(name)) {
            return faction;
        }
    }
    return null;
}
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
    String factionName = playerFactions.get(uuid);
    if (factionName != null) {
        Faction faction = getFaction(factionName);
        if (faction != null) {
            return faction;
        }
    }
    for (Faction faction : factions.values()) {
        if (faction.isMember(uuid)) {
            playerFactions.put(uuid, faction.getName());
            return faction;
        }
    }
    return null;
}
public String getFactionName(UUID uuid) {
    Faction faction = getFaction(uuid);
    return faction == null ? null : faction.getName();
}
public boolean hasFaction(UUID uuid) {
    return getFaction(uuid) != null;
}
public boolean hasFaction(Player player) {
    return player != null && hasFaction(player.getUniqueId());
}
public int getFactionCount() {
    return factions.size();
}
public boolean isValidFactionName(String name) {
    if (name == null) {
        return false;
    }
    return name.length() >= 3
            && name.length() <= 16
            && name.matches("[A-Za-z0-9_]+");
}
// =========================================================
// CREATE
// =========================================================
public Faction createFaction(Player player, String name) {
    if (player == null || !isValidFactionName(name)) {
        return null;
    }
    if (getFaction(player) != null) {
        return null;
    }
    if (getFaction(name) != null) {
        return null;
    }
    double startingDtr =
            plugin.getConfig().getDouble(
                    "factions.starting-dtr",
                    1.0
            );
    double maxDtr =
            plugin.getConfig().getDouble(
                    "factions.max-dtr",
                    5.0
            );
    Faction faction =
            new Faction(
                    name,
                    player.getUniqueId(),
                    startingDtr,
                    maxDtr
            );
    factions.put(name.toLowerCase(Locale.ROOT), faction);
    playerFactions.put(
            player.getUniqueId(),
            faction.getName()
    );
    saveFaction(faction);
    return faction;
}
// =========================================================
// DISBAND
// =========================================================
public boolean disband(Faction faction) {
    if (faction == null) {
        return false;
    }
    String key = faction.getName().toLowerCase(Locale.ROOT);
    factions.remove(key);
    for (UUID uuid : new HashSet<>(faction.getMembers())) {
        playerFactions.remove(uuid);
    }
    plugin.getClaimManager().removeFactionClaims(faction);
    deleteFactionFromDatabase(faction.getName());
    return true;
}
// Compatibility overload
public boolean disband(Player player) {
    if (player == null) {
        return false;
    }
    return disband(getFaction(player));
}
// =========================================================
// INVITES
// =========================================================
public boolean invite(Player target, Faction faction) {
    if (target == null || faction == null) {
        return false;
    }
    if (getFaction(target) != null) {
        return false;
    }
    int maxMembers =
            plugin.getConfig().getInt(
                    "factions.max-members",
                    20
            );
    if (faction.getMembers().size() >= maxMembers) {
        return false;
    }
    invites.put(
            target.getUniqueId(),
            faction.getName()
    );
    return true;
}
public boolean hasInvite(Player player, String factionName) {
    if (player == null || factionName == null) {
        return false;
    }
    String invitedFaction =
            invites.get(player.getUniqueId());
    return invitedFaction != null
            && invitedFaction.equalsIgnoreCase(factionName);
}
public String getInvite(Player player) {
    if (player == null) {
        return null;
    }
    return invites.get(player.getUniqueId());
}
public void removeInvite(Player player) {
    if (player != null) {
        invites.remove(player.getUniqueId());
    }
}
// =========================================================
// JOIN / LEAVE
// =========================================================
public boolean joinFaction(Player player, String factionName) {
    if (player == null || factionName == null) {
        return false;
    }
    if (getFaction(player) != null) {
        return false;
    }
    Faction faction = getFaction(factionName);
    if (faction == null) {
        return false;
    }
    if (!hasInvite(player, faction.getName())) {
        return false;
    }
    int maxMembers =
            plugin.getConfig().getInt(
                    "factions.max-members",
                    20
            );
    if (faction.getMembers().size() >= maxMembers) {
        return false;
    }
    if (!faction.addMember(player.getUniqueId())) {
        return false;
    }
    playerFactions.put(
            player.getUniqueId(),
            faction.getName()
    );
    removeInvite(player);
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
    if (!faction.removeMember(uuid)) {
        return false;
    }
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
    if (!faction.isMember(target)) {
        return false;
    }
    if (faction.isLeader(target)) {
        return false;
    }
    if (!faction.removeMember(target)) {
        return false;
    }
    playerFactions.remove(target);
    saveFaction(faction);
    return true;
}
// Compatibility overload
public boolean kick(Player player, UUID target) {
    if (player == null) {
        return false;
    }
    return kick(getFaction(player), target);
}
// =========================================================
// PROMOTE
// =========================================================
public boolean promote(Faction faction, UUID target) {
    if (faction == null || target == null) {
        return false;
    }
    Faction.FactionRole role =
            faction.getRole(target);
    if (role == null) {
        return false;
    }
    if (role == Faction.FactionRole.MEMBER) {
        return faction.promoteToCaptain(target)
                && saveFaction(faction);
    }
    if (role == Faction.FactionRole.CAPTAIN) {
        return faction.promoteToCoLeader(target)
                && saveFaction(faction);
    }
    return false;
}
// Compatibility overload
public boolean promote(Player player, UUID target) {
    if (player == null) {
        return false;
    }
    return promote(getFaction(player), target);
}
// =========================================================
// DEMOTE
// =========================================================
public boolean demote(Faction faction, UUID target) {
    if (faction == null || target == null) {
        return false;
    }
    Faction.FactionRole role =
            faction.getRole(target);
    if (role == null) {
        return false;
    }
    if (role == Faction.FactionRole.CO_LEADER) {
        return faction.demoteFromCoLeader(target)
                && saveFaction(faction);
    }
    if (role == Faction.FactionRole.CAPTAIN) {
        return faction.demoteFromCaptain(target)
                && saveFaction(faction);
    }
    return false;
}
// Compatibility overload
public boolean demote(Player player, UUID target) {
    if (player == null) {
        return false;
    }
    return demote(getFaction(player), target);
}
// =========================================================
// LEADERSHIP / PERMISSIONS
// =========================================================
public boolean isLeader(Player player) {
    return player != null
            && isLeader(player.getUniqueId());
}
public boolean isLeader(UUID uuid) {
    Faction faction = getFaction(uuid);
    return faction != null && faction.isLeader(uuid);
}
public Faction.FactionRole getRole(UUID uuid) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return null;
    }
    return faction.getRole(uuid);
}
public Faction.FactionRole getRole(Player player) {
    return player == null
            ? null
            : getRole(player.getUniqueId());
}
public boolean isOfficer(UUID uuid) {
    Faction faction = getFaction(uuid);
    return faction != null && faction.isCaptain(uuid);
}
public boolean isOfficer(Player player) {
    return player != null && isOfficer(player.getUniqueId());
}
public boolean hasPermission(
        Player player,
        Faction.FactionPermission permission
) {
    if (player == null) {
        return false;
    }
    return hasPermission(
            player.getUniqueId(),
            permission
    );
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
    return player != null
            && canManageFaction(player.getUniqueId());
}
public boolean canManageFaction(UUID uuid) {
    return hasPermission(
            uuid,
            Faction.FactionPermission.MANAGE_CLAIMS
    );
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
    faction.setHome(player.getLocation());
    return saveFaction(faction);
}
public Location getHome(Player player) {
    if (player == null) {
        return null;
    }
    Faction faction = getFaction(player);
    if (faction == null) {
        return null;
    }
    return faction.getHome();
}
public Location getHome(UUID uuid) {
    if (uuid == null) {
        return null;
    }
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return null;
    }
    return faction.getHome();
}
// =========================================================
// RELATIONS
// =========================================================
public boolean addAlly(Faction faction, Faction target) {
    if (faction == null || target == null) {
        return false;
    }
    if (faction == target) {
        return false;
    }
    boolean changed = faction.addAlly(target.getName());
    if (!changed) {
        return false;
    }
    target.addAlly(faction.getName());
    saveFaction(faction);
    saveFaction(target);
    return true;
}
public boolean removeAlly(Faction faction, Faction target) {
    if (faction == null || target == null) {
        return false;
    }
    boolean changed =
            faction.removeAlly(target.getName());
    target.removeAlly(faction.getName());
    saveFaction(faction);
    saveFaction(target);
    return changed;
}
public boolean addEnemy(Faction faction, Faction target) {
    if (faction == null || target == null) {
        return false;
    }
    if (faction == target) {
        return false;
    }
    boolean changed =
            faction.addEnemy(target.getName());
    if (!changed) {
        return false;
    }
    target.addEnemy(faction.getName());
    saveFaction(faction);
    saveFaction(target);
    return true;
}
public boolean removeEnemy(Faction faction, Faction target) {
    if (faction == null || target == null) {
        return false;
    }
    boolean changed =
            faction.removeEnemy(target.getName());
    target.removeEnemy(faction.getName());
    saveFaction(faction);
    saveFaction(target);
    return changed;
}
// =========================================================
// DTR
// =========================================================
public double getDtr(UUID uuid) {
    Faction faction = getFaction(uuid);
    return faction == null ? 0.0 : faction.getDtr();
}
public double getDtr(Player player) {
    return player == null
            ? 0.0
            : getDtr(player.getUniqueId());
}
public void setDtr(UUID uuid, double dtr) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    faction.setDtr(dtr);
    saveFaction(faction);
}
public void setDtr(Player player, double dtr) {
    if (player != null) {
        setDtr(player.getUniqueId(), dtr);
    }
}
public void addDtr(UUID uuid, double amount) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    faction.addDtr(amount);
    saveFaction(faction);
}
public void addDtr(Player player, double amount) {
    if (player != null) {
        addDtr(player.getUniqueId(), amount);
    }
}
public void removeDtr(UUID uuid, double amount) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    faction.removeDtr(amount);
    saveFaction(faction);
}
public void removeDtr(Player player, double amount) {
    if (player != null) {
        removeDtr(player.getUniqueId(), amount);
    }
}
public void freezeDtr(UUID uuid) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    faction.freezeDtr();
    saveFaction(faction);
}
public void unfreezeDtr(UUID uuid) {
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    faction.unfreezeDtr();
    saveFaction(faction);
}
// =========================================================
// STATS
// =========================================================
public int getKills(UUID uuid) {
    if (plugin.getDatabaseManager() == null) {
        return 0;
    }
    return getStat(uuid, "kills");
}
public int getKills(Player player) {
    return player == null
            ? 0
            : getKills(player.getUniqueId());
}
public int getDeaths(UUID uuid) {
    if (plugin.getDatabaseManager() == null) {
        return 0;
    }
    return getStat(uuid, "deaths");
}
public int getDeaths(Player player) {
    return player == null
            ? 0
            : getDeaths(player.getUniqueId());
}
public void addKill(UUID uuid) {
    if (uuid == null) {
        return;
    }
    updateStat(uuid, "kills");
}
public void addKill(Player player) {
    if (player != null) {
        addKill(player.getUniqueId());
    }
}
public void addDeath(UUID uuid) {
    if (uuid == null) {
        return;
    }
    updateStat(uuid, "deaths");
}
public void addDeath(Player player) {
    if (player != null) {
        addDeath(player.getUniqueId());
    }
}
public void handleDeath(UUID uuid) {
    if (uuid == null) {
        return;
    }
    Faction faction = getFaction(uuid);
    if (faction == null) {
        return;
    }
    double loss =
            plugin.getConfig().getDouble(
                    "factions.dtr-loss-on-death",
                    1.0
            );
    if (loss <= 0) {
        return;
    }
    faction.removeDtr(loss);
    saveFaction(faction);
}
public void handleDeath(Player player) {
    if (player != null) {
        handleDeath(player.getUniqueId());
    }
}
// =========================================================
// PERSISTENCE
// =========================================================
public void saveAll() {
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return;
    }
    boolean autoCommit = true;
    try {
        autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        // IMPORTANT:
        // Never delete factions here.
        // Claims reference factions through foreign keys.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM faction_members");
            statement.executeUpdate("DELETE FROM faction_allies");
            statement.executeUpdate("DELETE FROM faction_enemies");
            statement.executeUpdate("DELETE FROM faction_homes");
        }
        for (Faction faction : factions.values()) {
            upsertFaction(connection, faction);
            saveMembers(connection, faction);
            saveRelations(connection, faction);
            saveHome(connection, faction);
        }
        connection.commit();
    } catch (SQLException e) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
        plugin.getLogger().severe(
                "No se pudieron guardar las factions: "
                        + e.getMessage()
        );
    } finally {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
        }
    }
}
private boolean saveFaction(Faction faction) {
    if (faction == null) {
        return false;
    }
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return false;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return false;
    }
    try {
        upsertFaction(connection, faction);
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_members WHERE faction = ?"
                     )) {
            statement.setString(1, faction.getName());
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_allies WHERE faction = ?"
                     )) {
            statement.setString(1, faction.getName());
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_enemies WHERE faction = ?"
                     )) {
            statement.setString(1, faction.getName());
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_homes WHERE faction = ?"
                     )) {
            statement.setString(1, faction.getName());
            statement.executeUpdate();
        }
        saveMembers(connection, faction);
        saveRelations(connection, faction);
        saveHome(connection, faction);
        return true;
    } catch (SQLException e) {
        plugin.getLogger().warning(
                "No se pudo guardar la faction "
                        + faction.getName()
                        + ": "
                        + e.getMessage()
        );
        return false;
    }
}
private void upsertFaction(
        Connection connection,
        Faction faction
) throws SQLException {
    String sql =
            "INSERT INTO factions " +
            "(name, leader, dtr, balance) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT(name) DO UPDATE SET " +
            "leader = excluded.leader, " +
            "dtr = excluded.dtr, " +
            "balance = excluded.balance";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
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
        statement.executeUpdate();
    }
}
private void saveMembers(
        Connection connection,
        Faction faction
) throws SQLException {
    String sql =
            "INSERT INTO faction_members " +
            "(faction, uuid, role) VALUES (?, ?, ?)";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
        for (UUID uuid : faction.getMembers()) {
            Faction.FactionRole role =
                    faction.getRole(uuid);
            if (role == null) {
                role = Faction.FactionRole.MEMBER;
            }
            statement.setString(
                    1,
                    faction.getName()
            );
            statement.setString(
                    2,
                    uuid.toString()
            );
            statement.setString(
                    3,
                    role.name()
            );
            statement.addBatch();
        }
        statement.executeBatch();
    }
}
private void saveRelations(
        Connection connection,
        Faction faction
) throws SQLException {
    String allySql =
            "INSERT INTO faction_allies " +
            "(faction, ally) VALUES (?, ?)";
    try (PreparedStatement statement =
                 connection.prepareStatement(allySql)) {
        for (String ally : faction.getAllies()) {
            statement.setString(
                    1,
                    faction.getName()
            );
            statement.setString(
                    2,
                    ally
            );
            statement.addBatch();
        }
        statement.executeBatch();
    }
    String enemySql =
            "INSERT INTO faction_enemies " +
            "(faction, enemy) VALUES (?, ?)";
    try (PreparedStatement statement =
                 connection.prepareStatement(enemySql)) {
        for (String enemy : faction.getEnemies()) {
            statement.setString(
                    1,
                    faction.getName()
            );
            statement.setString(
                    2,
                    enemy
            );
            statement.addBatch();
        }
        statement.executeBatch();
    }
}
private void saveHome(
        Connection connection,
        Faction faction
) throws SQLException {
    Location home = faction.getHome();
    if (home == null || home.getWorld() == null) {
        return;
    }
    String sql =
            "INSERT INTO faction_homes " +
            "(faction, world, x, y, z, yaw, pitch) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
        statement.setString(
                1,
                faction.getName()
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
        statement.executeUpdate();
    }
}
// =========================================================
// LOAD
// =========================================================
public void loadAll() {
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return;
    }
    factions.clear();
    playerFactions.clear();
    try {
        loadFactions(connection);
        loadMembers(connection);
        loadRelations(connection);
        loadHomes(connection);
    } catch (SQLException e) {
        plugin.getLogger().severe(
                "No se pudieron cargar las factions: "
                        + e.getMessage()
        );
    }
}
private void loadFactions(
        Connection connection
) throws SQLException {
    String sql =
            "SELECT name, leader, dtr, balance FROM factions";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql);
         ResultSet result =
                 statement.executeQuery()) {
        while (result.next()) {
            String name =
                    result.getString("name");
            String leaderString =
                    result.getString("leader");
            UUID leader;
            try {
                leader =
                        UUID.fromString(leaderString);
            } catch (IllegalArgumentException e) {
                continue;
            }
            double dtr =
                    result.getDouble("dtr");
            double balance =
                    result.getDouble("balance");
            double maxDtr =
                    plugin.getConfig().getDouble(
                            "factions.max-dtr",
                            5.0
                    );
            Faction faction =
                    new Faction(
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
    }
}
private void loadMembers(
        Connection connection
) throws SQLException {
    String sql =
            "SELECT faction, uuid, role FROM faction_members";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql);
         ResultSet result =
                 statement.executeQuery()) {
        while (result.next()) {
            String factionName =
                    result.getString("faction");
            Faction faction =
                    getFaction(factionName);
            if (faction == null) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(
                        result.getString("uuid")
                );
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!faction.isMember(uuid)) {
                faction.addMember(uuid);
            }
            String roleString =
                    result.getString("role");
            if (roleString == null) {
                roleString = "MEMBER";
            }
            try {
                Faction.FactionRole role =
                        Faction.FactionRole.valueOf(
                                roleString
                        );
                if (role ==
                        Faction.FactionRole.CO_LEADER) {
                    faction.promoteToCaptain(uuid);
                    faction.promoteToCoLeader(uuid);
                } else if (role ==
                        Faction.FactionRole.CAPTAIN) {
                    faction.promoteToCaptain(uuid);
                }
            } catch (IllegalArgumentException ignored) {
                // Old OFFICER values are intentionally
                // treated as MEMBER/CAPTAIN-compatible
                // legacy data.
                if ("OFFICER".equalsIgnoreCase(roleString)) {
                    faction.promoteToCaptain(uuid);
                }
            }
            playerFactions.put(
                    uuid,
                    faction.getName()
            );
        }
    }
}
private void loadRelations(
        Connection connection
) throws SQLException {
    String allySql =
            "SELECT faction, ally FROM faction_allies";
    try (PreparedStatement statement =
                 connection.prepareStatement(allySql);
         ResultSet result =
                 statement.executeQuery()) {
        while (result.next()) {
            Faction faction =
                    getFaction(
                            result.getString("faction")
                    );
            if (faction != null) {
                faction.addAlly(
                        result.getString("ally")
                );
            }
        }
    }
    String enemySql =
            "SELECT faction, enemy FROM faction_enemies";
    try (PreparedStatement statement =
                 connection.prepareStatement(enemySql);
         ResultSet result =
                 statement.executeQuery()) {
        while (result.next()) {
            Faction faction =
                    getFaction(
                            result.getString("faction")
                    );
            if (faction != null) {
                faction.addEnemy(
                        result.getString("enemy")
                );
            }
        }
    }
}
private void loadHomes(
        Connection connection
) throws SQLException {
    String sql =
            "SELECT faction, world, x, y, z, yaw, pitch " +
            "FROM faction_homes";
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
            org.bukkit.World world =
                    org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
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
    }
}
// =========================================================
// DATABASE DELETE
// =========================================================
private void deleteFactionFromDatabase(
        String factionName
) {
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return;
    }
    try {
        connection.setAutoCommit(false);
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_members WHERE faction = ?"
                     )) {
            statement.setString(1, factionName);
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_allies WHERE faction = ?"
                     )) {
            statement.setString(1, factionName);
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_enemies WHERE faction = ?"
                     )) {
            statement.setString(1, factionName);
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM faction_homes WHERE faction = ?"
                     )) {
            statement.setString(1, factionName);
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM factions WHERE name = ?"
                     )) {
            statement.setString(1, factionName);
            statement.executeUpdate();
        }
        connection.commit();
    } catch (SQLException e) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
        plugin.getLogger().warning(
                "No se pudo eliminar la faction "
                        + factionName
                        + ": "
                        + e.getMessage()
        );
    } finally {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
// =========================================================
// STATS DATABASE
// =========================================================
private int getStat(
        UUID uuid,
        String column
) {
    if (!column.equals("kills")
            && !column.equals("deaths")) {
        return 0;
    }
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return 0;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return 0;
    }
    String sql =
            "SELECT " + column +
            " FROM player_stats WHERE uuid = ?";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
        statement.setString(
                1,
                uuid.toString()
        );
        try (ResultSet result =
                     statement.executeQuery()) {
            if (result.next()) {
                return result.getInt(column);
            }
        }
    } catch (SQLException e) {
        plugin.getLogger().warning(
                "No se pudo obtener " +
                        column +
                        ": " +
                        e.getMessage()
        );
    }
    return 0;
}
private void updateStat(
        UUID uuid,
        String column
) {
    if (!column.equals("kills")
            && !column.equals("deaths")) {
        return;
    }
    DatabaseManager database =
            plugin.getDatabaseManager();
    if (database == null) {
        return;
    }
    Connection connection =
            database.getConnection();
    if (connection == null) {
        return;
    }
    String sql =
            "INSERT INTO player_stats " +
            "(uuid, kills, deaths) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT(uuid) DO UPDATE SET " +
            column +
            " = " +
            column +
            " + 1";
    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
        statement.setString(
                1,
                uuid.toString()
        );
        statement.setInt(2, 0);
        statement.setInt(3, 0);
        statement.executeUpdate();
    } catch (SQLException e) {
        plugin.getLogger().warning(
                "No se pudo actualizar " +
                        column +
                        ": " +
                        e.getMessage()
        );
    }
}