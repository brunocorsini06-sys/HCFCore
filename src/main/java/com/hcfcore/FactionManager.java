package com.hcfcore;

import org.bukkit.Location;
import org.bukkit.entity.Player;

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

        double startingDtr = plugin.getConfig().getDouble(
                "factions.starting-dtr",
                1.0
        );

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

        /*
         * Eliminamos claims de la faction.
         */
        plugin.getClaimManager()
                .removeFactionClaims(faction);

        factions.remove(key);

        lastDtrRegeneration.remove(key);

        /*
         * Eliminamos invitaciones relacionadas.
         */
        invites.entrySet().removeIf(
                entry -> entry.getValue().equalsIgnoreCase(key)
        );

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

        /*
         * El líder no puede abandonar.
         */
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

        /*
         * Solo líder u officer.
         */
        if (!isLeader(player)
                && !isOfficer(player)) {

            return false;
        }

        Location location =
                player.getLocation();

        /*
         * El faction home DEBE estar dentro
         * de un claim de la propia faction.
         */
        Faction claimedFaction =
                plugin.getClaimManager()
                        .getFactionAt(location);

        /*
         * Wilderness.
         */
        if (claimedFaction == null) {
            return false;
        }

        /*
         * Claim de otra faction.
         */
        if (!claimedFaction.getName()
                .equalsIgnoreCase(
                        faction.getName()
                )) {

            return false;
        }

        /*
         * Guardamos la ubicación completa.
         */
        faction.setHome(location);

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

    /**
     * Procesa la muerte de un jugador.
     *
     * Devuelve true si la faction perdió DTR.
     */
    public boolean handleDeath(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        /*
         * Jugador sin faction:
         * no pierde DTR.
         */
        if (faction == null) {
            return false;
        }

        /*
         * Si ya está raidable, no puede
         * perder más DTR.
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

        /*
         * Evitamos valores inválidos.
         */
        if (Double.isNaN(loss)
                || Double.isInfinite(loss)
                || loss <= 0.0) {

            return false;
        }

        double oldDtr =
                faction.getDtr();

        faction.removeDtr(loss);

        /*
         * Confirmamos que realmente perdió DTR.
         */
        return faction.getDtr() < oldDtr;
    }

    /**
     * Comprueba si una faction está Raidable.
     */
    public boolean isRaidable(Faction faction) {

        return faction != null
                && faction.isRaidable();
    }

    /**
     * Comprueba si la faction del jugador
     * está Raidable.
     */
    public boolean isRaidable(Player player) {

        if (player == null) {
            return false;
        }

        Faction faction =
                getFaction(player);

        return isRaidable(faction);
    }

    // =========================================================
    // DTR
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

        if (regenerationAmount <= 0) {
            regenerationAmount = 0.05;
        }

        double maxDtr =
                plugin.getConfig()
                        .getDouble(
                                "factions.max-dtr",
                                5.0
                        );

        long now =
                System.currentTimeMillis();

        for (Faction faction :
                factions.values()) {

            if (faction == null) {
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

            if (faction.getDtr() >= maxDtr) {
                continue;
            }

            double newDtr =
                    Math.min(
                            maxDtr,
                            faction.getDtr()
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

        faction.setDtr(dtr);
    }

    public void addDtr(
            Faction faction,
            double amount
    ) {

        if (faction == null || amount <= 0) {
            return;
        }

        faction.addDtr(amount);
    }

    public void removeDtr(
            Faction faction,
            double amount
    ) {

        if (faction == null || amount <= 0) {
            return;
        }

        faction.removeDtr(amount);
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
    // PERSISTENCIA
    // =========================================================

    public void saveAll() {

        /*
         * La persistencia SQLite/YAML
         * la añadiremos en la siguiente fase.
         */
    }
}