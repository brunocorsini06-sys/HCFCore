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

        plugin.getLogger().info("KOTH iniciado.");
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

        plugin.getLogger().info("KOTH detenido.");
    }

    /**
     * Tick principal.
     */
    private void tick() {

        if (!active || location == null) {
            return;
        }

        if (location.getWorld() == null) {
            stopKoth();
            return;
        }

        List<Player> players = getPlayersInside();

        // Nadie dentro.
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

        // Más de un jugador = contestado.
        if (players.size() > 1) {

            capturingPlayer = null;
            captureStart = 0L;

            if (bossBar != null) {
                bossBar.setProgress(0.0);
            }

            updateBossBar(
                    "🏰 KOTH | ⚔ CONTESTADO | "
                            + getPlayerNames(players)
            );

            return;
        }

        Player player = players.get(0);

        if (capturingPlayer == null
                || !capturingPlayer.getUniqueId()
                .equals(player.getUniqueId())) {

            capturingPlayer = player;
            captureStart = System.currentTimeMillis();
        }

        if (!player.isOnline() || player.isDead()) {

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
                elapsed / (double) required;

        progress = Math.max(
                0.0,
                Math.min(1.0, progress)
        );

        long remaining =
                Math.max(
                        0L,
                        (required - elapsed + 999L)
                                / 1000L
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
     * Obtiene los jugadores dentro del radio.
     */
    private List<Player> getPlayersInside() {

        List<Player> players = new ArrayList<>();

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
     * Compatibilidad con código anterior.
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
     * Obtiene los nombres de los jugadores
     * que están disputando el KOTH.
     */
    private String getPlayerNames(
            List<Player> players
    ) {

        List<String> names =
                new ArrayList<>();

        for (Player player : players) {
            names.add(player.getName());
        }

        return String.join(
                ChatColor.GRAY + ", ",
                names
        );
    }

    /**
     * Completa el KOTH.
     */
    private void completeKoth(Player player) {

        if (player == null) {
            return;
        }

        double money =
                plugin.getConfig().getDouble(
                        "koth.reward-money",
                        500.0
                );

        if (money > 0) {

            plugin.getEconomyManager()
                    .deposit(player, money);
        }

        String materialName =
                plugin.getConfig().getString(
                        "koth.reward-item",
                        "DIAMOND"
                );

        int amount =
                plugin.getConfig().getInt(
                        "koth.reward-item-amount",
                        4
                );

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material != null && amount > 0) {

            player.getInventory().addItem(
                    new ItemStack(
                            material,
                            amount
                    )
            );
        }

        Bukkit.broadcastMessage(
                ChatColor.GOLD
                        + "🏆 KOTH capturado por "
                        + ChatColor.YELLOW
                        + player.getName()
                        + ChatColor.GOLD
                        + "!"
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "Has recibido "
                        + ChatColor.WHITE
                        + "$"
                        + String.format(
                                java.util.Locale.US,
                                "%.2f",
                                money
                        )
                        + ChatColor.GREEN
                        + " y "
                        + amount
                        + "x "
                        + materialName
                        + "."
        );

        stopKoth();
    }

    /**
     * Crea la BossBar.
     */
    private void createBossBar() {

        removeBossBar();

        bossBar = Bukkit.createBossBar(
                ChatColor.GOLD + "🏰 KOTH",
                BarColor.PURPLE,
                BarStyle.SOLID
        );

        bossBar.setProgress(0.0);

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            bossBar.addPlayer(player);
        }
    }

    /**
     * Actualiza el texto de la BossBar.
     */
    private void updateBossBar(String title) {

        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(
                ChatColor.GOLD + title
        );
    }

    /**
     * Añade un jugador a la BossBar.
     */
    public void addPlayerToBossBar(
            Player player
    ) {

        if (bossBar == null || player == null) {
            return;
        }

        bossBar.addPlayer(player);
    }

    /**
     * Elimina la BossBar.
     */
    private void removeBossBar() {

        if (bossBar == null) {
            return;
        }

        bossBar.removeAll();
        bossBar = null;
    }

    /**
     * Establece la ubicación del KOTH.
     */
    public void setLocation(Location location) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }

        this.location =
                location.clone();

        saveLocation();
    }

    /**
     * Devuelve la ubicación del KOTH.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Comprueba si está activo.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Devuelve el jugador capturando.
     */
    public Player getCapturingPlayer() {
        return capturingPlayer;
    }

    /**
     * Devuelve la BossBar.
     */
    public BossBar getBossBar() {
        return bossBar;
    }

    /**
     * Carga la ubicación desde config.yml.
     */
    private void loadLocation() {

        String worldName =
                plugin.getConfig().getString(
                        "koth.world"
                );

        if (worldName == null
                || worldName.trim().isEmpty()) {

            location = null;
            return;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {

            location = null;

            plugin.getLogger().warning(
                    "El mundo del KOTH no existe: "
                            + worldName
            );

            return;
        }

        double x =
                plugin.getConfig().getDouble(
                        "koth.x",
                        0.0
                );

        double y =
                plugin.getConfig().getDouble(
                        "koth.y",
                        0.0
                );

        double z =
                plugin.getConfig().getDouble(
                        "koth.z",
                        0.0
                );

        float yaw =
                (float) plugin.getConfig()
                        .getDouble(
                                "koth.yaw",
                                0.0
                        );

        float pitch =
                (float) plugin.getConfig()
                        .getDouble(
                                "koth.pitch",
                                0.0
                        );

        location =
                new Location(
                        world,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                );
    }

    /**
     * Guarda la ubicación en config.yml.
     */
    private void saveLocation() {

        if (location == null
                || location.getWorld() == null) {

            return;
        }

        plugin.getConfig().set(
                "koth.world",
                location.getWorld().getName()
        );

        plugin.getConfig().set(
                "koth.x",
                location.getX()
        );

        plugin.getConfig().set(
                "koth.y",
                location.getY()
        );

        plugin.getConfig().set(
                "koth.z",
                location.getZ()
        );

        plugin.getConfig().set(
                "koth.yaw",
                location.getYaw()
        );

        plugin.getConfig().set(
                "koth.pitch",
                location.getPitch()
        );

        plugin.saveConfig();
    }
}