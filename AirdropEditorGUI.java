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
            deny(player);
            return;
        }

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        27,
                        MAIN_TITLE
                );

        boolean enabled =
                plugin.getConfig()
                        .getBoolean(
                                "airdrop.enabled",
                                true
                        );

        inv.setItem(
                10,
                item(
                        Material.CHEST,
                        ChatColor.GOLD +
                                "🎁 Editar Loot",
                        ChatColor.GRAY +
                                "Editar los objetos del Airdrop"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW +
                                "⏱ Intervalo",
                        ChatColor.GRAY +
                                "Actual: "
                                + plugin.getConfig()
                                .getInt(
                                        "airdrop.interval-minutes",
                                        30
                                )
                                + " minutos",
                        ChatColor.GRAY +
                                "Click izquierdo: +5 min",
                        ChatColor.GRAY +
                                "Click derecho: -5 min"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW +
                                "⌛ Duración",
                        ChatColor.GRAY +
                                "Actual: "
                                + plugin.getConfig()
                                .getInt(
                                        "airdrop.lifetime-seconds",
                                        180
                                )
                                + " segundos",
                        ChatColor.GRAY +
                                "Click izquierdo: +30s",
                        ChatColor.GRAY +
                                "Click derecho: -30s"
                )
        );

        inv.setItem(
                16,
                item(
                        enabled
                                ? Material.LIME_WOOL
                                : Material.RED_WOOL,
                        enabled
                                ? ChatColor.GREEN +
                                "🟢 Airdrop ACTIVADO"
                                : ChatColor.RED +
                                "🔴 Airdrop DESACTIVADO",
                        ChatColor.GRAY +
                                "Click para cambiar"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.BARRIER,
                        ChatColor.RED +
                                "✖ Cerrar",
                        ChatColor.GRAY +
                                "Cerrar editor"
                )
        );

        player.openInventory(inv);
    }

    private void openLoot(Player player) {

        Inventory inv =
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

            if (parts.length == 0) {
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

            if (parts.length >= 2) {

                try {
                    amount =
                            Integer.parseInt(parts[1]);

                } catch (NumberFormatException ignored) {
                }
            }

            inv.setItem(
                    slot++,
                    new ItemStack(
                            material,
                            Math.max(1, amount)
                    )
            );
        }

        inv.setItem(
                49,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN +
                                "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar loot"
                )
        );

        inv.setItem(
                53,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW +
                                "← Volver",
                        ChatColor.GRAY +
                                "Volver al editor"
                )
        );

        player.openInventory(inv);
    }

    private void saveLoot(Player player) {

        Inventory inv =
                player.getOpenInventory()
                        .getTopInventory();

        List<String> loot =
                new ArrayList<>();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack item =
                    inv.getItem(slot);

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
                ChatColor.GREEN +
                        "✓ Loot del Airdrop guardado."
        );
    }

    private void changeInterval(
            Player player,
            int amount
    ) {

        int current =
                plugin.getConfig()
                        .getInt(
                                "airdrop.interval-minutes",
                                30
                        );

        int newValue =
                Math.max(
                        1,
                        current + amount
                );

        plugin.getConfig().set(
                "airdrop.interval-minutes",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Intervalo: "
                        + newValue
                        + " minutos."
        );

        open(player);
    }

    private void changeLifetime(
            Player player,
            int amount
    ) {

        int current =
                plugin.getConfig()
                        .getInt(
                                "airdrop.lifetime-seconds",
                                180
                        );

        int newValue =
                Math.max(
                        10,
                        current + amount
                );

        plugin.getConfig().set(
                "airdrop.lifetime-seconds",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Duración: "
                        + newValue
                        + " segundos."
        );

        open(player);
    }

    private void toggleAirdrop(
            Player player
    ) {

        boolean current =
                plugin.getConfig()
                        .getBoolean(
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
                        ? ChatColor.GREEN +
                        "✓ Airdrops activados."
                        : ChatColor.RED +
                        "✓ Airdrops desactivados."
        );

        open(player);
    }

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

    private boolean isAdmin(Player player) {
        return player.hasPermission("hcf.admin");
    }

    private void deny(Player player) {

        player.sendMessage(
                ChatColor.RED +
                        "No tienes permiso para usar el editor."
        );
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

            switch (event.getSlot()) {

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

        if (title.equals(LOOT_TITLE)) {

            /*
             * Los slots 0-44 son totalmente editables.
             */

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

        // El loot solo se guarda pulsando GUARDAR.
    }
}