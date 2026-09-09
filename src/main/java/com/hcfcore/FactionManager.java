package com.hcfcore;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class FactionManager {

    private final HCFCore plugin;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();

    /*
     * Invitaciones.
     *
     * UUID del jugador invitado -> nombre de faction.
     */
    private final Map<UUID, String> invites = new HashMap<>();

    /*
     * Estadísticas.
     */
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    /*
     * Última regeneración DTR.
     *
     * Se mantiene en memoria porque es información temporal.
     */
    private final Map<String, Long> lastDtrRegeneration = new HashMap<>();

    private static final Pattern FACTION_NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]+$");

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;

        createPersistenceTables();
        loadAll();
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Map<String, Faction> getFactions() {
        return factions;
    }

    public Faction getFaction(String name) {
        if (name == null) {
            return null;
        }

        return factions.get(name.toLowerCase());
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

        if (factionName == null) {
            return null;
        }

        return getFaction(factionName);
    }

    public String getFactionName(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        return playerFactions.get(uuid);
    }

    public boolean hasFaction(UUID uuid) {
        return uuid != null && playerFactions.containsKey(uuid);
    }

    public boolean hasFaction(Player player) {
        return player != null && hasFaction(player.getUniqueId());
    }

    public int getFactionCount() {
        return factions.size();
    }

    // =========================================================
    // CREAR FACTION
    // =========================================================

    public boolean createFaction(Player player, String name) {

        if (player == null || name == null) {
            return false;
        }

        name = name.trim();

        if (!isValidFactionName(name)) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        if (hasFaction(uuid)) {
            return false;
        }

        String key = name.toLowerCase();

        if (factions.containsKey(key)) {
            return false;
        }

        int maxMembers = plugin.getConfig()
                .getInt("factions.max-members", 20);

        if (maxMembers < 1) {
            maxMembers = 20;
        }

        double startingDtr = getStartingDtr();
        double maxDtr = getConfiguredMaxDtr();

        Faction faction = new Faction(
                name,
                uuid,
                startingDtr,
                maxDtr
        );

        factions.put(key, faction);
        playerFactions.put(uuid, key);

        lastDtrRegeneration.put(
                key,
                System.currentTimeMillis()
        );

        saveAll();

        return true;
    }

    public boolean isValidFactionName(String name) {

        if (name == null) {
            return false;
        }

        name = name.trim();

        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        return FACTION_NAME_PATTERN.matcher(name).matches();
    }

    // =========================================================
    // DISBAND
    // =========================================================

    public boolean disband(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        if (!faction.isLeader(uuid)) {
            return false;
        }

        String factionKey = faction.getName().toLowerCase();

        /*
         * Primero eliminamos los claims.
         */
        if (plugin.getClaimManager() != null) {
            plugin.getClaimManager()
                    .removeFactionClaims(faction);
        }

        /*
         * Eliminamos referencias de jugadores.
         */
        for (UUID member : new HashSet<>(faction.getMembers())) {
            playerFactions.remove(member);
        }

        /*
         * Eliminamos invitaciones de esta faction.
         */
        invites.entrySet().removeIf(
                entry -> factionKey.equals(
                        entry.getValue()
                )
        );

        factions.remove(factionKey);
        lastDtrRegeneration.remove(factionKey);

        /*
         * IMPORTANTE:
         *
         * Como DatabaseManager utiliza foreign keys,
         * eliminamos solamente esta faction de SQLite.
         *
         * No usamos saveAll() para borrar toda la tabla.
         */
        deleteFactionFromDatabase(faction.getName());

        saveAll();

        return true;
    }

    // =========================================================
    // JOIN
    // =========================================================

    public boolean joinFaction(Player player, String name) {

        if (player == null || name == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        if (hasFaction(uuid)) {
            return false;
        }

        Faction faction = getFaction(name);

        if (faction == null) {
            return false;
        }

        if (!hasInvite(uuid, faction.getName())) {
            return false;
        }

        int maxMembers = plugin.getConfig()
                .getInt("factions.max-members", 20);

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        faction.addMember(uuid);
        playerFactions.put(
                uuid,
                faction.getName().toLowerCase()
        );

        removeInvite(uuid);

        saveAll();

        return true;
    }

    // =========================================================
    // LEAVE
    // =========================================================

    public boolean leaveFaction(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        if (faction.isLeader(uuid)) {
            return false;
        }

        faction.removeMember(uuid);
        playerFactions.remove(uuid);

        saveAll();

        return true;
    }

    // =========================================================
    // INVITES
    // =========================================================

    public boolean invite(Player sender, Player target) {

        if (sender == null || target == null) {
            return false;
        }

        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Faction faction = getFaction(senderUuid);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                senderUuid,
                Faction.FactionPermission.INVITE
        )) {
            return false;
        }

        if (hasFaction(targetUuid)) {
            return false;
        }

        int maxMembers = plugin.getConfig()
                .getInt("factions.max-members", 20);

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        invites.put(
                targetUuid,
                faction.getName().toLowerCase()
        );

        return true;
    }

    public boolean hasInvite(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        return invites.containsKey(uuid);
    }

    public boolean hasInvite(UUID uuid, String factionName) {

        if (uuid == null || factionName == null) {
            return false;
        }

        String invitedFaction = invites.get(uuid);

        if (invitedFaction == null) {
            return false;
        }

        return invitedFaction.equalsIgnoreCase(factionName);
    }

    public void removeInvite(UUID uuid) {

        if (uuid == null) {
            return;
        }

        invites.remove(uuid);
    }

    public String getInvite(UUID uuid) {

        if (uuid == null) {
            return null;
        }

        return invites.get(uuid);
    }

    // =========================================================
    // KICK
    // =========================================================

    public boolean kick(Player sender, UUID targetUuid) {

        if (sender == null || targetUuid == null) {
            return false;
        }

        UUID senderUuid = sender.getUniqueId();

        Faction faction = getFaction(senderUuid);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                senderUuid,
                Faction.FactionPermission.KICK
        )) {
            return false;
        }

        if (!faction.isMember(targetUuid)) {
            return false;
        }

        /*
         * No se puede expulsar al leader.
         */
        if (faction.isLeader(targetUuid)) {
            return false;
        }

        /*
         * Un miembro no puede expulsar a alguien
         * con rango superior o igual.
         */
        Faction.FactionRole senderRole =
                faction.getRole(senderUuid);

        Faction.FactionRole targetRole =
                faction.getRole(targetUuid);

        if (senderRole == null || targetRole == null) {
            return false;
        }

        if (!senderRole.isHigherThan(targetRole)) {
            return false;
        }

        faction.removeMember(targetUuid);
        playerFactions.remove(targetUuid);

        saveAll();

        return true;
    }

    // =========================================================
    // PROMOTE
    // =========================================================

    public boolean promote(Player sender, UUID targetUuid) {

        if (sender == null || targetUuid == null) {
            return false;
        }

        UUID senderUuid = sender.getUniqueId();

        Faction faction = getFaction(senderUuid);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                senderUuid,
                Faction.FactionPermission.PROMOTE
        )) {
            return false;
        }

        if (!faction.isMember(targetUuid)) {
            return false;
        }

        if (faction.isLeader(targetUuid)) {
            return false;
        }

        Faction.FactionRole senderRole =
                faction.getRole(senderUuid);

        Faction.FactionRole targetRole =
                faction.getRole(targetUuid);

        if (senderRole == null || targetRole == null) {
            return false;
        }

        /*
         * No puede promover a alguien de rango
         * igual o superior.
         */
        if (!senderRole.isHigherThan(targetRole)) {
            return false;
        }

        switch (targetRole) {

            case MEMBER:
                faction.promoteToCaptain(targetUuid);
                break;

            case CAPTAIN:
                faction.promoteToCoLeader(targetUuid);
                break;

            case CO_LEADER:
            case LEADER:
                return false;
        }

        saveAll();

        return true;
    }

    // =========================================================
    // DEMOTE
    // =========================================================

    public boolean demote(Player sender, UUID targetUuid) {

        if (sender == null || targetUuid == null) {
            return false;
        }

        UUID senderUuid = sender.getUniqueId();

        Faction faction = getFaction(senderUuid);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                senderUuid,
                Faction.FactionPermission.DEMOTE
        )) {
            return false;
        }

        if (!faction.isMember(targetUuid)) {
            return false;
        }

        if (faction.isLeader(targetUuid)) {
            return false;
        }

        Faction.FactionRole senderRole =
                faction.getRole(senderUuid);

        Faction.FactionRole targetRole =
                faction.getRole(targetUuid);

        if (senderRole == null || targetRole == null) {
            return false;
        }

        if (!senderRole.isHigherThan(targetRole)) {
            return false;
        }

        switch (targetRole) {

            case CO_LEADER:
                faction.demoteFromCoLeader(targetUuid);
                break;

            case CAPTAIN:
                faction.demoteFromCaptain(targetUuid);
                break;

            case MEMBER:
            case LEADER:
                return false;
        }

        saveAll();

        return true;
    }

    // =========================================================
    // RANGOS
    // =========================================================

    public Faction.FactionRole getRole(
            UUID uuid
    ) {

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return null;
        }

        return faction.getRole(uuid);
    }

    public boolean isOfficer(UUID uuid) {

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        return faction.isCaptain(uuid);
    }

    public boolean hasPermission(
            UUID uuid,
            Faction.FactionPermission permission
    ) {

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        return faction.hasPermission(
                uuid,
                permission
        );
    }

    public boolean canManageFaction(
            UUID uuid
    ) {

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        Faction.FactionRole role =
                faction.getRole(uuid);

        return role == Faction.FactionRole.LEADER
                || role == Faction.FactionRole.CO_LEADER;
    }

    // =========================================================
    // HOME
    // =========================================================

    public boolean setHome(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                uuid,
                Faction.FactionPermission.SET_HOME
        )) {
            return false;
        }

        if (plugin.getClaimManager() == null) {
            return false;
        }

        Faction claimFaction =
                plugin.getClaimManager()
                        .getFactionAt(
                                player.getLocation()
                        );

        if (claimFaction == null) {
            return false;
        }

        if (!claimFaction.getName()
                .equalsIgnoreCase(faction.getName())) {
            return false;
        }

        faction.setHome(
                player.getLocation()
        );

        saveAll();

        return true;
    }

    // =========================================================
    // ALLIES
    // =========================================================

    public boolean addAlly(
            Player sender,
            String targetName
    ) {

        if (sender == null || targetName == null) {
            return false;
        }

        Faction faction =
                getFaction(sender.getUniqueId());

        Faction target =
                getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        if (!faction.hasPermission(
                sender.getUniqueId(),
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.addAlly(target.getName());
        target.addAlly(faction.getName());

        /*
         * Una relación ally deja de ser enemy.
         */
        faction.removeEnemy(target.getName());
        target.removeEnemy(faction.getName());

        saveAll();

        return true;
    }

    public boolean removeAlly(
            Player sender,
            String targetName
    ) {

        if (sender == null || targetName == null) {
            return false;
        }

        Faction faction =
                getFaction(sender.getUniqueId());

        Faction target =
                getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (!faction.hasPermission(
                sender.getUniqueId(),
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.removeAlly(target.getName());
        target.removeAlly(faction.getName());

        saveAll();

        return true;
    }

    // =========================================================
    // ENEMIES
    // =========================================================

    public boolean addEnemy(
            Player sender,
            String targetName
    ) {

        if (sender == null || targetName == null) {
            return false;
        }

        Faction faction =
                getFaction(sender.getUniqueId());

        Faction target =
                getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        if (!faction.hasPermission(
                sender.getUniqueId(),
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.addEnemy(target.getName());
        target.addEnemy(faction.getName());

        /*
         * Una relación enemy deja de ser ally.
         */
        faction.removeAlly(target.getName());
        target.removeAlly(faction.getName());

        saveAll();

        return true;
    }

    public boolean removeEnemy(
            Player sender,
            String targetName
    ) {

        if (sender == null || targetName == null) {
            return false;
        }

        Faction faction =
                getFaction(sender.getUniqueId());

        Faction target =
                getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (!faction.hasPermission(
                sender.getUniqueId(),
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.removeEnemy(target.getName());
        target.removeEnemy(faction.getName());

        saveAll();

        return true;
    }

    // =========================================================
    // STATS
    // =========================================================

    public void addKill(UUID uuid) {

        if (uuid == null) {
            return;
        }

        kills.merge(uuid, 1, Integer::sum);

        savePlayerStats(uuid);
    }

    public void addDeath(UUID uuid) {

        if (uuid == null) {
            return;
        }

        deaths.merge(uuid, 1, Integer::sum);

        savePlayerStats(uuid);
    }

    public int getKills(UUID uuid) {

        if (uuid == null) {
            return 0;
        }

        return kills.getOrDefault(uuid, 0);
    }

    public int getDeaths(UUID uuid) {

        if (uuid == null) {
            return 0;
        }

        return deaths.getOrDefault(uuid, 0);
    }

    // =========================================================
    // DEATH / DTR
    // =========================================================

    public void handleDeath(UUID uuid) {

        if (uuid == null) {
            return;
        }

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return;
        }

        /*
         * Si la faction ya está raidable,
         * no pierde más DTR.
         */
        if (faction.isRaidable()) {
            return;
        }

        double loss = getDtrLossOnDeath();

        faction.removeDtr(loss);

        saveAll();
    }

    public double getDtrLossOnDeath() {

        return plugin.getConfig()
                .getDouble(
                        "factions.dtr-loss-on-death",
                        1.0
                );
    }

    public double getStartingDtr() {

        return plugin.getConfig()
                .getDouble(
                        "factions.starting-dtr",
                        1.0
                );
    }

    public double getConfiguredMaxDtr() {

        return plugin.getConfig()
                .getDouble(
                        "factions.max-dtr",
                        5.0
                );
    }

    // =========================================================
    // DTR REGEN
    // =========================================================

    public void regenerateDtr() {

        if (!plugin.getConfig()
                .getBoolean(
                        "factions.dtr-regeneration",
                        true
                )) {
            return;
        }

        long now = System.currentTimeMillis();

        long minutes =
                plugin.getConfig()
                        .getLong(
                                "factions.dtr-regeneration-minutes",
                                10
                        );

        long interval =
                minutes * 60_000L;

        double amount =
                plugin.getConfig()
                        .getDouble(
                                "factions.dtr-regeneration-amount",
                                0.05
                        );

        for (Faction faction : factions.values()) {

            if (!faction.canRegenerateDtr()) {
                continue;
            }

            String key =
                    faction.getName().toLowerCase();

            long last =
                    lastDtrRegeneration.getOrDefault(
                            key,
                            faction.getLastDtrRegeneration()
                    );

            if (now - last < interval) {
                continue;
            }

            faction.addDtr(amount);

            lastDtrRegeneration.put(
                    key,
                    now
            );

            faction.setLastDtrRegeneration(now);
        }

        saveAll();
    }

    // =========================================================
    // DTR FREEZE
    // =========================================================

    public boolean freezeDtr(String factionName) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        faction.freezeDtr();

        saveAll();

        return true;
    }

    public boolean unfreezeDtr(String factionName) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        faction.unfreezeDtr();

        saveAll();

        return true;
    }

    // =========================================================
    // DTR ADMIN
    // =========================================================

    public boolean setDtr(
            String factionName,
            double amount
    ) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        faction.setDtr(amount);

        saveAll();

        return true;
    }

    public boolean addDtr(
            String factionName,
            double amount
    ) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        faction.addDtr(amount);

        saveAll();

        return true;
    }

    public boolean removeDtr(
            String factionName,
            double amount
    ) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return false;
        }

        faction.removeDtr(amount);

        saveAll();

        return true;
    }

    // =========================================================
    // DATABASE TABLES
    // =========================================================

    private void createPersistenceTables() {

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

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS faction_homes (
                        faction TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL
                    )
                    """);

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not create faction persistence tables: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD ALL
    // =========================================================

    public void loadAll() {

        factions.clear();
        playerFactions.clear();
        lastDtrRegeneration.clear();

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

        loadFactions(connection);
        loadFactionMembers(connection);
        loadRelations(connection);
        loadHomes(connection);
        loadStats(connection);
    }

    // =========================================================
    // LOAD FACTIONS
    // =========================================================

    private void loadFactions(
            Connection connection
    ) {

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
                    leader = UUID.fromString(
                            leaderString
                    );
                } catch (IllegalArgumentException exception) {
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
                                dtr,
                                getConfiguredMaxDtr()
                        );

                faction.setBalance(balance);

                String key =
                        name.toLowerCase();

                factions.put(
                        key,
                        faction
                );

                playerFactions.put(
                        leader,
                        key
                );

                long now =
                        System.currentTimeMillis();

                lastDtrRegeneration.put(
                        key,
                        now
                );

                faction.setLastDtrRegeneration(now);
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load factions: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD MEMBERS
    // =========================================================

    private void loadFactionMembers(
            Connection connection
    ) {

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
                } catch (IllegalArgumentException exception) {
                    continue;
                }

                String roleName =
                        result.getString("role");

                faction.addMember(uuid);

                playerFactions.put(
                        uuid,
                        faction.getName().toLowerCase()
                );

                if (roleName == null) {
                    continue;
                }

                try {

                    Faction.FactionRole role =
                            Faction.FactionRole.valueOf(
                                    roleName.toUpperCase()
                            );

                    switch (role) {

                        case LEADER:
                            break;

                        case CO_LEADER:
                            faction.promoteToCoLeader(uuid);
                            break;

                        case CAPTAIN:
                            faction.promoteToCaptain(uuid);
                            break;

                        case MEMBER:
                            break;
                    }

                } catch (IllegalArgumentException ignored) {

                    /*
                     * Compatibilidad con bases antiguas.
                     */
                    if ("OFFICER".equalsIgnoreCase(roleName)) {
                        faction.promoteToCaptain(uuid);
                    }
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load faction members: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD RELATIONS
    // =========================================================

    private void loadRelations(
            Connection connection
    ) {

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

                if (faction == null) {
                    continue;
                }

                faction.addAlly(
                        result.getString("ally")
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load faction allies: "
                            + exception.getMessage()
            );
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

                if (faction == null) {
                    continue;
                }

                faction.addEnemy(
                        result.getString("enemy")
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load faction enemies: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD HOMES
    // =========================================================

    private void loadHomes(
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

                if (plugin.getServer()
                        .getWorld(worldName) == null) {
                    continue;
                }

                Location location =
                        new Location(
                                plugin.getServer()
                                        .getWorld(worldName),
                                result.getDouble("x"),
                                result.getDouble("y"),
                                result.getDouble("z"),
                                result.getFloat("yaw"),
                                result.getFloat("pitch")
                        );

                faction.setHome(location);
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load faction homes: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD STATS
    // =========================================================

    private void loadStats(
            Connection connection
    ) {

        String sql =
                "SELECT uuid, kills, deaths FROM player_stats";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                UUID uuid;

                try {
                    uuid = UUID.fromString(
                            result.getString("uuid")
                    );
                } catch (IllegalArgumentException exception) {
                    continue;
                }

                kills.put(
                        uuid,
                        result.getInt("kills")
                );

                deaths.put(
                        uuid,
                        result.getInt("deaths")
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not load player stats: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // SAVE ALL
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

        boolean previousAutoCommit = true;

        try {

            previousAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            /*
             * IMPORTANTE:
             *
             * Solo borramos tablas secundarias.
             *
             * NO borramos "factions".
             *
             * Esto evita que SQLite haga CASCADE
             * sobre claims.
             */
            clearFactionChildData(connection);

            saveFactions(connection);
            saveMembers(connection);
            saveRelations(connection);
            saveHomes(connection);
            saveAllPlayerStats(connection);

            connection.commit();

        } catch (SQLException exception) {

            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                plugin.getLogger().warning(
                        "Could not rollback faction save: "
                                + rollbackException.getMessage()
                );
            }

            plugin.getLogger().warning(
                    "Could not save factions: "
                            + exception.getMessage()
            );

        } finally {

            try {
                connection.setAutoCommit(
                        previousAutoCommit
                );
            } catch (SQLException ignored) {
            }
        }
    }

    // =========================================================
    // CLEAR CHILD DATA
    // =========================================================

    private void clearFactionChildData(
            Connection connection
    ) throws SQLException {

        try (Statement statement =
                     connection.createStatement()) {

            /*
             * Estas tablas no tienen relación directa
             * destructiva con claims.
             */
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
        }
    }

    // =========================================================
    // SAVE FACTIONS
    // =========================================================

    private void saveFactions(
            Connection connection
    ) throws SQLException {

        String updateSql = """
                UPDATE factions
                SET leader = ?, dtr = ?, balance = ?
                WHERE name = ?
                """;

        String insertSql = """
                INSERT INTO factions
                (name, leader, dtr, balance)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement update =
                     connection.prepareStatement(updateSql);
             PreparedStatement insert =
                     connection.prepareStatement(insertSql)) {

            for (Faction faction : factions.values()) {

                update.setString(
                        1,
                        faction.getLeader().toString()
                );

                update.setDouble(
                        2,
                        faction.getDtr()
                );

                update.setDouble(
                        3,
                        faction.getBalance()
                );

                update.setString(
                        4,
                        faction.getName()
                );

                int affected =
                        update.executeUpdate();

                /*
                 * Si no existe, la creamos.
                 */
                if (affected == 0) {

                    insert.setString(
                            1,
                            faction.getName()
                    );

                    insert.setString(
                            2,
                            faction.getLeader().toString()
                    );

                    insert.setDouble(
                            3,
                            faction.getDtr()
                    );

                    insert.setDouble(
                            4,
                            faction.getBalance()
                    );

                    insert.executeUpdate();
                }
            }
        }
    }

    // =========================================================
    // SAVE MEMBERS
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

            for (Faction faction : factions.values()) {

                for (UUID uuid :
                        faction.getMembers()) {

                    Faction.FactionRole role =
                            faction.getRole(uuid);

                    if (role == null) {
                        role =
                                Faction.FactionRole.MEMBER;
                    }

                    statement.setString(
                            1,
                            faction.getName()
                    );

                    statement.setString(
                            2,
                            uuid.toString()
                    );

                    /*
                     * Persistimos solamente:
                     *
                     * LEADER
                     * CO_LEADER
                     * CAPTAIN
                     * MEMBER
                     *
                     * OFFICER nunca se guarda.
                     */
                    statement.setString(
                            3,
                            role.name()
                    );

                    statement.addBatch();
                }
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // SAVE RELATIONS
    // =========================================================

    private void saveRelations(
            Connection connection
    ) throws SQLException {

        String allySql = """
                INSERT INTO faction_allies
                (faction, ally)
                VALUES (?, ?)
                """;

        String enemySql = """
                INSERT INTO faction_enemies
                (faction, enemy)
                VALUES (?, ?)
                """;

        try (PreparedStatement ally =
                     connection.prepareStatement(allySql);
             PreparedStatement enemy =
                     connection.prepareStatement(enemySql)) {

            for (Faction faction : factions.values()) {

                for (String allyName :
                        faction.getAllies()) {

                    /*
                     * Solo guardamos relaciones con
                     * factions que realmente existen.
                     */
                    if (getFaction(allyName) == null) {
                        continue;
                    }

                    ally.setString(
                            1,
                            faction.getName()
                    );

                    ally.setString(
                            2,
                            allyName
                    );

                    ally.addBatch();
                }

                for (String enemyName :
                        faction.getEnemies()) {

                    if (getFaction(enemyName) == null) {
                        continue;
                    }

                    enemy.setString(
                            1,
                            faction.getName()
                    );

                    enemy.setString(
                            2,
                            enemyName
                    );

                    enemy.addBatch();
                }
            }

            ally.executeBatch();
            enemy.executeBatch();
        }
    }

    // =========================================================
    // SAVE HOMES
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

            for (Faction faction : factions.values()) {

                if (!faction.hasHome()) {
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

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    // =========================================================
    // SAVE ALL STATS
    // =========================================================

    private void saveAllPlayerStats(
            Connection connection
    ) throws SQLException {

        Set<UUID> players =
                new HashSet<>();

        players.addAll(kills.keySet());
        players.addAll(deaths.keySet());

        String updateSql = """
                UPDATE player_stats
                SET kills = ?, deaths = ?
                WHERE uuid = ?
                """;

        String insertSql = """
                INSERT INTO player_stats
                (uuid, kills, deaths)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement update =
                     connection.prepareStatement(updateSql);
             PreparedStatement insert =
                     connection.prepareStatement(insertSql)) {

            for (UUID uuid : players) {

                int playerKills =
                        kills.getOrDefault(uuid, 0);

                int playerDeaths =
                        deaths.getOrDefault(uuid, 0);

                update.setInt(
                        1,
                        playerKills
                );

                update.setInt(
                        2,
                        playerDeaths
                );

                update.setString(
                        3,
                        uuid.toString()
                );

                int affected =
                        update.executeUpdate();

                if (affected == 0) {

                    insert.setString(
                            1,
                            uuid.toString()
                    );

                    insert.setInt(
                            2,
                            playerKills
                    );

                    insert.setInt(
                            3,
                            playerDeaths
                    );

                    insert.executeUpdate();
                }
            }
        }
    }

    // =========================================================
    // SAVE PLAYER STATS
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

        String updateSql = """
                UPDATE player_stats
                SET kills = ?, deaths = ?
                WHERE uuid = ?
                """;

        String insertSql = """
                INSERT INTO player_stats
                (uuid, kills, deaths)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement update =
                     connection.prepareStatement(updateSql);
             PreparedStatement insert =
                     connection.prepareStatement(insertSql)) {

            int playerKills =
                    kills.getOrDefault(uuid, 0);

            int playerDeaths =
                    deaths.getOrDefault(uuid, 0);

            update.setInt(
                    1,
                    playerKills
            );

            update.setInt(
                    2,
                    playerDeaths
            );

            update.setString(
                    3,
                    uuid.toString()
            );

            int affected =
                    update.executeUpdate();

            if (affected == 0) {

                insert.setString(
                        1,
                        uuid.toString()
                );

                insert.setInt(
                        2,
                        playerKills
                );

                insert.setInt(
                        3,
                        playerDeaths
                );

                insert.executeUpdate();
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "Could not save player stats: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // DELETE ONE FACTION FROM DATABASE
    // =========================================================

    private void deleteFactionFromDatabase(
            String factionName
    ) {

        if (factionName == null) {
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

        boolean previousAutoCommit = true;

        try {

            previousAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            /*
             * Claims ya fueron eliminados por ClaimManager.
             *
             * Primero eliminamos las relaciones y miembros.
             */
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM faction_members WHERE faction = ?"
                         )) {

                statement.setString(
                        1,
                        factionName
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM faction_allies WHERE faction = ? OR ally = ?"
                         )) {

                statement.setString(
                        1,
                        factionName
                );

                statement.setString(
                        2,
                        factionName
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM faction_enemies WHERE faction = ? OR enemy = ?"
                         )) {

                statement.setString(
                        1,
                        factionName
                );

                statement.setString(
                        2,
                        factionName
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM faction_homes WHERE faction = ?"
                         )) {

                statement.setString(
                        1,
                        factionName
                );

                statement.executeUpdate();
            }

            /*
             * Ahora sí podemos eliminar la faction.
             *
             * Los claims ya no existen.
             */
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM factions WHERE name = ?"
                         )) {

                statement.setString(
                        1,
                        factionName
                );

                statement.executeUpdate();
            }

            connection.commit();

        } catch (SQLException exception) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            plugin.getLogger().warning(
                    "Could not delete faction from database: "
                            + exception.getMessage()
            );

        } finally {

            try {
                connection.setAutoCommit(
                        previousAutoCommit
                );
            } catch (SQLException ignored) {
            }
        }
    }
}