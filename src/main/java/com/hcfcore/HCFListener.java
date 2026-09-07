package com.hcfcore;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;

public class HCFListener implements Listener {

    private final HCFCore plugin;

    public HCFListener(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // PLAYER JOIN
    // =========================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        plugin.getDeathbanManager().setupPlayer(player);

        plugin.getScoreboardManager().update(player);

        if (plugin.getKothManager().isActive()) {

            if (plugin.getKothManager().getBossBar() != null) {

                plugin.getKothManager()
                        .getBossBar()
                        .addPlayer(player);
            }
        }

        long deathban =
                plugin.getDeathbanManager()
                        .getRemainingSeconds(player);

        if (deathban == -1) {

            player.sendMessage(
                    ChatColor.RED +
                            "☠ Estás permanentemente deathbaneado."
            );

        } else if (deathban > 0) {

            player.sendMessage(
                    ChatColor.RED +
                            "☠ Estás deathbaneado por "
                            + formatTime(deathban)
            );
        }

        player.sendMessage(
                ChatColor.GOLD +
                        "━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "      Bienvenido a HCF"
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Usa /f para gestionar tu faction."
        );

        player.sendMessage(
                ChatColor.GOLD +
                        "━━━━━━━━━━━━━━━━━━━━"
        );
    }

