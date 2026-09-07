package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final HCFCore plugin;

    private final Map<UUID, Long> combatTags =
            new HashMap<>();

    public CombatManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // TAG
    // =========================================================

    /**
     * Pone al jugador en combate.
     *
     * Si ya estaba en combate, el tiempo se reinicia.
     */
    public void tag(Player player) {

        if (player == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "combat.enabled",
                true
        )) {
            return;
        }

        int seconds =
                plugin.getConfig().getInt(
                        "combat.tag-seconds",
                        30
                );

        if (seconds <= 0) {
            return;
        }

        long expires =
                System.currentTimeMillis()
                        + (seconds * 1000L);

        combatTags.put(
                player.getUniqueId(),
                expires
        );
    }

    // =========================================================
    // COMBAT CHECK
    // =========================================================

    /**
     * Comprueba si el jugador está actualmente
     * en combate.
     */
    public boolean isInCombat(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        Long expires =
                combatTags.get(uuid);

        if (expires == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expires) {

            combatTags.remove(uuid);

            return false;
        }

        return true;
    }

    // =========================================================
    // REMAINING TIME
    // =========================================================

    /**
     * Devuelve los segundos restantes de Combat Tag.
     */
    public long getRemainingSeconds(Player player) {

        if (player == null) {
            return 0;
        }

        UUID uuid =
                player.getUniqueId();

        Long expires =
                combatTags.get(uuid);

        if (expires == null) {
            return 0;
        }

        long remaining =
                expires - System.currentTimeMillis();

        if (remaining <= 0) {

            combatTags.remove(uuid);

            return 0;
        }

        return Math.max(
                0,
                (remaining + 999) / 1000
        );
    }

    // =========================================================
    // REMOVE TAG
    // =========================================================

    /**
     * Saca al jugador de combate.
     */
    public void removeTag(Player player) {

        if (player == null) {
            return;
        }

        combatTags.remove(
                player.getUniqueId()
        );
    }

    // =========================================================
    // FORCE TAG
    // =========================================================

    /**
     * Comprueba si el jugador está en combate
     * y, si lo está, extiende el tag.
     */
    public void refreshTag(Player player) {

        if (player == null) {
            return;
        }

        if (isInCombat(player)) {
            tag(player);
        }
    }

    // =========================================================
    // COMMAND BLOCK
    // =========================================================

    /**
     * Comprueba si un comando está bloqueado
     * durante combate.
     */
    public boolean isCommandBlocked(String command) {

        if (command == null) {
            return false;
        }

        if (!plugin.getConfig().getBoolean(
                "combat.block-commands",
                true
        )) {
            return false;
        }

        String cmd =
                command.toLowerCase()
                        .replace("/", "")
                        .trim();

        return cmd.equals("spawn")
                || cmd.equals("home")
                || cmd.equals("f home")
                || cmd.equals("fhome")
                || cmd.equals("tpa")
                || cmd.equals("tpaccept")
                || cmd.equals("back")
                || cmd.equals("warp")
                || cmd.equals("rtp")
                || cmd.equals("hub")
                || cmd.equals("server")
                || cmd.equals("tp")
                || cmd.equals("teleport");
    }

    // =========================================================
    // COMBAT LOGOUT
    // =========================================================

    /**
     * Maneja la desconexión de un jugador
     * durante Combat Tag.
     *
     * Si punish-logout está activo, el jugador
     * recibe una muerte real para que el
     * PlayerDeathEvent se encargue de:
     *
     * - DTR
     * - Deathban
     * - estadísticas
     * - mensajes
     * - etc.
     */
    public void handleQuit(Player player) {

        if (player == null) {
            return;
        }

        boolean inCombat =
                isInCombat(player);

        if (!inCombat) {
            return;
        }

        boolean punish =
                plugin.getConfig().getBoolean(
                        "combat.punish-logout",
                        true
                );

        removeTag(player);

        if (!punish) {
            return;
        }

        plugin.getLogger().info(
                "[CombatTag] "
                        + player.getName()
                        + " se desconectó durante combate."
        );

        /*
         * Provocamos una muerte real.
         *
         * Esto dispara PlayerDeathEvent,
         * donde HCFListener aplica:
         *
         * - pérdida de DTR
         * - deathban
         * - estadísticas
         * - etc.
         */
        if (!player.isDead()) {

            try {

                player.setHealth(0.0);

            } catch (IllegalArgumentException ignored) {

                plugin.getLogger().warning(
                        "No se pudo matar a "
                                + player.getName()
                                + " al desconectarse en combate."
                );
            }
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    /**
     * Limpia Combat Tags expirados.
     */
    public void cleanup() {

        long now =
                System.currentTimeMillis();

        combatTags.entrySet().removeIf(
                entry ->
                        entry.getValue() <= now
        );
    }

    // =========================================================
    // CLEANUP TASK
    // =========================================================

    /**
     * Inicia la limpieza automática.
     */
    public void startCleanupTask() {

        plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        this::cleanup,
                        20L,
                        20L
                );
    }

    // =========================================================
    // COMBAT COUNT
    // =========================================================

    /**
     * Devuelve la cantidad de jugadores
     * actualmente en Combat Tag.
     */
    public int getCombatCount() {

        cleanup();

        return combatTags.size();
    }

    // =========================================================
    // CLEAR ALL
    // =========================================================

    /**
     * Elimina todos los Combat Tags.
     */
    public void clearAll() {
        combatTags.clear();
    }
}