package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class KothManager {

    private final HCFCore plugin;

    private Location location;

    private boolean active;

    private Player capturingPlayer;
    private long captureStart;

    private BossBar bossBar;
    private BukkitTask task;

    public KothManager(HCFCore plugin) {
        this.plugin = plugin;
        loadLocation();
    }

    /**
     * Inicia el KOTH.
     */
    public void startKoth() {

        if (active) {
            return;
        }

        loadLocation();

        if (location == null) {

            plugin.getLogger().warning(
                    "No se puede iniciar KOTH: no hay ubicación configurada."
            );

            return;
        }

        active = true;
        capturingPlayer = null;
        captureStart = 0L;

        createBossBar();

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                0L,
                20L
        );

        Bukkit.broadcastMessage(
                ChatColor.GOLD
                        + "🏰 KOTH "
                        + ChatColor.YELLOW
                        + "ha comenzado!"
        );

        plugin.getLogger().info(
                "KOTH iniciado."
        );
    }

    /**
     * Detiene el KOTH.
     */
    public void stopKoth() {

        active = false;

        capturingPlayer = null;
        captureStart = 0L;

        if (task != null) {
            task.cancel();
            task = null;
        }

        removeBossBar();

        plugin.getLogger().info(
                "KOTH detenido."
        );
    }

    /**
     * Tick principal del KOTH.
     */
    private void tick() {

        if (!active || location == null) {
            return;
        }

        if (location.getWorld() == null) {
            stopKoth();
            return;
        }

        List<Player> players =
                getPlayersInside();

        /*
         * Nadie dentro.
         */
        if (players.isEmpty()) {

            capturingPlayer = null;
            captureStart = 0L;

            if (bossBar != null) {
                bossBar.setProgress(0.0);
            }

            updateBossBar(
                    "🏰 KOTH | Esperando jugador..."
            );

            return;
        }

        /*
         * Más de un jugador:
         * KOTH contestado.
         */
        if (players.size() > 1) {

            capturingPlayer = null;
            captureStart = 0L;

            if (bossBar != null) {
                bossBar.setProgress(0.0);
            }

            String names = getPlayerNames(players);

            updateBossBar(
                    "🏰 KOTH | ⚔ CONTESTADO | " + names
            );

            return;
        }

        /*
         * Solo un jugador dentro.
         */
        Player player = players.get(0);

        if (capturingPlayer == null
                || !capturingPlayer.getUniqueId()
                .equals(player.getUniqueId())) {

            capturingPlayer = player;

            captureStart =
                    System.currentTimeMillis();
        }

        /*
         * Si el jugador murió/desconectó.
         */
        if (!player.isOnline()
                || player.isDead()) {

            capturingPlayer = null;
            captureStart = 0L;

            if (bossBar != null) {
                bossBar.setProgress(0.0);
            }

            return;
        }

        int captureSeconds =
                plugin.getConfig().getInt(
                        "koth.capture-seconds",
                        120
                );

        if (captureSeconds <= 0) {
            captureSeconds = 120;
        }

        long elapsed =
                System.currentTimeMillis()
                        - captureStart;

        long required =
                captureSeconds * 1000L;

        double progress =
                elapsed
                        / (double) required;

        progress =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                progress
                        )
                );

        long remaining =
                Math.max(
                        0L,
                        (required - elapsed + 999)
                                / 1000
                );

        updateBossBar(
                "🏰 KOTH | "
                        + player.getName()
                        + " | "
                        + remaining
                        + "s"
        );

        if (bossBar != null) {
            bossBar.setProgress(progress);
        }

        if (elapsed >= required) {
            completeKoth(player);
        }
    }

    /**
     * Obtiene todos los jugadores dentro del radio.
     */
    private List<Player> getPlayersInside() {

        List<Player> players =
                new ArrayList<>();

        if (location == null
                || location.getWorld() == null) {

            return players;
        }

        double radius =
                plugin.getConfig().getDouble(
                        "koth.radius",
                        10.0
                );

        if (radius <= 0) {
            radius = 10.0;
        }

        double radiusSquared =
                radius * radius;

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            if (!player.isOnline()
                    || player.isDead()) {
                continue;
            }

            if (!player.getWorld().equals(
                    location.getWorld()
            )) {
                continue;
            }

            if (player.getLocation()
                    .distanceSquared(location)
                    <= radiusSquared) {

                players.add(player);
            }
        }

        return players;
    }

    /**
     * Compatibilidad con el código anterior.
     */
    private Player findCapturingPlayer() {

        List<Player> players =
                getPlayersInside();

        if (players.size() != 1) {
            return null;
        }

        return players.get(0);
    }

    /**