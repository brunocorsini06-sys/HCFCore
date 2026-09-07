package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class AirdropManager {

    private final HCFCore plugin;
    private final Random random = new Random();

    private Location activeDrop;
    private BossBar bossBar;

    private BukkitTask schedulerTask;
    private BukkitTask activeDropTask;

    public AirdropManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia el sistema automático de Airdrops.
     */
    public void startScheduler() {

        if (schedulerTask != null) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "airdrop.enabled",
                true
        )) {
            return;
        }

        int interval =
                plugin.getConfig()
                        .getInt(
                                "airdrop.interval-minutes",
                                30
                        );

        long ticks =
                Math.max(
                        20L,
                        interval * 60L * 20L
                );

        schedulerTask =
                Bukkit.getScheduler().runTaskTimer(
                        plugin,
                        this::spawnAirdrop,
                        ticks,
                        ticks
                );
    }

    /**
     * Genera un Airdrop aleatorio.
     *
     * No manda mensaje al chat.
     * La información se muestra mediante BossBar.
     */
    public void spawnAirdrop() {

        if (!plugin.getConfig().getBoolean(
                "airdrop.enabled",
                true
        )) {
            return;
        }

        World world = getWorld();

        if (world == null) {
            return;
        }

        removeActiveDrop();

        int radius =
                plugin.getConfig()
                        .getInt(
                                "airdrop.radius",
                                2450
                        );

        radius = Math.max(1, radius);

        int x =
                random.nextInt(
                        radius * 2 + 1
                ) - radius;

        int z =
                random.nextInt(
                        radius * 2 + 1
                ) - radius;

        int highest =
                world.getHighestBlockYAt(x, z);

        Location location =
                new Location(
                        world,
                        x + 0.5,
                        highest + 1,
                        z + 0.5
                );

        Block block =
                location.getBlock();

        block.setType(Material.CHEST);

        if (!(block.getState() instanceof Chest)) {
            return;
        }

        Chest chest =
                (Chest) block.getState();

        fillChest(chest);

        activeDrop = location;

        createBossBar();

        int lifetime =
                plugin.getConfig()
                        .getInt(
                                "airdrop.lifetime-seconds",
                                180
                        );

        lifetime = Math.max(10, lifetime);

        final long start =
                System.currentTimeMillis();

        final long duration =
                lifetime * 1000L;

        activeDropTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (activeDrop == null) {
                                        return;
                                    }

                                    long elapsed =
                                            System.currentTimeMillis()
                                                    - start;

                                    long remaining =
                                            duration - elapsed;

                                    double progress =
                                            Math.max(
                                                    0.0,
                                                    Math.min(
                                                            1.0,
                                                            remaining
                                                                    / (double)
                                                                    duration
                                                    )
                                            );

                                    if (bossBar != null) {

                                        bossBar.setProgress(
                                                progress
                                        );

                                        bossBar.setTitle(
                                                ChatColor.GOLD
                                                        + "✈ AIRDROP "
                                                        + ChatColor.WHITE
                                                        + "| "
                                                        + formatSeconds(
                                                                remaining
                                                        )
                                        );
                                    }

                                    if (remaining <= 0) {
                                        removeActiveDrop();
                                    }

                                },
                                0L,
                                20L
                        );
    }

    /**
     * Llena el cofre con loot configurable.
     */
    private void fillChest(Chest chest) {

        List<String> loot =
                plugin.getConfig()
                        .getStringList(
                                "airdrop.loot"
                        );

        for (String entry : loot) {

            String[] parts =
                    entry.split(":");

            if (parts.length < 2) {
                continue;
            }

            Material material =
                    Material.matchMaterial(
                            parts[0].trim().toUpperCase()
                    );

            if (material == null) {
                continue;
            }

            int amount;

            try {

                amount =
                        Integer.parseInt(
                                parts[1].trim()
                        );

            } catch (NumberFormatException e) {

                amount = 1;
            }

            amount =
                    Math.max(
                            1,
                            amount
                    );

            chest.getInventory()
                    .addItem(
                            new ItemStack(
                                    material,
                                    amount
                            )
                    );
        }
    }

    /**
     * Crea la BossBar.
     */
    private void createBossBar() {

        if (bossBar != null) {
            bossBar.removeAll();
        }

        bossBar =
                Bukkit.createBossBar(
                        ChatColor.GOLD
                                + "✈ AIRDROP",
                        BarColor.YELLOW,
                        BarStyle.SOLID
                );

        bossBar.setProgress(1.0);

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            bossBar.addPlayer(player);
        }
    }

    /**
     * Elimina el Airdrop activo.
     */
    public void removeActiveDrop() {

        if (activeDropTask != null) {

            activeDropTask.cancel();
            activeDropTask = null;
        }

        if (activeDrop != null) {

            Block block =
                    activeDrop.getBlock();

            if (block.getType()
                    == Material.CHEST) {

                block.setType(
                        Material.AIR
                );
            }
        }

        activeDrop = null;

        if (bossBar != null) {

            bossBar.removeAll();
            bossBar = null;
        }
    }

    /**
     * Detiene completamente el sistema de Airdrops.
     */
    public void shutdown() {

        if (schedulerTask != null) {

            schedulerTask.cancel();
            schedulerTask = null;
        }

        removeActiveDrop();
    }

    /**
     * Devuelve el mundo configurado.
     */
    private World getWorld() {

        String worldName =
                plugin.getConfig()
                        .getString(
                                "world.name",
                                "world"
                        );

        return Bukkit.getWorld(
                worldName
        );
    }

    private String formatSeconds(
            long milliseconds
    ) {

        long seconds =
                Math.max(
                        0,
                        (milliseconds + 999)
                                / 1000
                );

        long minutes =
                seconds / 60;

        long remainingSeconds =
                seconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                remainingSeconds
        );
    }

    public Location getActiveDrop() {
        return activeDrop;
    }

    public BossBar getBossBar() {
        return bossBar;
    }
}