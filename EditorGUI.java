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

import java.util.*;

public class EditorGUI implements Listener {

    private final HCFCore plugin;

    private final Map<UUID, String> editingKit = new HashMap<>();
    private final Map<UUID, String> editingKoth = new HashMap<>();

    public EditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {

        if (!player.hasPermission("hcf.admin")) {
            player.sendMessage(
                    ChatColor.RED + "No tienes permiso."
            );
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_GRAY + "HCF Editor"
        );

        inv.setItem(
                10,
                item(
                        Material.CHEST,
                        ChatColor.GOLD + "✈ Airdrops",
                        ChatColor.GRAY + "Editar Airdrops"
                )
        );

        inv.setItem(
                13,
                item(
                        Material.ENDER_CHEST,
                        ChatColor.GREEN + "🎒 Kits",
                        ChatColor.GRAY + "Editar Kits"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.BEACON,
                        ChatColor.RED + "🏰 KOTH",
                        ChatColor.GRAY + "Editar KOTH"
                )
        );

        player.openInventory(inv);
    }

    public void openKitMenu(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_GREEN + "Kit Editor"
        );

        int slot = 10;

        for (String kit :
                plugin.getConfig()
                        .getConfigurationSection("kits")
                        .getKeys(false)) {

            if (slot >= 17) {
                break;
            }

            inv.setItem(
                    slot++,
                    item(
                            Material.CHEST,
                            ChatColor.GREEN + kit,
                            ChatColor.GRAY
                                    + "Click para editar"
                    )
            );
        }

        inv.setItem(
                22,
                item(
                        Material.EMERALD,
                        ChatColor.GREEN
                                + "➕ Crear Kit",
                        ChatColor.GRAY
                                + "Crear un nuevo kit"
                )
        );

        player.openInventory(inv);
    }

    public void openKothMenu(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_RED + "KOTH Editor"
        );

        inv.setItem(
                11,
                item(
                        Material.BEACON,
                        ChatColor.RED
                                + "🏰 KOTH Principal",
                        ChatColor.GRAY
                                + "Editar KOTH"
                )
        );

        inv.setItem(
                15,
                item(
                        Material.EMERALD,
                        ChatColor.GREEN
                                + "➕ Crear KOTH",
                        ChatColor.GRAY
                                + "Crear un nuevo KOTH"
                )
        );

        player.openInventory(inv);
    }

    public void openKitEditor(
            Player player,
            String kitName
    ) {

        editingKit.put(
                player.getUniqueId(),
                kitName
        );

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_GREEN
                        + "Editando Kit: "
                        + kitName
        );

        player.openInventory(inv);

        loadKitItems(
                inv,
                kitName
        );
    }

    private void loadKitItems(
            Inventory inv,
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
                        ChatColor.GREEN
                                + "💾 GUARDAR",
                        ChatColor.GRAY
                                + "Guardar el kit"
                )
        );

        inv.setItem(
                53,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW
                                + "← Volver",
                        ChatColor.GRAY
                                + "Volver al menú"
                )
        );
    }

    public void openKothEditor(Player player) {

        editingKoth.put(
                player.getUniqueId(),
                "main"
        );

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_RED
                        + "Editando KOTH"
        );

        inv.setItem(
                10,
                item(
                        Material.COMPASS,
                        ChatColor.AQUA
                                + "📍 Ubicación",
                        ChatColor.GRAY
                                + "Usar mi ubicación"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW
                                + "⏱ Captura",
                        ChatColor.GRAY
                                + "Tiempo de captura"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.SLIME_BALL,
                        ChatColor.GREEN
                                + "📏 Radio",
                        ChatColor.GRAY
                                + "Radio de captura"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.CHEST,
                        ChatColor.GOLD
                                + "🎁 Recompensas",
                        ChatColor.GRAY
                                + "Editar recompensas"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN
                                + "💾 GUARDAR",
                        ChatColor.GRAY
                                + "Guardar KOTH"
                )
        );

        player.openInventory(inv);
    }

    private ItemStack item(
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

            meta.setLore(
                    Collections.singletonList(
                            lore
                    )
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String title =
                event.getView()
                        .getTitle();

        if (title.equals(
                ChatColor.DARK_GRAY
                        + "HCF Editor"
        )) {

            event.setCancelled(true);

            if (event.getSlot() == 10) {

                player.performCommand(
                        "airdrop edit"
                );

            } else if (event.getSlot() == 13) {

                openKitMenu(player);

            } else if (event.getSlot() == 16) {

                openKothMenu(player);
            }

            return;
        }

        if (title.equals(
                ChatColor.DARK_GREEN
                        + "Kit Editor"
        )) {

            event.setCancelled(true);

            if (event.getSlot() == 22) {

                player.sendMessage(
                        ChatColor.YELLOW
                                + "La creación de kits "
                                + "se añadirá en el siguiente "
                                + "menú."
                );

                return;
            }

            ItemStack clicked =
                    event.getCurrentItem();

            if (clicked == null
                    || !clicked.hasItemMeta()
                    || clicked.getItemMeta()
                    .getDisplayName() == null) {
                return;
            }

            String kit =
                    ChatColor.stripColor(
                            clicked.getItemMeta()
                                    .getDisplayName()
                    );

            if (plugin.getKitManager()
                    .kitExists(kit)) {

                openKitEditor(
                        player,
                        kit
                );
            }

            return;
        }

        if (title.startsWith(
                ChatColor.DARK_GREEN
                        + "Editando Kit:"
        )) {

            if (event.getSlot() == 49) {

                event.setCancelled(true);

                saveKit(
                        player
                );

                return;
            }

            if (event.getSlot() == 53) {

                event.setCancelled(true);

                openKitMenu(player);
            }

            return;
        }

        if (title.equals(
                ChatColor.DARK_RED
                        + "KOTH Editor"
        )) {

            event.setCancelled(true);

            if (event.getSlot() == 11) {

                openKothEditor(player);

            } else if (event.getSlot() == 15) {

                player.sendMessage(
                        ChatColor.YELLOW
                                + "La creación de KOTH "
                                + "se añadirá después."
                );
            }

            return;
        }

        if (title.equals(
                ChatColor.DARK_RED
                        + "Editando KOTH"
        )) {

            event.setCancelled(true);

            if (event.getSlot() == 10) {

                LocationStorage.setLocation(
                        player
                );

                player.sendMessage(
                        ChatColor.GREEN
                                + "✓ Ubicación del KOTH "
                                + "establecida."
                );

            } else if (event.getSlot() == 22) {

                player.sendMessage(
                        ChatColor.GREEN
                                + "✓ KOTH guardado."
                );

                player.closeInventory();
            }
        }
    }

    private void saveKit(Player player) {

        String kit =
                editingKit.get(
                        player.getUniqueId()
                );

        if (kit == null) {
            return;
        }

        List<String> items =
                new ArrayList<>();

        Inventory inv =
                player.getOpenInventory()
                        .getTopInventory();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack item =
                    inv.getItem(slot);

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
                "kits."
                        + kit
                        + ".items",
                items
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Kit "
                        + kit
                        + " guardado correctamente."
        );
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        Player player =
                (Player) event.getPlayer();

        String title =
                event.getView()
                        .getTitle();

        if (title.startsWith(
                ChatColor.DARK_GREEN
                        + "Editando Kit:"
        )) {

            editingKit.remove(
                    player.getUniqueId()
            );
        }

        if (title.equals(
                ChatColor.DARK_RED
                        + "Editando KOTH"
        )) {

            editingKoth.remove(
                    player.getUniqueId()
            );
        }
    }
}