package com.hcfcore;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
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
import java.util.Locale;

public class HCFListener implements Listener {

    private final HCFCore plugin;

    public HCFListener(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // JOIN
    // =========================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (plugin.getVanishManager() != null) {
            plugin.getVanishManager().handleJoin(player);
        }

        plugin.getDeathbanManager().setupPlayer(player);

        plugin.getScoreboardManager().update(player);

        if (plugin.getKothManager().isActive()
                && plugin.getKothManager().getBossBar() != null) {

            plugin.getKothManager()
                    .getBossBar()
                    .addPlayer(player);
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
    // QUIT
    // =========================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (plugin.getVanishManager() != null) {
            plugin.getVanishManager().handleQuit(player);
        }

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            plugin.getStaffManager().handleQuit(player);
        }

        if (plugin.getCombatManager().isInCombat(player)) {
            plugin.getCombatManager().handleQuit(player);
        }

        if (plugin.getKothManager().getBossBar() != null) {
            plugin.getKothManager()
                    .getBossBar()
                    .removePlayer(player);
        }
    }

    // =========================================================
    // DAMAGE / PVP
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim =
                (Player) event.getEntity();

        Player attacker =
                getAttackingPlayer(event.getDamager());

        if (attacker == null) {
            return;
        }

        // -----------------------------------------------------
        // STAFF
        // -----------------------------------------------------

        if (plugin.getStaffManager() != null) {

            if (plugin.getStaffManager().isStaff(victim)
                    || plugin.getStaffManager().isStaff(attacker)) {

                event.setCancelled(true);
                return;
            }
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
                && attackerFaction.getName()
                .equalsIgnoreCase(victimFaction.getName())) {

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
        // COMBAT TAG
        // -----------------------------------------------------

        plugin.getCombatManager().tag(attacker);
        plugin.getCombatManager().tag(victim);
    }

    // =========================================================
    // STAFF DAMAGE
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onStaffDamage(EntityDamageByEntityEvent event) {

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

        Player attacker =
                getAttackingPlayer(event.getDamager());

        if (attacker != null
                && plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(attacker)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // BLOCK BREAK
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {

        Player player =
                event.getPlayer();

        // Staff Mode
        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
            return;
        }

        Location location =
                event.getBlock().getLocation();

        if (!canBuild(player, location)) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // BLOCK PLACE
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player =
                event.getPlayer();

        // Staff Mode
        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
            return;
        }

        Location location =
                event.getBlock().getLocation();

        if (!canBuild(player, location)) {

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
    public void onInteract(PlayerInteractEvent event) {

        Player player =
                event.getPlayer();

        // -----------------------------------------------------
        // STAFF MODE
        // -----------------------------------------------------

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            if (event.getAction().isRightClick()) {

                org.bukkit.inventory.ItemStack item =
                        event.getItem();

                if (item != null
                        && plugin.getStaffToolsManager() != null
                        && plugin.getStaffToolsManager()
                        .isStaffTool(item)) {

                    String name = "";

                    if (item.getItemMeta() != null
                            && item.getItemMeta().hasDisplayName()) {

                        name =
                                item.getItemMeta()
                                        .getDisplayName();
                    }

                    // VANISH
                    if (name.equals(
                            ChatColor.GREEN + "Vanish"
                    )) {

                        if (plugin.getVanishManager() != null) {

                            boolean vanished =
                                    plugin.getVanishManager()
                                            .toggle(player);

                            player.sendMessage(
                                    vanished
                                            ? ChatColor.GREEN
                                            + "Vanish activado."
                                            : ChatColor.RED
                                            + "Vanish desactivado."
                            );
                        }

                        event.setCancelled(true);
                        return;
                    }

                    // DESACTIVAR STAFF
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

                    event.setCancelled(true);
                    return;
                }
            }

            event.setCancelled(true);
            return;
        }

        // -----------------------------------------------------
        // NORMAL PLAYER
        // -----------------------------------------------------

        Block clicked =
                event.getClickedBlock();

        if (clicked == null) {
            return;
        }

        Location location =
                clicked.getLocation();

        if (canBuild(player, location)) {
            return;
        }

        if (isProtectedInteractBlock(
                clicked.getType()
        )) {

            event.setCancelled(true);
            sendClaimDenied(player);
        }
    }

    // =========================================================
    // DROP
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDrop(PlayerDropItemEvent event) {

        Player player =
                event.getPlayer();

        if (plugin.getStaffManager() != null
                && plugin.getStaffManager().isStaff(player)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // PICKUP
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPickup(EntityPickupItemEvent event) {

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
    // EXPLOSIONS
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onExplosion(EntityExplodeEvent event) {

        Iterator<Block> iterator =
                event.blockList().iterator();

        while (iterator.hasNext()) {

            Block block =
                    iterator.next();

            if (plugin.getClaimManager()
                    .isClaimed(block.getLocation())) {

                iterator.remove();
            }
        }
    }

    // =========================================================
    // PISTON EXTEND
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonExtend(
            BlockPistonExtendEvent event
    ) {

        for (Block block : event.getBlocks()) {

            Location destination =
                    block.getLocation().clone().add(
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
    // PISTON RETRACT
    // =========================================================

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonRetract(
            BlockPistonRetractEvent event
    ) {

        for (Block block : event.getBlocks()) {

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
    // LIQUID FLOW
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
    // FIRE
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
    // DEATH
    // =========================================================

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player player =
                event.getEntity();

        plugin.getFactionManager()
                .addDeath(player);

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        double oldDtr = 0.0;

        if (faction != null) {
            oldDtr = faction.getDtr();
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
    // HELPERS
    // =========================================================

    private Player getAttackingPlayer(Entity damager) {

        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {

            Projectile projectile =
                    (Projectile) damager;

            if (projectile.getShooter()
                    instanceof Player) {

                return (Player) projectile.getShooter();
            }
        }

        return null;
    }

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

    private boolean isProtectedInteractBlock(
            Material material
    ) {

        String name =
                material.name();

        // Contenedores y máquinas
        if (name.contains("CHEST")
                || name.contains("BARREL")
                || name.contains("FURNACE")
                || name.contains("HOPPER")
                || name.contains("DROPPER")
                || name.contains("DISPENSER")
                || name.contains("BREWING")
                || name.contains("SHULKER")) {

            return true;
        }

        // Mesas / estaciones
        switch (material) {

            case CRAFTING_TABLE:
            case ENCHANTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case SMITHING_TABLE:
            case STONECUTTER:
            case LOOM:
            case CARTOGRAPHY_TABLE:
            case FLETCHING_TABLE:
            case GRINDSTONE:
            case BEACON:
            case JUKEBOX:
            case LEVER:
            case STONE_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
                return true;

            default:
                break;
        }

        // Puertas, trampillas y vallas
        if (name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE")) {

            return true;
        }

        // Botones de madera
        if (name.endsWith("_BUTTON")) {
            return true;
        }

        return false;
    }

    private void sendClaimDenied(Player player) {

        player.sendMessage(
                ChatColor.RED +
                        "No puedes hacer eso dentro del claim de otra faction."
        );
    }

    private void broadcastRaidable(Faction faction) {

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

    private String formatDtr(double value) {

        return String.format(
                Locale.US,
                "%.1f",
                Math.max(0.0, value)
        );
    }

    private String formatTime(long seconds) {

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