package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AirdropEditorGUI implements Listener {

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_AQUA + "✈ Airdrop Editor";

    private static final String LOOT_TITLE =
            ChatColor.DARK_GREEN + "✈ Airdrop Loot";

    public AirdropEditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    public void open(Player player) {

        if (!isAdmin(player)) {
            deny(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                MAIN_TITLE
        );

        boolean enabled = plugin.getConfig().getBoolean(
                "airdrop.enabled",
                true
        );

        inv.setItem(
                10,
                item(
                        Material.CHEST,
                        ChatColor.GOLD + "🎁 Editar Loot",
                        ChatColor.GRAY + "Editar los objetos del Airdrop",
                        ChatColor.YELLOW + "Click para abrir"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⏱ Intervalo",
                        ChatColor.GRAY + "Actual: "
                                + plugin.getConfig().getInt(
                                "airdrop.interval-minutes",
                                30
                        )
                                + " minutos",
                        ChatColor.GRAY + "Click izquierdo: +5 min",
                        ChatColor.GRAY + "Click derecho: -5 min"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⌛ Duración",
                        ChatColor.GRAY + "Actual: "
                                + plugin.getConfig().getInt(
                                "airdrop.lifetime-seconds",
                                180
                        )
                                + " segundos",
                        ChatColor.GRAY + "Click izquierdo: +30s",
                        ChatColor.GRAY + "Click derecho: -30s"
                )
        );

        inv.setItem(
                16,
                item(
                        enabled
                                ? Material.LIME_WOOL
                                : Material.RED_WOOL,
                        enabled
                                ? ChatColor.GREEN + "🟢 Airdrop ACTIVADO"
                                : ChatColor.RED + "🔴 Airdrop DESACTIVADO",
                        ChatColor.GRAY + "Click para cambiar"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.BARRIER,
                        ChatColor.RED + "✖ Cerrar",
                        ChatColor.GRAY + "Cerrar editor"
                )
        );

        player.openInventory(inv);
    }

    // =========================================================
    // LOOT MENU
    // =========================================================

    private void openLoot(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                LOOT_TITLE
        );

        List<String> loot = plugin.getConfig().getStringList(
                "airdrop.loot"
        );

        int slot = 0;

        for (String entry : loot) {

            if (slot >= 45) {
                break;
            }

            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }

            String[] parts = entry.split(":");

            Material material = Material.matchMaterial(
                    parts[0].trim().toUpperCase()
            );

            if (material == null
                    || material == Material.AIR) {
                continue;
            }

            int amount = 1;

            if (parts.length >= 2) {

                try {
                    amount = Integer.parseInt(
                            parts[1].trim()
                    );
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }

            amount = Math.max(1, amount);

            /*
             * Bukkit no permite cantidades superiores al
             * máximo de stack del material.
             */
            amount = Math.min(
                    amount,
                    material.getMaxStackSize()
            );

            inv.setItem(
                    slot,
                    new ItemStack(
                            material,
                            amount
                    )
            );

            slot++;
        }

        /*
         * Botones inferiores.
         */
        inv.setItem(
                49,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY + "Guardar loot del Airdrop"
                )
        );

        inv.setItem(
                53,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Volver",
                        ChatColor.GRAY + "Volver al editor"
                )
        );

        player.openInventory(inv);
    }

    // =========================================================
    // SAVE LOOT
    // =========================================================

    private void saveLoot(Player player) {

        Inventory inv =
                player.getOpenInventory().getTopInventory();

        List<String> loot = new ArrayList<>();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack stack = inv.getItem(slot);

            if (stack == null
                    || stack.getType() == Material.AIR) {
                continue;
            }

            loot.add(
                    stack.getType().name()
                            + ":"
                            + stack.getAmount()
            );
        }

        plugin.getConfig().set(
                "airdrop.loot",
                loot
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Loot del Airdrop guardado."
        );
    }

    // =========================================================
    // INTERVAL
    // =========================================================

    private void changeInterval(
            Player player,
            int amount
    ) {

        int current = plugin.getConfig().getInt(
                "airdrop.interval-minutes",
                30
        );

        int newValue = Math.max(
                1,
                current + amount
        );

        plugin.getConfig().set(
                "airdrop.interval-minutes",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Intervalo: "
                        + newValue
                        + " minutos."
        );

        open(player);
    }

    // =========================================================
    // LIFETIME
    // =========================================================

    private void changeLifetime(
            Player player,
            int amount
    ) {

        int current = plugin.getConfig().getInt(
                "airdrop.lifetime-seconds",
                180
        );

        int newValue = Math.max(
                10,
                current + amount
        );

        plugin.getConfig().set(
                "airdrop.lifetime-seconds",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Duración: "
                        + newValue
                        + " segundos."
        );

        open(player);
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    private void toggleAirdrop(Player player) {

        boolean current = plugin.getConfig().getBoolean(
                "airdrop.enabled",
                true
        );

        boolean newValue = !current;

        plugin.getConfig().set(
                "airdrop.enabled",
                newValue
        );

        plugin.saveConfig();

        if (!newValue) {

            plugin.getAirdropManager()
                    .removeActiveDrop();

        } else {

            plugin.getAirdropManager()
                    .startScheduler();
        }

        player.sendMessage(
                newValue
                        ? ChatColor.GREEN
                        + "✓ Airdrops activados."
                        : ChatColor.RED
                        + "✓ Airdrops desactivados."
        );

        open(player);
    }

    // =========================================================
    // ITEM
    // =========================================================

    private ItemStack item(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack stack =
                new ItemStack(material);

        ItemMeta meta =
                stack.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            List<String> loreList =
                    new ArrayList<>();

            for (String line : lore) {
                loreList.add(line);
            }

            meta.setLore(loreList);

            stack.setItemMeta(meta);
        }

        return stack;
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private boolean isAdmin(Player player) {

        return player != null
                && player.hasPermission("hcf.admin");
    }

    private void deny(Player player) {

        if (player == null) {
            return;
        }

        player.sendMessage(
                ChatColor.RED
                        + "No tienes permiso para usar el editor."
        );
    }

    // =========================================================
    // INVENTORY CLICK
    // =========================================================

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String title =
                event.getView().getTitle();

        // =====================================================
        // MAIN MENU
        // =====================================================

        if (title.equals(MAIN_TITLE)) {

            /*
             * Todo el inventario del editor está protegido.
             */
            event.setCancelled(true);

            if (!isAdmin(player)) {
                player.closeInventory();
                return;
            }

            /*
             * Solo procesamos clicks dentro del inventario
             * superior.
             */
            if (event.getRawSlot() < 0
                    || event.getRawSlot()
                    >= event.getView()
                    .getTopInventory()
                    .getSize()) {
                return;
            }

            switch (event.getRawSlot()) {

                case 10:

                    openLoot(player);
                    break;

                case 12:

                    if (event.isLeftClick()) {

                        changeInterval(
                                player,
                                5
                        );

                    } else if (event.isRightClick()) {

                        changeInterval(
                                player,
                                -5
                        );
                    }

                    break;

                case 14:

                    if (event.isLeftClick()) {

                        changeLifetime(
                                player,
                                30
                        );

                    } else if (event.isRightClick()) {

                        changeLifetime(
                                player,
                                -30
                        );
                    }

                    break;

                case 16:

                    toggleAirdrop(player);
                    break;

                case 22:

                    player.closeInventory();
                    break;

                default:
                    break;
            }

            return;
        }

        // =====================================================
        // LOOT MENU
        // =====================================================

        if (title.equals(LOOT_TITLE)) {

            /*
             * Protegemos únicamente los slots inferiores.
             *
             * Los slots 0-44 quedan editables.
             */
            if (event.getRawSlot() >= 45) {

                event.setCancelled(true);

                if (event.getRawSlot() == 49) {

                    saveLoot(player);
                    return;
                }

                if (event.getRawSlot() == 53) {

                    open(player);
                    return;
                }

                return;
            }

            /*
             * Si el click viene del inventario del jugador
             * no lo bloqueamos.
             *
             * Esto permite colocar objetos en los slots 0-44.
             */
            if (event.getRawSlot() < 0) {
                return;
            }
        }
    }
}