package com.hcfcore;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class CombatManager {
    private final HCFCore plugin;
    /**
     * UUID -> tiempo de expiración en milisegundos.
     */
    private final Map<UUID, Long> combatTags =
            new HashMap<>();
    public CombatManager(HCFCore plugin) {
        this.plugin = plugin;
    }
    // =========================================================
    // TAG
    // =========================================================
    /**
     * Pone al jugador en Combat Tag.
     *
     * Si ya estaba en combate,
     * el contador se reinicia.
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
     * en Combat Tag.
     */
    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        Long expires =
                combatTags.get(
                        player.getUniqueId()
                );
        if (expires == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expires) {
            combatTags.remove(
                    player.getUniqueId()
            );
            return false;
        }
        return true;
    }
    // =========================================================
    // REMAINING TIME
    // =========================================================
    /**
     * Devuelve los segundos restantes.
     */
    public long getRemainingSeconds(Player player) {
        if (player == null) {
            return 0;
        }
        Long expires =
                combatTags.get(
                        player.getUniqueId()
                );
        if (expires == null) {
            return 0;
        }
        long remaining =
                expires - System.currentTimeMillis();
        if (remaining <= 0) {
            combatTags.remove(
                    player.getUniqueId()
            );
            return 0;
        }
        return (remaining + 999L) / 1000L;
    }
    // =========================================================
    // GET EXPIRATION
    // =========================================================
    /**
     * Devuelve el timestamp de expiración.
     *
     * Si no está en combate devuelve 0.
     */
    public long getExpiration(Player player) {
        if (player == null) {
            return 0;
        }
        Long expires =
                combatTags.get(
                        player.getUniqueId()
                );
        if (expires == null) {
            return 0;
        }
        if (System.currentTimeMillis() >= expires) {
            combatTags.remove(
                    player.getUniqueId()
            );
            return 0;
        }
        return expires;
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
    // REFRESH
    // =========================================================
    /**
     * Reinicia el Combat Tag únicamente
     * si el jugador ya estaba en combate.
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
     * durante Combat Tag.
     *
     * Acepta:
     *
     * /spawn
     * /home
     * /f home
     * /fhome
     * /tpa
     * etc.
     */
    public boolean isCommandBlocked(String command) {
        if (command == null
                || command.isBlank()) {
            return false;
        }
        if (!plugin.getConfig().getBoolean(
                "combat.block-commands",
                true
        )) {
            return false;
        }
        String cmd =
                command
                        .toLowerCase()
                        .replace("/", "")
                        .trim();
        /*
         * Eliminamos argumentos para comprobar
         * únicamente el comando principal.
         */
        String baseCommand = cmd;
        int space =
                baseCommand.indexOf(' ');
        if (space >= 0) {
            baseCommand =
                    baseCommand.substring(
                            0,
                            space
                    );
        }
        // -----------------------------------------------------
        // COMANDOS SIMPLES
        // -----------------------------------------------------
        switch (baseCommand) {
            case "spawn":
            case "home":
            case "fhome":
            case "tpa":
            case "tpaccept":
            case "back":
            case "warp":
            case "rtp":
            case "hub":
            case "server":
            case "tp":
            case "teleport":
            case "vanish":
                return true;
            default:
                break;
        }
        /*
         * /f home
         * /fhome
         */
        if (cmd.equals("f home")
                || cmd.startsWith("f home ")) {
            return true;
        }
        /*
         * /f teleport
         * /f tp
         */
        if (cmd.equals("f tp")
                || cmd.startsWith("f tp ")
                || cmd.equals("f teleport")
                || cmd.startsWith("f teleport ")) {
            return true;
        }
        return false;
    }
    // =========================================================
    // COMBAT LOGOUT
    // =========================================================
    /**
     * Maneja un logout durante Combat Tag.
     */
    public void handleQuit(Player player) {
        if (player == null) {
            return;
        }
        if (!isInCombat(player)) {
            return;
        }
        boolean punish =
                plugin.getConfig().getBoolean(
                        "combat.punish-logout",
                        true
                );
        /*
         * Quitamos el tag inmediatamente para
         * evitar doble procesamiento.
         */
        removeTag(player);
        if (!punish) {
            return;
        }
        plugin.getLogger().warning(
                "[CombatTag] "
                        + player.getName()
                        + " se desconectó durante combate."
        );
        /*
         * Intentamos producir una muerte real.
         *
         * Si Bukkit todavía mantiene al jugador
         * activo durante PlayerQuitEvent,
         * PlayerDeathEvent será ejecutado.
         */
        if (!player.isDead()) {
            try {
                player.setHealth(0.0);
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "[CombatTag] No se pudo matar a "
                                + player.getName()
                                + " durante logout."
                );
            }
        }
    }
    // =========================================================
    // CLEANUP
    // =========================================================
    /**
     * Elimina Combat Tags expirados.
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
     * Inicia la tarea de limpieza.
     *
     * Se ejecuta cada segundo.
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
     * Cantidad de jugadores actualmente
     * en Combat Tag.
     */
    public int getCombatCount() {
        cleanup();
        return combatTags.size();
    }
    // =========================================================
    // IS ANYONE IN COMBAT
    // =========================================================
    /**
     * Comprueba si existe al menos
     * un jugador en combate.
     */
    public boolean hasPlayersInCombat() {
        cleanup();
        return !combatTags.isEmpty();
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