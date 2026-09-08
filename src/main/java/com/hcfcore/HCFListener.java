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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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

        /*
         * VANISH
         */

        if (plugin.getVanishManager() != null) {

            plugin.getVanishManager()
                    .handleJoin(player);
        }

        plugin.getDeathbanManager()
                .setupPlayer(player);

        plugin.getScoreboardManager()
                .update(player);

        if (plugin.getKothManager()
                .isActive()) {

            if (plugin.getKothManager()
                    .getBossBar() != null) {

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

        /*
         * VANISH
         */

        if (plugin.getVanishManager() != null) {

            plugin.getVanishManager()
                    .handleQuit(player);
        }

        /*
         * STAFF MODE
         */

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            plugin.getStaffManager()
                    .handleQuit(player);
        }

        /*
         * COMBAT
         */

        if (plugin.getCombatManager()
                .isInCombat(player)) {

            plugin.getCombatManager()
                    .handleQuit(player);
        }

        /*
         * KOTH BOSSBAR
         */

        if (plugin.getKothManager()
                .getBossBar() != null) {

            plugin.getKothManager()
                    .getBossBar()
                    .removePlayer(player);
        }
    }

    // =========================================================
    // STAFF MODE - DAMAGE
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim =
                (Player) event.getEntity();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(victim)) {

            event.setCancelled(true);
            return;
        }

        Entity damager =
                event.getDamager();

        if (damager instanceof Player) {

            Player attacker =
                    (Player) damager;

            if (plugin.getStaffManager() != null
                    && plugin.getStaffManager().isStaff(attacker)) {

                event.setCancelled(true);
            }

            return;
        }

        if (damager instanceof Projectile) {

            Projectile projectile =
                    (Projectile) damager;

            if (projectile.getShooter()
                    instanceof Player) {

                Player shooter =
                        (Player) projectile.getShooter();

                if (plugin.getStaffManager() != null
                        && plugin.getStaffManager().isStaff(shooter)) {

                    event.setCancelled(true);
                }
            }
        }
    }

    // =========================================================
    // STAFF MODE - BLOCK BREAK
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffBlockBreak(
            BlockBreakEvent event
    ) {

        Player player =
                event.getPlayer();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
            return;
        }

        if (!canBuild(
                player,
                event.getBlock().getLocation()
        )) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // STAFF MODE - BLOCK PLACE
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffBlockPlace(
            BlockPlaceEvent event
    ) {

        Player player =
                event.getPlayer();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
            return;
        }

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
    public void onInventoryOpen(
            InventoryOpenEvent event
    ) {

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getPlayer();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
            return;
        }

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
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInteract(
            PlayerInteractEvent event
    ) {

        Player player =
                event.getPlayer();

        /*
         * =====================================================
         * STAFF MODE
         * =====================================================
         */

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            /*
             * Solo procesamos herramientas
             * con clic derecho.
             */

            if (event.getAction().isRightClick()) {

                org.bukkit.inventory.ItemStack item =
                        event.getItem();

                if (item != null
                        && plugin.getStaffToolsManager() != null
                        && plugin.getStaffToolsManager()
                        .isStaffTool(item)) {

                    String name = "";

                    if (item.getItemMeta() != null
                            && item.getItemMeta()
                            .hasDisplayName()) {

                        name =
                                item.getItemMeta()
                                        .getDisplayName();
                    }

                    /*
                     * VANISH
                     */

                    if (name.equals(
                            ChatColor.GREEN + "Vanish"
                    )) {

                        if (plugin.getVanishManager() != null) {

                            boolean vanished =
                                    plugin.getVanishManager()
                                            .toggle(player);

                            if (vanished) {

                                player.sendMessage(
                                        ChatColor.GREEN +
                                                "Vanish activado."
                                );

                            } else {

                                player.sendMessage(
                                        ChatColor.RED +
                                                "Vanish desactivado."
                                );
                            }
                        }

                        event.setCancelled(true);
                        return;
                    }

                    /*
                     * DESACTIVAR STAFF MODE
                     */

                    if (name.equals(
                            ChatColor.RED +
                                    "Desactivar Staff Mode"
                    )) {

                        if (plugin.getVanishManager() != null
                                && plugin.getVanishManager()
                                .isVanished(player)) {

                            plugin.getVanishManager()
                                    .disable(player);
                        }

                        if (plugin.getStaffManager() != null) {

                            plugin.getStaffManager()
                                    .disable(player);
                        }

                        player.sendMessage(
                                ChatColor.RED +
                                        "Staff Mode desactivado."
                        );

                        event.setCancelled(true);
                        return;
                    }

                    /*
                     * OTROS STAFF TOOLS
                     *
                     * Los implementaremos
                     * progresivamente.
                     */

                    event.setCancelled(true);
                    return;
                }
            }

            /*
             * Staff Mode no puede interactuar
             * normalmente con el mundo.
             */

            event.setCancelled(true);
            return;
        }

        /*
         * =====================================================
         * NORMAL PLAYER INTERACTION
         * =====================================================
         */

        if (event.getClickedBlock() == null) {
            return;
        }

        Location location =
                event.getClickedBlock()
                        .getLocation();

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
    // STAFF MODE - DROP ITEMS
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffDrop(
            PlayerDropItemEvent event
    ) {

        Player player =
                event.getPlayer();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // STAFF MODE - PICKUP ITEMS
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getEntity();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // EXPLOSIONES
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onExplosion(
            EntityExplodeEvent event
    ) {

        Iterator<org.bukkit.block.Block> iterator =
                event.blockList().iterator();

        while (iterator.hasNext()) {

            org.bukkit.block.Block block =
                    iterator.next();

            if (plugin.getClaimManager()
                    .isClaimed(
                            block.getLocation()
                    )) {

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
                    .isClaimed(
                            block.getLocation()
                    )) {

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
                        event.getToBlock()
                                .getLocation()
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
                        event.getBlock()
                                .getLocation()
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

        if (damager instanceof Player) {

            attacker =
                    (Player) damager;

        } else if (damager instanceof Projectile) {

            Projectile projectile =
                    (Projectile) damager;

            if (projectile.getShooter()
                    instanceof Player) {

                attacker =
                        (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
            return;
        }

        /*
         * Staff no puede participar
         * en PvP.
         */

        if (plugin.getStaffManager() != null) {

            if (plugin.getStaffManager()
                    .isStaff(attacker)
                    || plugin.getStaffManager()
                    .isStaff(victim)) {

                event.setCancelled(true);
                return;
            }
        }

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

        if (event.isCancelled()) {
            return;
        }

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

        plugin.getFactionManager()
                .addDeath(player);

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        double oldDtr = 0.0;

        if (faction != null) {

            oldDtr =
                    faction.getDtr();
        }

        boolean dtrLost = false;

        if (faction != null) {

            dtrLost =
                    plugin.getFactionManager()
                            .handleDeath(player);

            double newDtr =
                    faction.getDtr();

            if (dtrLost) {

                player.sendMessage(
                        ChatColor.RED +
                                "☠ Tu faction perdió "
                                + formatDtr(
                                oldDtr - newDtr
                        )
                                + " DTR."
                );

                player.sendMessage(
                        ChatColor.RED +
                                "DTR actual: "
                                + formatDtr(newDtr)
                );

                if (newDtr <= 0.0
                        && oldDtr > 0.0) {

                    broadcastRaidable(faction);
                }
            }
        }

        if (plugin.getConfig()
                .getBoolean(
                        "deathban.enabled",
                        true
                )) {

            plugin.getDeathbanManager()
                    .deathban(player);
        }

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

        plugin.getCombatManager()
                .removeTag(player);

        if (killer != null) {

            plugin.getCombatManager()
                    .removeTag(killer);
        }

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

            return days + "d "
                    + hours + "h";
        }

        if (hours > 0) {

            return hours + "h "
                    + minutes + "m";
        }

        if (minutes > 0) {

            return minutes + "m "
                    + seconds + "s";
        }

        return seconds + "s";
    }
}