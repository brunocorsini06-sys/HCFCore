package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final HCFCore plugin;

    private final Map<UUID, Long> combatTags = new HashMap<>();

    public CombatManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Pone al jugador en combate.
     */
    public void tag(Player player) {

        if (player == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "combat.enabled", true)) {
            return;
        }

        int seconds = plugin.getConfig().getInt(
                "combat.tag-seconds", 30);

        combatTags.put(
                player.getUniqueId(),
                System.currentTimeMillis()
                        + (seconds * 1000L)
        );
    }

    /**
     * Comprueba si el jugador está en combate.
     */
    public boolean isInCombat(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        Long expires = combatTags.get(uuid);

        if (expires == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expires) {
            combatTags.remove(uuid);
            return false;
        }

        return true;
    }

    /**
     * Devuelve los segundos restantes.
     */
    public long getRemainingSeconds(Player player) {

        if (!isInCombat(player)) {
            return 0;
        }

        Long expires =
                combatTags.get(player.getUniqueId());

        long remaining =
                expires - System.currentTimeMillis();

        return Math.max(
                0,
                (remaining + 999) / 1000
        );
    }

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

    /**
     * Comprueba si un comando está bloqueado
     * mientras el jugador está en combate.
     */
    public boolean isCommandBlocked(String command) {

        String cmd =
                command.toLowerCase()
                        .replace("/", "");

        return cmd.equals("spawn")
                || cmd.equals("home")
                || cmd.equals("f home")
                || cmd.equals("tpa")
                || cmd.equals("tpaccept")
                || cmd.equals("back")
                || cmd.equals("warp")
                || cmd.equals("rtp")
                || cmd.equals("hub")
                || cmd.equals("server");
    }

    /**
     * Castigo por desconectarse durante combate.
     */
    public void handleQuit(Player player) {

        if (player == null) {
            return;
        }

        if (!isInCombat(player)) {
            removeTag(player);
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

        /*
         * El castigo real de muerte se ejecutará
         * desde HCFListener para que Bukkit procese
         * correctamente la muerte del jugador.
         */
        plugin.getLogger().info(
                player.getName()
                        + " se desconectó durante combate."
        );
    }

    /**
     * Limpia combat tags expirados.
     */
    public void cleanup() {

        long now =
                System.currentTimeMillis();

        combatTags.entrySet().removeIf(
                entry -> entry.getValue() <= now
        );
    }

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
}