package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final HCFCore plugin;

    private final Set<UUID> vanishedPlayers =
            new HashSet<>();

    public VanishManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /*
     * =========================================================
     * ENABLE VANISH
     * =========================================================
     */

    public boolean enable(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            return false;
        }

        vanishedPlayers.add(uuid);

        /*
         * Ocultar de jugadores normales.
         */

        for (Player online :
                Bukkit.getOnlinePlayers()) {

            if (online.equals(player)) {
                continue;
            }

            if (plugin.getStaffManager()
                    .isStaff(online)) {

                continue;
            }

            online.hidePlayer(
                    plugin,
                    player
            );
        }

        player.sendMessage(
                ChatColor.GREEN
                        + "Vanish activado."
        );

        return true;
    }

    /*
     * =========================================================
     * DISABLE VANISH
     * =========================================================
     */

    public boolean disable(Player player) {

        if (player == null) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        if (!vanishedPlayers.remove(uuid)) {
            return false;
        }

        /*
         * Mostrar nuevamente al jugador.
         */

        for (Player online :
                Bukkit.getOnlinePlayers()) {

            if (online.equals(player)) {
                continue;
            }

            online.showPlayer(
                    plugin,
                    player
            );
        }

        player.sendMessage(
                ChatColor.RED
                        + "Vanish desactivado."
        );

        return true;
    }

    /*
     * =========================================================
     * TOGGLE
     * =========================================================
     */

    public boolean toggle(Player player) {

        if (isVanished(player)) {

            disable(player);
            return false;
        }

        enable(player);
        return true;
    }

    /*
     * =========================================================
     * CHECK
     * =========================================================
     */

    public boolean isVanished(Player player) {

        if (player == null) {
            return false;
        }

        return vanishedPlayers.contains(
                player.getUniqueId()
        );
    }

    /*
     * =========================================================
     * PLAYER JOIN
     * =========================================================
     */

    public void handleJoin(Player joining) {

        if (joining == null) {
            return;
        }

        /*
         * Si entra un jugador normal,
         * ocultarle todos los staff vanish.
         */

        if (!plugin.getStaffManager()
                .isStaff(joining)) {

            for (UUID uuid :
                    vanishedPlayers) {

                Player vanished =
                        Bukkit.getPlayer(uuid);

                if (vanished == null
                        || !vanished.isOnline()) {

                    continue;
                }

                joining.hidePlayer(
                        plugin,
                        vanished
                );
            }
        }
    }

    /*
     * =========================================================
     * PLAYER QUIT
     * =========================================================
     */

    public void handleQuit(Player player) {

        if (player == null) {
            return;
        }

        vanishedPlayers.remove(
                player.getUniqueId()
        );
    }

    /*
     * =========================================================
     * DISABLE ALL
     * =========================================================
     */

    public void disableAll() {

        for (UUID uuid :
                new HashSet<>(vanishedPlayers)) {

            Player player =
                    Bukkit.getPlayer(uuid);

            if (player != null) {

                disable(player);
            }
        }

        vanishedPlayers.clear();
    }

    /*
     * =========================================================
     * COUNT
     * =========================================================
     */

    public int getVanishedCount() {

        return vanishedPlayers.size();
    }
}