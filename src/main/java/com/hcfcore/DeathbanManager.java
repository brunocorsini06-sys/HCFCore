package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathbanManager {

    private final HCFCore plugin;

    private final Map<UUID, Long> deathbans =
            new HashMap<>();

    private final Map<UUID, Integer> lives =
            new HashMap<>();

    public DeathbanManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa las vidas del jugador.
     */
    public void setupPlayer(Player player) {

        if (player == null) {
            return;
        }

        lives.putIfAbsent(
                player.getUniqueId(),
                plugin.getConfig().getInt(
                        "deathban.starting-lives",
                        3
                )
        );
    }

    /**
     * Devuelve las vidas actuales.
     */
    public int getLives(Player player) {

        if (player == null) {
            return 0;
        }

        setupPlayer(player);

        return lives.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    /**
     * Añade vidas.
     */
    public void addLife(
            Player player,
            int amount
    ) {

        if (player == null || amount <= 0) {
            return;
        }

        setupPlayer(player);

        int current =
                getLives(player);

        lives.put(
                player.getUniqueId(),
                current + amount
        );
    }

    /**
     * Quita vidas.
     */
    public void removeLife(
            Player player,
            int amount
    ) {

        if (player == null || amount <= 0) {
            return;
        }

        setupPlayer(player);

        int newLives =
                Math.max(
                        0,
                        getLives(player) - amount
                );

        lives.put(
                player.getUniqueId(),
                newLives
        );
    }

    /**
     * Comprueba si está deathbaneado.
     */
    public boolean isDeathbanned(Player player) {

        if (player == null) {
            return false;
        }

        return isDeathbanned(
                player.getUniqueId()
        );
    }

    /**
     * Comprueba deathban mediante UUID.
     */
    public boolean isDeathbanned(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        Long expires =
                deathbans.get(uuid);

        if (expires == null) {
            return false;
        }

        // Deathban permanente.
        if (expires == -1L) {
            return true;
        }

        // Deathban expirado.
        if (System.currentTimeMillis() >= expires) {

            deathbans.remove(uuid);

            return false;
        }

        return true;
    }

    /**
     * Aplica un deathban y descuenta
     * una vida si está configurado.
     */
    public void deathban(Player player) {

        if (player == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "deathban.enabled",
                true
        )) {
            return;
        }

        setupPlayer(player);

        boolean removeLife =
                plugin.getConfig().getBoolean(
                        "deathban.remove-life",
                        true
                );

        /*
         * El DeathbanManager se encarga
         * de descontar la vida.
         */
        if (removeLife) {
            removeLife(player, 1);
        }

        int playerLives =
                getLives(player);

        boolean permanent =
                plugin.getConfig().getBoolean(
                        "deathban.permanent-at-zero-lives",
                        true
                );

        /*
         * Sin vidas = deathban permanente.
         */
        if (playerLives <= 0 && permanent) {

            deathbans.put(
                    player.getUniqueId(),
                    -1L
            );

            return;
        }

        int minutes =
                plugin.getConfig().getInt(
                        "deathban.minutes-per-death",
                        30
                );

        if (minutes <= 0) {
            minutes = 1;
        }

        long expires =
                System.currentTimeMillis()
                        + (minutes * 60_000L);

        deathbans.put(
                player.getUniqueId(),
                expires
        );
    }

    /**
     * Revive al jugador.
     */
    public boolean revive(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        if (!deathbans.containsKey(uuid)) {
            return false;
        }

        deathbans.remove(uuid);

        return true;
    }

    /**
     * Tiempo restante del deathban
     * en segundos.
     *
     * -1 = permanente.
     */
    public long getRemainingSeconds(
            Player player
    ) {

        if (player == null) {
            return 0;
        }

        Long expires =
                deathbans.get(
                        player.getUniqueId()
                );

        if (expires == null) {
            return 0;
        }

        if (expires == -1L) {
            return -1;
        }

        long remaining =
                expires - System.currentTimeMillis();

        if (remaining <= 0) {

            deathbans.remove(
                    player.getUniqueId()
            );

            return 0;
        }

        return (remaining + 999) / 1000;
    }

    /**
     * Devuelve el mapa de vidas.
     */
    public Map<UUID, Integer> getLivesMap() {
        return lives;
    }

    /**
     * Devuelve los deathbans.
     */
    public Map<UUID, Long> getDeathbans() {
        return deathbans;
    }

    /**
     * Guarda los datos.
     */
    public void saveAll() {
        // Persistencia próximamente.
    }
}