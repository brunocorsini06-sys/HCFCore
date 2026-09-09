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

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    // =========================================================
    // FACTIONS
    // =========================================================

    public Faction getFaction(String name) {
        if (name == null) {
            return null;
        }

        return factions.get(name.toLowerCase(Locale.ROOT));
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

    public Collection<Faction> getFactions() {
        return Collections.unmodifiableCollection(factions.values());
    }

    public boolean exists(String name) {
        return getFaction(name) != null;
    }

    // =========================================================
    // CREAR
    // =========================================================

    public boolean createFaction(String name, UUID leader) {

        if (name == null || name.isBlank() || leader == null) {
            return false;
        }

        name = name.trim();

        if (name.length() < 2 || name.length() > 16) {
            return false;
        }

        if (!name.matches("[A-Za-z0-9_]+")) {
            return false;
        }

        if (getFaction(name) != null) {
            return false;
        }

        if (getFaction(leader) != null) {
            return false;
        }

        double startingDtr = plugin.getConfig().getDouble(
                "factions.starting-dtr",
                1.0
        );

        double maxDtr = plugin.getConfig().getDouble(
                "factions.max-dtr",
                5.0
        );

        Faction faction = new Faction(
                name,
                leader,
                startingDtr,
                maxDtr
        );

        factions.put(
                name.toLowerCase(Locale.ROOT),
                faction
        );

        playerFactions.put(
                leader,
                name
        );

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // DISBAND
    // =========================================================

    public boolean disbandFaction(String name) {

        Faction faction = getFaction(name);

        if (faction == null) {
            return false;
        }

        for (UUID uuid : new HashSet<>(faction.getMembers())) {
            playerFactions.remove(uuid);
        }

        if (plugin.getClaimManager() != null) {
            plugin.getClaimManager().removeFactionClaims(
                    faction.getName()
            );
        }

        factions.remove(
                faction.getName().toLowerCase(Locale.ROOT)
        );

        try (Connection connection =
                     plugin.getDatabaseManager().getConnection()) {

            try (PreparedStatement ps =
                         connection.prepareStatement(
                                 "DELETE FROM factions WHERE name = ?"
                         )) {

                ps.setString(1, faction.getName());
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().severe(
                    "Could not delete faction " +
                            faction.getName() +
                            ": " +
                            e.getMessage()
            );
        }

        return true;
    }

    public boolean disbandFaction(Faction faction) {

        if (faction == null) {
            return false;
        }

        return disbandFaction(faction.getName());
    }

    // =========================================================
    // INVITES
    // =========================================================

    public boolean invitePlayer(
            UUID inviter,
            UUID target
    ) {

        if (inviter == null || target == null) {
            return false;
        }

        Faction faction = getFaction(inviter);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                inviter,
                Faction.FactionPermission.INVITE
        )) {
            return false;
        }

        if (getFaction(target) != null) {
            return false;
        }

        int maxMembers = plugin.getConfig().getInt(
                "factions.max-members",
                20
        );

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        invites.computeIfAbsent(
                target,
                k -> new HashMap<>()
        ).put(
                faction.getName().toLowerCase(Locale.ROOT),
                System.currentTimeMillis()
        );

        return true;
    }

    public boolean hasInvite(
            UUID player,
            String factionName
    ) {

        if (player == null || factionName == null) {
            return false;
        }

        Map<String, Long> playerInvites =
                invites.get(player);

        if (playerInvites == null) {
            return false;
        }

        Long timestamp = playerInvites.get(
                factionName.toLowerCase(Locale.ROOT)
        );

        if (timestamp == null) {
            return false;
        }

        long duration = plugin.getConfig().getLong(
                "factions.invite-expiration-seconds",
                120
        ) * 1000L;

        if (duration > 0L &&
                System.currentTimeMillis() - timestamp > duration) {

            playerInvites.remove(
                    factionName.toLowerCase(Locale.ROOT)
            );

            return false;
        }

        return true;
    }

    public boolean joinFaction(
            UUID player,
            String factionName
    ) {

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

        int maxMembers = plugin.getConfig().getInt(
                "factions.max-members",
                20
        );

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        faction.addMember(player);

        playerFactions.put(
                player,
                faction.getName()
        );

        Map<String, Long> playerInvites =
                invites.get(player);

        if (playerInvites != null) {
            playerInvites.remove(
                    faction.getName().toLowerCase(Locale.ROOT)
            );
        }

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // LEAVE
    // =========================================================

    public boolean leaveFaction(UUID player) {

        if (player == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (faction.isLeader(player)) {
            return false;
        }

        faction.removeMember(player);

        playerFactions.remove(player);

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // KICK
    // =========================================================

    public boolean kickPlayer(
            UUID kicker,
            UUID target
    ) {

        if (kicker == null || target == null) {
            return false;
        }

        Faction faction = getFaction(kicker);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                kicker,
                Faction.FactionPermission.KICK
        )) {
            return false;
        }

        if (!faction.isMember(target)) {
            return false;
        }

        if (faction.isLeader(target)) {
            return false;
        }

        Faction.FactionRole kickerRole =
                faction.getRole(kicker);

        Faction.FactionRole targetRole =
                faction.getRole(target);

        if (kickerRole == null || targetRole == null) {
            return false;
        }

        if (targetRole.isAtLeast(kickerRole)) {
            return false;
        }

        faction.removeMember(target);

        playerFactions.remove(target);

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // PROMOVER
    // =========================================================

    public boolean promote(
            UUID promoter,
            UUID target
    ) {

        if (promoter == null || target == null) {
            return false;
        }

        Faction faction = getFaction(promoter);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                promoter,
                Faction.FactionPermission.PROMOTE
        )) {
            return false;
        }

        if (!faction.isMember(target)) {
            return false;
        }

        if (faction.isLeader(target)) {
            return false;
        }

        Faction.FactionRole promoterRole =
                faction.getRole(promoter);

        Faction.FactionRole targetRole =
                faction.getRole(target);

        if (promoterRole == null || targetRole == null) {
            return false;
        }

        if (!promoterRole.isHigherThan(targetRole)) {
            return false;
        }

        boolean changed = false;

        if (targetRole == Faction.FactionRole.MEMBER) {
            changed = faction.promoteToCaptain(target);
        } else if (targetRole == Faction.FactionRole.CAPTAIN) {
            changed = faction.promoteToCoLeader(target);
        }

        if (changed) {
            saveFaction(faction);
        }

        return changed;
    }

    // =========================================================
    // DEMOVER
    // =========================================================

    public boolean demote(
            UUID demoter,
            UUID target
    ) {

        if (demoter == null || target == null) {
            return false;
        }

        Faction faction = getFaction(demoter);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                demoter,
                Faction.FactionPermission.DEMOTE
        )) {
            return false;
        }

        if (!faction.isMember(target)) {
            return false;
        }

        if (faction.isLeader(target)) {
            return false;
        }

        Faction.FactionRole demoterRole =
                faction.getRole(demoter);

        Faction.FactionRole targetRole =
                faction.getRole(target);

        if (demoterRole == null || targetRole == null) {
            return false;
        }

        if (!demoterRole.isHigherThan(targetRole)) {
            return false;
        }

        boolean changed = false;

        if (targetRole == Faction.FactionRole.CO_LEADER) {
            changed = faction.demoteFromCoLeader(target);
        } else if (targetRole == Faction.FactionRole.CAPTAIN) {
            changed = faction.demoteFromCaptain(target);
        }

        if (changed) {
            saveFaction(faction);
        }

        return changed;
    }

    // =========================================================
    // ROLES
    // =========================================================

    public Faction.FactionRole getRole(UUID player) {

        Faction faction = getFaction(player);

        if (faction == null) {
            return null;
        }

        return faction.getRole(player);
    }

    // =========================================================
    // PERMISOS
    // =========================================================

    public boolean hasPermission(
            UUID player,
            Faction.FactionPermission permission
    ) {

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        return faction.hasPermission(
                player,
                permission
        );
    }

    // =========================================================
    // HOME
    // =========================================================

    public boolean setHome(
            UUID player,
            Location location
    ) {

        if (player == null || location == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                player,
                Faction.FactionPermission.SET_HOME
        )) {
            return false;
        }

        faction.setHome(location);

        saveFaction(faction);

        return true;
    }

    public Location getHome(String factionName) {

        Faction faction = getFaction(factionName);

        if (faction == null) {
            return null;
        }

        return faction.getHome();
    }

    public Location getHome(UUID player) {

        Faction faction = getFaction(player);

        if (faction == null) {
            return null;
        }

        return faction.getHome();
    }

    // =========================================================
    // ALLIES
    // =========================================================

    public boolean addAlly(
            UUID player,
            String targetName
    ) {

        if (player == null || targetName == null) {
            return false;
        }

        Faction faction = getFaction(player);
        Faction target = getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        if (!faction.hasPermission(
                player,
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.addAlly(target.getName());

        faction.getEnemies().remove(
                target.getName().toLowerCase(Locale.ROOT)
        );

        saveFaction(faction);

        return true;
    }

    public boolean removeAlly(
            UUID player,
            String targetName
    ) {

        if (player == null || targetName == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                player,
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.removeAlly(targetName);

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // ENEMIES
    // =========================================================

    public boolean addEnemy(
            UUID player,
            String targetName
    ) {

        if (player == null || targetName == null) {
            return false;
        }

        Faction faction = getFaction(player);
        Faction target = getFaction(targetName);

        if (faction == null || target == null) {
            return false;
        }

        if (faction == target) {
            return false;
        }

        if (!faction.hasPermission(
                player,
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.addEnemy(target.getName());

        faction.getAllies().remove(
                target.getName().toLowerCase(Locale.ROOT)
        );

        saveFaction(faction);

        return true;
    }

    public boolean removeEnemy(
            UUID player,
            String targetName
    ) {

        if (player == null || targetName == null) {
            return false;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return false;
        }

        if (!faction.hasPermission(
                player,
                Faction.FactionPermission.MANAGE_RELATIONS
        )) {
            return false;
        }

        faction.removeEnemy(targetName);

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // DTR - MUERTE
    // =========================================================

    public boolean handleDeath(Player player) {

        if (player == null) {
            return false;
        }

        return handleDeath(player.getUniqueId());
    }

    public boolean handleDeath(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        Faction faction = getFaction(uuid);

        if (faction == null) {
            return false;
        }

        double loss = plugin.getConfig().getDouble(
                "factions.dtr-loss-on-death",
                1.0
        );

        if (loss <= 0.0) {
            return false;
        }

        double oldDtr = faction.getDtr();

        faction.removeDtr(loss);

        double newDtr = faction.getDtr();

        if (newDtr >= oldDtr) {
            return false;
        }

        saveFaction(faction);

        return true;
    }

    // =========================================================
    // DTR - REGENERACIÓN
    // =========================================================

    public void regenerateDtr() {

        long now = System.currentTimeMillis();

        long intervalSeconds = plugin.getConfig().getLong(
                "factions.dtr-regeneration.interval-seconds",
                3600L
        );

        double amount = plugin.getConfig().getDouble(
                "factions.dtr-regeneration.amount",
                0.5
        );

        if (intervalSeconds <= 0L) {
            intervalSeconds = 3600L;
        }

        if (amount <= 0.0) {
            return;
        }

        long intervalMillis = intervalSeconds * 1000L;

        for (Faction faction : factions.values()) {

            if (faction == null) {
                continue;
            }

            if (!faction.canRegenerateDtr()) {
                continue;
            }

            long lastRegeneration =
                    faction.getLastDtrRegeneration();

            if (now - lastRegeneration < intervalMillis) {
                continue;
            }

            double oldDtr = faction.getDtr();

            faction.addDtr(amount);

            double newDtr = faction.getDtr();

            if (newDtr <= oldDtr) {
                continue;
            }

            faction.setLastDtrRegeneration(now);

            saveFaction(faction);
        }
    }

    // =========================================================
    // ESTADÍSTICAS
    // =========================================================

    public void addKill(UUID player) {

        if (player == null) {
            return;
        }

        kills.merge(
                player,
                1,
                Integer::sum
        );

        saveStats(player);
    }

    public void addDeath(UUID player) {

        if (player == null) {
            return;
        }

        deaths.merge(
                player,
                1,
                Integer::sum
        );

        saveStats(player);
    }

    public int getKills(UUID player) {

        if (player == null) {
            return 0;
        }

        return kills.getOrDefault(
                player,
                0
        );
    }

    public int getDeaths(UUID player) {

        if (player == null) {
            return 0;
        }

        return deaths.getOrDefault(
                player,
                0
        );
    }

    // =========================================================
    // GUARDAR STATS
    // =========================================================

    private void saveStats(UUID player) {

        try (Connection connection =
                     plugin.getDatabaseManager().getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(
                             "INSERT INTO player_stats " +
                                     "(uuid, kills, deaths) " +
                                     "VALUES (?, ?, ?) " +
                                     "ON CONFLICT(uuid) DO UPDATE SET " +
                                     "kills = excluded.kills, " +
                                     "deaths = excluded.deaths"
                     )) {

            ps.setString(
                    1,
                    player.toString()
            );

            ps.setInt(
                    2,
                    getKills(player)
            );

            ps.setInt(
                    3,
                    getDeaths(player)
            );

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe(
                    "Could not save player stats: " +
                            e.getMessage()
            );
        }
    }

    // =========================================================
    // GUARDAR FACTION
    // =========================================================

    public void saveFaction(Faction faction) {

        if (faction == null) {
            return;
        }

        try (Connection connection =
                     plugin.getDatabaseManager().getConnection()) {

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "INSERT INTO factions " +
                                             "(name, leader, dtr, max_dtr, " +
                                             "dtr_frozen, last_dtr_regeneration, balance) " +
                                             "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                             "ON CONFLICT(name) DO UPDATE SET " +
                                             "leader = excluded.leader, " +
                                             "dtr = excluded.dtr, " +
                                             "max_dtr = excluded.max_dtr, " +
                                             "dtr_frozen = excluded.dtr_frozen, " +
                                             "last_dtr_regeneration = excluded.last_dtr_regeneration, " +
                                             "balance = excluded.balance"
                             )) {

                    ps.setString(
                            1,
                            faction.getName()
                    );

                    ps.setString(
                            2,
                            faction.getLeader() == null
                                    ? null
                                    : faction.getLeader().toString()
                    );

                    ps.setDouble(
                            3,
                            faction.getDtr()
                    );

                    ps.setDouble(
                            4,
                            faction.getMaxDtr()
                    );

                    ps.setBoolean(
                            5,
                            faction.isDtrFrozen()
                    );

                    ps.setLong(
                            6,
                            faction.getLastDtrRegeneration()
                    );

                    ps.setDouble(
                            7,
                            faction.getBalance()
                    );

                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "DELETE FROM faction_members " +
                                             "WHERE faction = ?"
                             )) {

                    ps.setString(
                            1,
                            faction.getName()
                    );

                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "INSERT INTO faction_members " +
                                             "(faction, uuid, role) " +
                                             "VALUES (?, ?, ?)"
                             )) {

                    for (UUID uuid :
                            faction.getMembers()) {

                        Faction.FactionRole role =
                                faction.getRole(uuid);

                        if (role == null) {
                            continue;
                        }

                        ps.setString(
                                1,
                                faction.getName()
                        );

                        ps.setString(
                                2,
                                uuid.toString()
                        );

                        ps.setString(
                                3,
                                role.name()
                        );

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "DELETE FROM faction_allies " +
                                             "WHERE faction = ?"
                             )) {

                    ps.setString(
                            1,
                            faction.getName()
                    );

                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "INSERT INTO faction_allies " +
                                             "(faction, ally) VALUES (?, ?)"
                             )) {

                    for (String ally :
                            faction.getAllies()) {

                        ps.setString(
                                1,
                                faction.getName()
                        );

                        ps.setString(
                                2,
                                ally
                        );

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "DELETE FROM faction_enemies " +
                                             "WHERE faction = ?"
                             )) {

                    ps.setString(
                            1,
                            faction.getName()
                    );

                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "INSERT INTO faction_enemies " +
                                             "(faction, enemy) VALUES (?, ?)"
                             )) {

                    for (String enemy :
                            faction.getEnemies()) {

                        ps.setString(
                                1,
                                faction.getName()
                        );

                        ps.setString(
                                2,
                                enemy
                        );

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                try (PreparedStatement ps =
                             connection.prepareStatement(
                                     "DELETE FROM faction_homes " +
                                             "WHERE faction = ?"
                             )) {

                    ps.setString(
                            1,
                            faction.getName()
                    );

                    ps.executeUpdate();
                }

                Location home = faction.getHome();

                if (home != null &&
                        home.getWorld() != null) {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(
                                         "INSERT INTO faction_homes " +
                                                 "(faction, world, x, y, z, yaw, pitch) " +
                                                 "VALUES (?, ?, ?, ?, ?, ?, ?)"
                                 )) {

                        ps.setString(
                                1,
                                faction.getName()
                        );

                        ps.setString(
                                2,
                                home.getWorld().getName()
                        );

                        ps.setDouble(
                                3,
                                home.getX()
                        );

                        ps.setDouble(
                                4,
                                home.getY()
                        );

                        ps.setDouble(
                                5,
                                home.getZ()
                        );

                        ps.setFloat(
                                6,
                                home.getYaw()
                        );

                        ps.setFloat(
                                7,
                                home.getPitch()
                        );

                        ps.executeUpdate();
                    }
                }

                connection.commit();

            } catch (SQLException e) {

                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }

                throw e;
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not save faction " +
                            faction.getName() +
                            ": " +
                            e.getMessage()
            );
        }
    }

    // =========================================================
    // GUARDAR TODO
    // =========================================================

    public void saveAll() {

        for (Faction faction :
                factions.values()) {

            saveFaction(faction);
        }

        for (UUID uuid :
                new HashSet<>(
                        new HashSet<>(kills.keySet())
                )) {

            saveStats(uuid);
        }

        for (UUID uuid :
                new HashSet<>(
                        new HashSet<>(deaths.keySet())
                )) {

            if (!kills.containsKey(uuid)) {
                saveStats(uuid);
            }
        }
    }

    // =========================================================
    // CARGAR TODO
    // =========================================================

    public void loadAll() {

        factions.clear();
        playerFactions.clear();
        kills.clear();
        deaths.clear();

        try (Connection connection =
                     plugin.getDatabaseManager().getConnection()) {

            try (PreparedStatement ps =
                         connection.prepareStatement(
                                 "SELECT name, leader, dtr, max_dtr, " +
                                         "dtr_frozen, last_dtr_regeneration, balance " +
                                         "FROM factions"
                         );
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String name =
                            rs.getString("name");

                    String leaderString =
                            rs.getString("leader");

                    UUID leader = null;

                    if (leaderString != null) {
                        try {
                            leader = UUID.fromString(
                                    leaderString
                            );
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    double dtr =
                            rs.getDouble("dtr");

                    double maxDtr =
                            rs.getDouble("max_dtr");

                    Faction faction =
                            new Faction(
                                    name,
                                    leader,
                                    dtr,
                                    maxDtr
                            );

                    faction.setBalance(
                            rs.getDouble("balance")
                    );

                    faction.setDtrFrozen(
                            rs.getBoolean("dtr_frozen")
                    );

                    faction.setLastDtrRegeneration(
                            Math.max(
                                    0L,
                                    rs.getLong(
                                            "last_dtr_regeneration"
                                    )
                            )
                    );

                    factions.put(
                            name.toLowerCase(Locale.ROOT),
                            faction
                    );

                    if (leader != null) {
                        playerFactions.put(
                                leader,
                                name
                        );
                    }
                }
            }

            loadMembers(connection);
            loadRelations(connection);
            loadHomes(connection);
            loadStats(connection);

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not load factions: " +
                            e.getMessage()
            );
        }
    }

    // =========================================================
    // CARGAR MIEMBROS
    // =========================================================

    private void loadMembers(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT faction, uuid, role " +
                                     "FROM faction_members"
                     );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String factionName =
                        rs.getString("faction");

                Faction faction =
                        getFaction(factionName);

                if (faction == null) {
                    continue;
                }

                UUID uuid;

                try {
                    uuid = UUID.fromString(
                            rs.getString("uuid")
                    );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                String role =
                        rs.getString("role");

                if (role == null) {
                    continue;
                }

                role = role.toUpperCase(
                        Locale.ROOT
                );

                faction.addMember(uuid);

                switch (role) {

                    case "LEADER":
                        break;

                    case "CO_LEADER":
                    case "CO-LEADER":

                        faction.promoteToCaptain(uuid);
                        faction.promoteToCoLeader(uuid);
                        break;

                    case "CAPTAIN":
                    case "OFFICER":

                        faction.promoteToCaptain(uuid);
                        break;

                    case "MEMBER":
                    default:
                        break;
                }

                playerFactions.put(
                        uuid,
                        faction.getName()
                );
            }
        }
    }

    // =========================================================
    // CARGAR RELACIONES
    // =========================================================

    private void loadRelations(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT faction, ally " +
                                     "FROM faction_allies"
                     );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(
                                rs.getString("faction")
                        );

                if (faction != null) {
                    faction.addAlly(
                            rs.getString("ally")
                    );
                }
            }
        }

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT faction, enemy " +
                                     "FROM faction_enemies"
                     );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(
                                rs.getString("faction")
                        );

                if (faction != null) {
                    faction.addEnemy(
                            rs.getString("enemy")
                    );
                }
            }
        }
    }

    // =========================================================
    // CARGAR HOMES
    // =========================================================

    private void loadHomes(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT faction, world, x, y, z, yaw, pitch " +
                                     "FROM faction_homes"
                     );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Faction faction =
                        getFaction(
                                rs.getString("faction")
                        );

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

                Location location =
                        new Location(
                                world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );

                faction.setHome(location);
            }
        }
    }

    // =========================================================
    // CARGAR STATS
    // =========================================================

    private void loadStats(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT uuid, kills, deaths " +
                                     "FROM player_stats"
                     );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                UUID uuid;

                try {
                    uuid = UUID.fromString(
                            rs.getString("uuid")
                    );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                kills.put(
                        uuid,
                        rs.getInt("kills")
                );

                deaths.put(
                        uuid,
                        rs.getInt("deaths")
                );
            }
        }
    }
}