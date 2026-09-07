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

    /**
     * Pone al jugador en combate.
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

    /**
     * Devuelve los segundos restantes.
     */
    public long getRemainingSeconds(Player player) {

        if (player == null) {
            return 0;
        }

        if (!isInCombat(player)) {
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

        /*
         * Comandos bloqueados durante combate.
         */
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

        boolean inCombat =
                isInCombat(player);

        removeTag(player);

        if (!inCombat) {
            return;
        }

        boolean punish =
                plugin.getConfig().getBoolean(
                        "combat.punish-logout",
                        true
                );

        if (!punish) {
            return;
        }

        /*
         * El castigo de muerte se maneja
         * desde HCFListener.
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
                entry ->
                        entry.getValue() <= now
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

    /**
     * Devuelve la cantidad de jugadores
     * actualmente marcados en combate.
     */
    public int getCombatCount() {
        cleanup();
        return combatTags.size();
    }
}