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

    public void startKoth() {

        if (active) {
            return;
        }

        loadLocation();

        if (location == null) {
            plugin.getLogger().warning(
                    "No se puede iniciar KOTH: "
                            + "no hay ubicación configurada."
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

        plugin.getLogger().info(
                "KOTH iniciado."
        );
    }

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

    private void tick() {

        if (!active || location == null) {
            return;
        }

        Player player =
                findCapturingPlayer();

        if (player == null) {

            capturingPlayer = null;
            captureStart = 0L;

            updateBossBar(
                    "🏰 KOTH | Esperando jugador..."
            );

            return;
        }

        if (capturingPlayer == null
                || !capturingPlayer
                .getUniqueId()
                .equals(
                        player.getUniqueId()
                )) {

            capturingPlayer = player;
            captureStart =
                    System.currentTimeMillis();
        }

        int captureSeconds =
                plugin.getConfig().getInt(
                        "koth.capture-seconds",
                        120
                );

        long elapsed =
                System.currentTimeMillis()
                        - captureStart;

        long required =
                captureSeconds * 1000L;

        double progress =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                elapsed
                                        / (double) required
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

    private Player findCapturingPlayer() {

        if (location.getWorld() == null) {
            return null;
        }

        double radius =
                plugin.getConfig().getDouble(
                        "koth.radius",
                        10.0
                );

        double radiusSquared =
                radius * radius;

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(
                    location.getWorld()
            )) {
                continue;
            }

            if (player.getLocation()
                    .distanceSquared(location)
                    <= radiusSquared) {

                return player;
            }
        }

        return null;
    }

    private void completeKoth(
            Player player
    ) {

        double money =
                plugin.getConfig().getDouble(
                        "koth.reward-money",
                        500
                );

        if (money > 0) {
            plugin.getEconomyManager()
                    .deposit(
                            player,
                            money
                    );
        }

        String materialName =
                plugin.getConfig().getString(
                        "koth.reward-item",
                        "DIAMOND"
                );

        Material material =
                Material.matchMaterial(
                        materialName
                );

        int amount =
                plugin.getConfig().getInt(
                        "koth.reward-item-amount",
                        4
                );

        if (material != null
                && amount > 0) {

            player.getInventory().addItem(
                    new ItemStack(
                            material,
                            amount
                    )
            );
        }

        player.sendMessage(
                ChatColor.GOLD
                        + "🏆 ¡Has capturado el KOTH!"
        );

        Bukkit.broadcastMessage(
                ChatColor.GOLD
                        + "🏰 KOTH capturado por "
                        + ChatColor.WHITE
                        + player.getName()
                        + ChatColor.GOLD
                        + "!"
        );

        stopKoth();
    }

    private void createBossBar() {

        removeBossBar();

        bossBar =
                Bukkit.createBossBar(
                        ChatColor.GOLD
                                + "🏰 KOTH",
                        BarColor.RED,
                        BarStyle.SOLID
                );

        bossBar.setProgress(0.0);

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            bossBar.addPlayer(player);
        }
    }

    private void updateBossBar(
            String text
    ) {

        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(
                ChatColor.GOLD + text
        );
    }

    private void removeBossBar() {

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void setLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {
            return;
        }

        this.location =
                location.clone();

        saveLocation();
    }

    public Location getLocation() {

        if (location == null) {
            return null;
        }

        return location.clone();
    }

    private void loadLocation() {

        String worldName =
                plugin.getConfig()
                        .getString(
                                "koth.world"
                        );

        if (worldName == null) {
            return;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {
            return;
        }

        double x =
                plugin.getConfig()
                        .getDouble(
                                "koth.x"
                        );

        double y =
                plugin.getConfig()
                        .getDouble(
                                "koth.y"
                        );

        double z =
                plugin.getConfig()
                        .getDouble(
                                "koth.z"
                        );

        float yaw =
                (float) plugin.getConfig()
                        .getDouble(
                                "koth.yaw"
                        );

        float pitch =
                (float) plugin.getConfig()
                        .getDouble(
                                "koth.pitch"
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

    private void saveLocation() {

        if (location == null
                || location.getWorld() == null) {
            return;
        }

        plugin.getConfig().set(
                "koth.world",
                location.getWorld()
                        .getName()
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

    public boolean isActive() {
        return active;
    }

    public Player getCapturingPlayer() {
        return capturingPlayer;
    }

    public BossBar getBossBar() {
        return bossBar;
    }
}