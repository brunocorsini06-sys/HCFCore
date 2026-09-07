package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.*;

public class FactionManager {

    private final HCFCore plugin;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();

    /*
     * Estadísticas individuales
     */
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    /*
     * Última regeneración de DTR por faction.
     */
    private final Map<String, Long> lastDtrRegeneration = new HashMap<>();

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // FACTIONS
    // =========================================================

    public Faction createFaction(Player player, String name) {

        if (player == null || name == null) {
            return null;
        }

        name = name.trim();

        if (name.length() < 3 || name.length() > 16) {
            return null;
        }

        if (getFaction(player) != null) {
            return null;
        }

        if (factions.containsKey(name.toLowerCase(Locale.ROOT))) {
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

        String key = name.toLowerCase(Locale.ROOT);

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

    public void disband(Faction faction) {

        if (faction == null) {
            return;
        }

        String key = faction.getName().toLowerCase(Locale.ROOT);

        for (UUID uuid : new HashSet<>(faction.getMembers())) {
            playerFactions.remove(uuid);
        }

        factions.remove(key);
        lastDtrRegeneration.remove(key);
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

        return factions.get(factionName);
    }

    public Faction getFaction(String name) {

        if (name == null) {
            return null;
        }

        return factions.get(
                name.toLowerCase(Locale.ROOT)
        );
    }

    public boolean joinFaction(
            Player player,
            String name
    ) {

        if (player == null || name == null) {
            return false;
        }

        Faction faction = getFaction(name);

        if (faction == null) {
            return false;
        }

        if (getFaction(player) != null) {
            return false;
        }

        if (!hasInvite(player, name)) {
            return false;
        }

        int maxMembers = plugin.getConfig().getInt(
                "factions.max-members",
                20
        );

        if (faction.getMembers().size() >= maxMembers) {
            return false;
        }

        faction.addMember(
                player.getUniqueId()
        );

        playerFactions.put(
                player.getUniqueId(),
                faction.getName().toLowerCase(Locale.ROOT)
        );

        removeInvite(player);

        return true;
    }

    public void leaveFaction(Player player) {

        if (player == null) {
            return;
        }

        Faction faction = getFaction(player);

        if (faction == null) {
            return;
        }

        /*
         * El líder no puede abandonar directamente.
         */
        if (faction.isLeader(player.getUniqueId())) {
            return;
        }

        faction.removeMember(
                player.getUniqueId()
        );

        playerFactions.remove(
                player.getUniqueId()
        );
    }

    // =========================================================
    // INVITACIONES
    // =========================================================

    public boolean invite(
            Player target,
            Faction faction
    ) {

        if (target == null || faction == null) {
            return false;
        }

        invites.put(
                target.getUniqueId(),
                faction.getName().toLowerCase(Locale.ROOT)
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

        String invite = invites.get(
                player.getUniqueId()
        );

        return invite != null
                && invite.equalsIgnoreCase(factionName);
    }

    public void removeInvite(Player player) {

        if (player == null) {
            return;
        }

        invites.remove(
                player.getUniqueId()
        );
    }

    // =========================================================
    // RANKS
    // =========================================================

    public void promote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null || uuid == null) {
            return;
        }

        faction.promote(uuid);
    }

    public void demote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null || uuid == null) {
            return;
        }

        faction.demote(uuid);
    }

    // =========================================================
    // KILLS / DEATHS
    // =========================================================

    public void addKill(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        kills.put(
                uuid,
                getKills(player) + 1
        );
    }

    public void addDeath(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

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
    // DTR
    // =========================================================

    public void regenerateDtr() {

        if (!plugin.getConfig().getBoolean(
                "factions.dtr-regeneration",
                true
        )) {
            return;
        }

        /*
         * Cuánto tiempo debe pasar entre regeneraciones.
         */
        int regenerationMinutes = plugin.getConfig().getInt(
                "factions.dtr-regeneration-minutes",
                10
        );

        if (regenerationMinutes <= 0) {
            regenerationMinutes = 10;
        }

        long regenerationInterval =
                regenerationMinutes * 60L * 1000L;

        /*
         * Cantidad de DTR recuperada por ciclo.
         */
        double regenerationAmount = 0.05;

        /*
         * DTR máximo.
         */
        double maxDtr = plugin.getConfig().getDouble(
                "factions.max-dtr",
                5.0
        );

        long now = System.currentTimeMillis();

        for (Faction faction : factions.values()) {

            if (faction == null) {
                continue;
            }

            String key =
                    faction.getName().toLowerCase(Locale.ROOT);

            long last =
                    lastDtrRegeneration.getOrDefault(
                            key,
                            now
                    );

            /*
             * Todavía no pasó el tiempo necesario.
             */
            if (now - last < regenerationInterval) {
                continue;
            }

            /*
             * Actualizamos el timestamp aunque
             * la faction ya esté al máximo.
             */
            lastDtrRegeneration.put(
                    key,
                    now
            );

            if (faction.getDtr() >= maxDtr) {
                continue;
            }

            double newDtr = Math.min(
                    maxDtr,
                    faction.getDtr() + regenerationAmount
            );

            faction.setDtr(newDtr);
        }
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    public Collection<Faction> getFactions() {
        return factions.values();
    }

    public Map<String, Faction> getFactionMap() {
        return factions;
    }

    // =========================================================
    // PERSISTENCIA
    // =========================================================

    public void saveAll() {

        /*
         * La persistencia se añadirá posteriormente.
         *
         * Actualmente las factions viven en memoria.
         */
    }
}