    // =========================================================
    // PLAYER QUIT
    // =========================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (plugin.getCombatManager()
                .isInCombat(player)) {

            plugin.getCombatManager()
                    .handleQuit(player);
        }

        if (plugin.getKothManager()
                .getBossBar() != null) {

            plugin.getKothManager()
                    .getBossBar()
                    .removePlayer(player);
        }
    }

    // =========================================================
    // BLOCK BREAK
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (!canBuild(
                player,
                event.getBlock().getLocation()
        )) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // BLOCK PLACE
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (!canBuild(
                player,
                event.getBlock().getLocation()
        )) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // INVENTORY OPEN
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onInventoryOpen(InventoryOpenEvent event) {

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getPlayer();

        Location location =
                event.getInventory().getLocation();

        if (location == null) {
            return;
        }

        if (!canBuild(player, location)) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // INTERACT
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onInteract(PlayerInteractEvent event) {

        Player player =
                event.getPlayer();

        if (event.getClickedBlock() == null) {
            return;
        }

        Location location =
                event.getClickedBlock().getLocation();

        if (canBuild(player, location)) {
            return;
        }

        switch (event.getClickedBlock().getType()) {

            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case HOPPER:
            case DROPPER:
            case DISPENSER:
            case BREWING_STAND:
            case ENCHANTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case CRAFTING_TABLE:
            case STONECUTTER:
            case LOOM:
            case CARTOGRAPHY_TABLE:
            case FLETCHING_TABLE:
            case GRINDSTONE:
            case SMITHING_TABLE:
            case BEACON:
            case JUKEBOX:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case OAK_DOOR:
            case SPRUCE_DOOR:
            case BIRCH_DOOR:
            case JUNGLE_DOOR:
            case ACACIA_DOOR:
            case DARK_OAK_DOOR:
            case MANGROVE_DOOR:
            case CHERRY_DOOR:
            case BAMBOO_DOOR:
            case CRIMSON_DOOR:
            case WARPED_DOOR:
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case ACACIA_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case MANGROVE_TRAPDOOR:
            case CHERRY_TRAPDOOR:
            case BAMBOO_TRAPDOOR:
            case CRIMSON_TRAPDOOR:
            case WARPED_TRAPDOOR:
            case OAK_FENCE_GATE:
            case SPRUCE_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case ACACIA_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case MANGROVE_FENCE_GATE:
            case CHERRY_FENCE_GATE:
            case BAMBOO_FENCE_GATE:
            case CRIMSON_FENCE_GATE:
            case WARPED_FENCE_GATE:
            case LEVER:
            case STONE_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case MANGROVE_BUTTON:
            case CHERRY_BUTTON:
            case BAMBOO_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:

                event.setCancelled(true);
                sendClaimDenied(player);
                break;

            default:
                break;
        }
    }

    // =========================================================
    // EXPLOSIONES
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onExplosion(EntityExplodeEvent event) {

        Iterator<org.bukkit.block.Block> iterator =
                event.blockList().iterator();

        while (iterator.hasNext()) {

            org.bukkit.block.Block block =
                    iterator.next();

            if (plugin.getClaimManager()
                    .isClaimed(block.getLocation())) {

                iterator.remove();
            }
        }
    }

    // =========================================================
    // PISTONES - EXTENDER
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonExtend(
            BlockPistonExtendEvent event
    ) {

        for (org.bukkit.block.Block block :
                event.getBlocks()) {

            Location destination =
                    block.getLocation().add(
                            event.getDirection().getModX(),
                            event.getDirection().getModY(),
                            event.getDirection().getModZ()
                    );

            if (plugin.getClaimManager()
                    .isClaimed(destination)) {

                event.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================
    // PISTONES - RETRAER
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonRetract(
            BlockPistonRetractEvent event
    ) {

        for (org.bukkit.block.Block block :
                event.getBlocks()) {

            if (plugin.getClaimManager()
                    .isClaimed(block.getLocation())) {

                event.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================
    // LÍQUIDOS
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onLiquidFlow(
            BlockFromToEvent event
    ) {

        if (plugin.getClaimManager()
                .isClaimed(
                        event.getToBlock().getLocation()
                )) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // FUEGO
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockBurn(
            BlockBurnEvent event
    ) {

        if (plugin.getClaimManager()
                .isClaimed(
                        event.getBlock().getLocation()
                )) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // PVP / COMBAT TAG
    // =========================================================

    @EventHandler
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim =
                (Player) event.getEntity();

        Player attacker = null;

        Entity damager =
                event.getDamager();

        // -----------------------------------------------------
        // PVP CUERPO A CUERPO
        // -----------------------------------------------------

        if (damager instanceof Player) {

            attacker =
                    (Player) damager;
        }

        // -----------------------------------------------------
        // PVP CON PROYECTILES
        // -----------------------------------------------------

        else if (damager instanceof Projectile) {

            Projectile projectile =
                    (Projectile) damager;

            if (projectile.getShooter()
                    instanceof Player) {

                attacker =
                        (Player) projectile.getShooter();
            }
        }

        // No fue un ataque de jugador.
        if (attacker == null) {
            return;
        }

        // -----------------------------------------------------
        // FRIENDLY FIRE
        // -----------------------------------------------------

        Faction attackerFaction =
                plugin.getFactionManager()
                        .getFaction(attacker);

        Faction victimFaction =
                plugin.getFactionManager()
                        .getFaction(victim);

        if (attackerFaction != null
                && victimFaction != null
                && attackerFaction
                .getName()
                .equalsIgnoreCase(
                        victimFaction.getName()
                )) {

            boolean friendlyFire =
                    plugin.getConfig()
                            .getBoolean(
                                    "factions.friendly-fire",
                                    false
                            );

            if (!friendlyFire) {

                event.setCancelled(true);

                attacker.sendMessage(
                        ChatColor.RED +
                                "No puedes atacar a un miembro de tu faction."
                );

                return;
            }
        }

        // -----------------------------------------------------
        // SI EL EVENTO FUE CANCELADO
        // -----------------------------------------------------

        if (event.isCancelled()) {
            return;
        }

        // -----------------------------------------------------
        // COMBAT TAG
        // -----------------------------------------------------

        plugin.getCombatManager()
                .tag(attacker);

        plugin.getCombatManager()
                .tag(victim);
    }

    // =========================================================
    // PLAYER DEATH
    // =========================================================

    @EventHandler
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getEntity();

        // -----------------------------------------------------
        // ESTADÍSTICA DE MUERTES
        // -----------------------------------------------------

        plugin.getFactionManager()
                .addDeath(player);

        // -----------------------------------------------------
        // DTR
        // -----------------------------------------------------

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        boolean dtrLost = false;

        if (faction != null) {

            double oldDtr =
                    faction.getDtr();

            dtrLost =
                    plugin.getFactionManager()
                            .handleDeath(player);

            double newDtr =
                    faction.getDtr();

            if (dtrLost) {

                player.sendMessage(
                        ChatColor.RED +
                                "☠ Tu faction perdió "
                                + formatDtr(oldDtr - newDtr)
                                + " DTR."
                );

                player.sendMessage(
                        ChatColor.RED +
                                "DTR actual: "
                                + formatDtr(newDtr)
                );

                // -------------------------------------------------
                // RAIDABLE
                // -------------------------------------------------

                if (newDtr <= 0.0
                        && oldDtr > 0.0) {

                    broadcastRaidable(faction);
                }
            }
        }

        // -----------------------------------------------------
        // DEATHBAN
        // -----------------------------------------------------

        if (plugin.getConfig()
                .getBoolean(
                        "deathban.enabled",
                        true
                )) {

            plugin.getDeathbanManager()
                    .deathban(player);
        }

        // -----------------------------------------------------
        // KILL
        // -----------------------------------------------------

        Player killer =
                player.getKiller();

        if (killer != null
                && !killer.equals(player)) {

            plugin.getFactionManager()
                    .addKill(killer);

            killer.sendMessage(
                    ChatColor.GREEN +
                            "⚔ ¡Has matado a "
                            + player.getName()
                            + "!"
            );

            plugin.getScoreboardManager()
                    .update(killer);
        }

        // -----------------------------------------------------
        // COMBAT TAG
        // -----------------------------------------------------

        plugin.getCombatManager()
                .removeTag(player);

        if (killer != null) {

            plugin.getCombatManager()
                    .removeTag(killer);
        }

        // -----------------------------------------------------
        // SCOREBOARD
        // -----------------------------------------------------

        plugin.getScoreboardManager()
                .update(player);
    }

    // =========================================================
    // RAIDABLE ANNOUNCEMENT
    // =========================================================

    private void broadcastRaidable(
            Faction faction
    ) {

        if (faction == null) {
            return;
        }

        String message =
                ChatColor.DARK_RED
                        + "☠ "
                        + ChatColor.RED
                        + "¡La faction "
                        + ChatColor.YELLOW
                        + faction.getName()
                        + ChatColor.RED
                        + " está ahora "
                        + ChatColor.DARK_RED
                        + "RAIDABLE"
                        + ChatColor.RED
                        + "!";

        plugin.getServer()
                .broadcastMessage(message);
    }

    // =========================================================
    // CLAIM CHECK
    // =========================================================

    private boolean canBuild(
            Player player,
            Location location
    ) {

        if (player == null || location == null) {
            return false;
        }

        if (player.hasPermission("hcf.bypass")) {
            return true;
        }

        return plugin.getClaimManager()
                .canBuild(
                        player,
                        location
                );
    }

    // =========================================================
    // CLAIM DENIED
    // =========================================================

    private void sendClaimDenied(
            Player player
    ) {

        player.sendMessage(
                ChatColor.RED +
                        "No puedes hacer eso dentro del claim de otra faction."
        );
    }

    // =========================================================
    // DTR FORMAT
    // =========================================================

    private String formatDtr(
            double value
    ) {

        return String.format(
                java.util.Locale.US,
                "%.1f",
                Math.max(0.0, value)
        );
    }

    // =========================================================
    // TIME FORMAT
    // =========================================================

    private String formatTime(
            long seconds
    ) {

        if (seconds <= 0) {
            return "0s";
        }

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        seconds %= 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }
}