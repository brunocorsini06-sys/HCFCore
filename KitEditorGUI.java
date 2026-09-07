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

public class KitEditorGUI implements Listener {

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_GREEN + "🎒 Kit Editor";

    private final String PREFIX =
            ChatColor.DARK_GREEN + "Editando Kit: ";

    public KitEditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        if (!isAdmin(player)) {
            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso para usar el editor."
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
                        ChatColor.GREEN + "🎒 Starter",
                        ChatColor.GRAY +
                                "Editar kit Starter"
                )
        );

        inventory.setItem(
                12,
                createItem(
                        Material.BOW,
                        ChatColor.AQUA + "🏹 Archer",
                        ChatColor.GRAY +
                                "Editar kit Archer"
                )
        );

        inventory.setItem(
                16,
                createItem(
                        Material.EMERALD,
                        ChatColor.GREEN + "➕ Crear Kit",
                        ChatColor.GRAY +
                                "Crear un nuevo kit"
                )
        );

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Cerrar",
                        ChatColor.GRAY +
                                "Cerrar editor"
                )
        );

        player.openInventory(inventory);
    }

    public void openEditor(
            Player player,
            String kitName
    ) {

        if (!isAdmin(player)) {
            return;
        }

        String path =
                "kits." + kitName.toLowerCase();

        if (!plugin.getConfig().contains(path)) {

            player.sendMessage(
                    ChatColor.RED +
                            "Ese kit no existe."
            );

            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        PREFIX + kitName
                );

        loadKit(
                inventory,
                kitName
        );

        inventory.setItem(
                45,
                createItem(
                        Material.CLOCK,
                        ChatColor.YELLOW +
                                "⏱ Cooldown",
                        ChatColor.GRAY +
                                "Cooldown actual: "
                                + plugin.getConfig()
                                .getLong(
                                        path +
                                                ".cooldown-seconds",
                                        86400
                                )
                                + " segundos"
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.LIME_WOOL,
                        ChatColor.GREEN +
                                "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar contenido del kit"
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW +
                                "← Volver",
                        ChatColor.GRAY +
                                "Volver"
                )
        );

        player.openInventory(inventory);
    }

    private void loadKit(
            Inventory inventory,
            String kitName
    ) {

        List<String> items =
                plugin.getConfig()
                        .getStringList(
                                "kits."
                                        + kitName
                                        + ".items"
                        );

        int slot = 0;

        for (String entry : items) {

            if (slot >= 45) {
                break;
            }

            String[] parts =
                    entry.split(":");

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
                            Integer.parseInt(
                                    parts[1]
                            );
                } catch (
                        NumberFormatException ignored
                ) {
                }
            }

            inventory.setItem(
                    slot++,
                    new ItemStack(
                            material,
                            Math.max(
                                    1,
                                    amount
                            )
                    )
            );
        }
    }

    private void saveKit(
            Player player
    ) {

        String title =
                player.getOpenInventory()
                        .getTitle();

        if (!title.startsWith(PREFIX)) {
            return;
        }

        String kitName =
                ChatColor.stripColor(
                        title.substring(
                                PREFIX.length()
                        )
                );

        String path =
                "kits." +
                        kitName.toLowerCase();

        Inventory inventory =
                player.getOpenInventory()
                        .getTopInventory();

        List<String> items =
                new ArrayList<>();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack item =
                    inventory.getItem(slot);

            if (item == null
                    || item.getType()
                    == Material.AIR) {
                continue;
            }

            items.add(
                    item.getType().name()
                            + ":"
                            + item.getAmount()
            );
        }

        plugin.getConfig().set(
                path + ".items",
                items
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Kit " +
                        kitName +
                        " guardado correctamente."
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

                openEditor(
                        player,
                        "starter"
                );

            } else if (event.getSlot() == 12) {

                openEditor(
                        player,
                        "archer"
                );

            } else if (event.getSlot() == 16) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "Para crear un kit nuevo " +
                                "usaremos el comando /kit create."
                );

            } else if (event.getSlot() == 22) {

                player.closeInventory();
            }

            return;
        }

        if (title.startsWith(PREFIX)) {

            if (event.getSlot() == 49) {

                event.setCancelled(true);

                saveKit(player);

                return;
            }

            if (event.getSlot() == 53) {

                event.setCancelled(true);

                open(player);
            }
        }
    }
}