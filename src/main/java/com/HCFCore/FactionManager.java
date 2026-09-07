package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.*;

public class FactionManager {

    private final HCFCore plugin;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();

    public FactionManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    public Faction createFaction(Player player, String name) {

        if (name == null || name.length() < 3 || name.length() > 16) {
            return null;
        }

        if (getFaction(player) != null) {
            return null;
        }

        if (factions.containsKey(name.toLowerCase())) {
            return null;
        }

        double startingDtr =
                plugin.getConfig().getDouble(
                        "factions.starting-dtr",
                        1.0
                );

        Faction faction =
                new Faction(
                        name,
                        player.getUniqueId(),
                        startingDtr
                );

        factions.put(
                name.toLowerCase(),
                faction
        );

        playerFactions.put(
                player.getUniqueId(),
                name.toLowerCase()
        );

        return faction;
    }

    public void disband(Faction faction) {

        if (faction == null) {
            return;
        }

        for (UUID uuid :
                new HashSet<>(faction.getMembers())) {

            playerFactions.remove(uuid);
        }

        factions.remove(
                faction.getName().toLowerCase()
        );
    }

    public Faction getFaction(Player player) {
        return getFaction(player.getUniqueId());
    }

    public Faction getFaction(UUID uuid) {

        String factionName =
                playerFactions.get(uuid);

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
                name.toLowerCase()
        );
    }

    public boolean joinFaction(
            Player player,
            String name
    ) {

        Faction faction =
                getFaction(name);

        if (faction == null) {
            return false;
        }

        if (getFaction(player) != null) {
            return false;
        }

        if (!hasInvite(player, name)) {
            return false;
        }

        int maxMembers =
                plugin.getConfig().getInt(
                        "factions.max-members",
                        20
                );

        if (faction.getMembers().size()
                >= maxMembers) {

            return false;
        }

        faction.addMember(
                player.getUniqueId()
        );

        playerFactions.put(
                player.getUniqueId(),
                faction.getName().toLowerCase()
        );

        removeInvite(player);

        return true;
    }

    public void leaveFaction(Player player) {

        Faction faction =
                getFaction(player);

        if (faction == null) {
            return;
        }

        if (faction.isLeader(
                player.getUniqueId())) {

            return;
        }

        faction.removeMember(
                player.getUniqueId()
        );

        playerFactions.remove(
                player.getUniqueId()
        );
    }

    public boolean invite(
            Player target,
            Faction faction
    ) {

        if (target == null || faction == null) {
            return false;
        }

        invites.put(
                target.getUniqueId(),
                faction.getName().toLowerCase()
        );

        return true;
    }

    public boolean hasInvite(
            Player player,
            String factionName
    ) {

        String invite =
                invites.get(
                        player.getUniqueId()
                );

        return invite != null
                && invite.equalsIgnoreCase(
                        factionName
                );
    }

    public void removeInvite(Player player) {

        invites.remove(
                player.getUniqueId()
        );
    }

    public void promote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null) {
            return;
        }

        faction.promote(uuid);
    }

    public void demote(
            Faction faction,
            UUID uuid
    ) {

        if (faction == null) {
            return;
        }

        faction.demote(uuid);
    }

    public Collection<Faction> getFactions() {
        return factions.values();
    }

    public Map<String, Faction> getFactionMap() {
        return factions;
    }

    public void regenerateDtr() {

        if (!plugin.getConfig().getBoolean(
                "factions.dtr-regeneration",
                true
        )) {
            return;
        }

        double maxDtr =
                plugin.getConfig().getDouble(
                        "factions.max-dtr",
                        5.0
                );

        for (Faction faction :
                factions.values()) {

            if (faction.getDtr() < maxDtr) {

                faction.setDtr(
                        Math.min(
                                maxDtr,
                                faction.getDtr() + 0.05
                        )
                );
            }
        }
    }

    public void saveAll() {
        /*
         * La persistencia de factions
         * se añadirá en la parte de base de datos.
         */
    }
}