package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

    public void open(Player player) {

        if (!isAdmin(player)) {
            player.sendMessage(
                    ChatColor.RED
                            + "No tienes permiso para usar el editor."
            );
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        MAIN_TITLE
                );

        inventory.setItem(
                10,
                createItem(
                        Material.CHEST,
                        ChatColor.GOLD + "🎁 Editar Loot",
                        ChatColor.GRAY
                                + "Editar los objetos del Airdrop"
                )
        );

        inventory.setItem(
                12,
                createItem(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⏱ Intervalo",
                        ChatColor.GRAY
                                + "Cada "
                                + plugin.getConfig()
                                .getInt(
                                        "airdrop.interval-minutes",
                                        30
                                )
                                + " minutos"
                )
        );

        inventory.setItem(
                14,
                createItem(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⌛ Duración",
                        ChatColor.GRAY
                                + plugin.getConfig()
                                .getInt(
                                        "airdrop.lifetime-seconds",
                                        180
                                )
                                + " segundos"
                )
        );

        inventory.setItem(
                16,
                createItem(
                        Material.BEACON,
                        ChatColor.AQUA + "📍 Ubicación",
                        ChatColor.GRAY
                                + "Los Airdrops aparecen"
                                + " fuera de claims"
                )
        );

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Cerrar",
                        ChatColor.GRAY
                                + "Cerrar editor"
                )
        );

        player.openInventory(inventory);
    }

    private void openLoot(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        LOOT_TITLE
                );

        List<String> loot =
                plugin.getConfig()
                        .getStringList(
                                "airdrop.loot"
                        );

        int slot = 0;

        for (String entry : loot) {

            if (slot >= 45) {
                break;
            }

            String[] parts =
                    entry.split(":");

            if (parts.length < 2) {
                continue;
            }

            Material material =
                    Material.matchMaterial(
                            parts[0].toUpperCase()
                    );

            if (material == null) {
                continue;
            }

            int amount = 1;

            try {
                amount =
                        Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }

            inventory.setItem(
                    slot++,
                    new ItemStack(
                            material,
                            Math.max(1, amount)
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.LIME_WOOL,
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY
                                + "Guardar el nuevo loot"
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Volver",
                        ChatColor.GRAY
                                + "Volver al editor"
                )
        );

        player.openInventory(inventory);
    }

    private void saveLoot(Player player) {

        Inventory inventory =
                player.getOpenInventory()
                        .getTopInventory();

        List<String> loot =
                new ArrayList<>();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack item =
                    inventory.getItem(slot);

            if (item == null
                    || item.getType()
                    == Material.AIR) {
                continue;
            }

            loot.add(
                    item.getType().name()
                            + ":"
                            + item.getAmount()
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

    private ItemStack createItem(
            Material material,
            String name,
            String lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            List<String> loreList =
                    new ArrayList<>();

            loreList.add(lore);

            meta.setLore(loreList);

            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isAdmin(Player player) {
        return player.hasPermission("hcf.admin");
    }

    @EventHandler
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String title =
                event.getView().getTitle();

        if (title.equals(MAIN_TITLE)) {

            event.setCancelled(true);

            if (!isAdmin(player)) {
                player.closeInventory();
                return;
            }

            if (event.getSlot() == 10) {

                openLoot(player);

            } else if (event.getSlot() == 22) {

                player.closeInventory();
            }

            return;
        }

        if (title.equals(LOOT_TITLE)) {

            if (event.getSlot() == 49) {

                event.setCancelled(true);

                saveLoot(player);

                return;
            }

            if (event.getSlot() == 53) {

                event.setCancelled(true);

                open(player);
            }
        }
    }

    @EventHandler
    public void onClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getPlayer();

        if (event.getView()
                .getTitle()
                .equals(LOOT_TITLE)) {

            // El loot solamente se guarda
            // cuando se pulsa GUARDAR.
        }
    }
}