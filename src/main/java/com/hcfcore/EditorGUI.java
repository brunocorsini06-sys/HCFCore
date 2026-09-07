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

public class EditorGUI implements Listener {

    private static final String TITLE =
            ChatColor.DARK_GRAY + "⚙ HCF Editor";

    private final HCFCore plugin;

    public EditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {

        if (!player.hasPermission("hcf.admin")) {
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
                        TITLE
                );

        inventory.setItem(
                10,
                createItem(
                        Material.CHEST,
                        ChatColor.GOLD + "☁ Airdrops",
                        ChatColor.GRAY + "Editar loot y configuración",
                        ChatColor.YELLOW + "Click para abrir"
                )
        );

        inventory.setItem(
                12,
                createItem(
                        Material.DIAMOND_SWORD,
                        ChatColor.RED + "⚔ Kits",
                        ChatColor.GRAY + "Crear y editar kits",
                        ChatColor.YELLOW + "Click para abrir"
                )
        );

        inventory.setItem(
                14,
                createItem(
                        Material.NETHER_STAR,
                        ChatColor.DARK_RED + "🏰 KOTH",
                        ChatColor.GRAY + "Editar KOTH",
                        ChatColor.GRAY + "Ubicación, captura y recompensas",
                        ChatColor.YELLOW + "Click para abrir"
                )
        );

        inventory.setItem(
                16,
                createItem(
                        Material.ENCHANTED_BOOK,
                        ChatColor.LIGHT_PURPLE + "📚 Aldeanos",
                        ChatColor.GRAY + "Editar bibliotecarios",
                        ChatColor.GRAY + "Encantamientos y precios",
                        ChatColor.YELLOW + "Click para abrir"
                )
        );

        inventory.setItem(
                22,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "✖ Cerrar"
                )
        );

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        Player player =
                (Player) event.getWhoClicked();

        if (!player.hasPermission("hcf.admin")) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {

            case 10:
                plugin.getGuiManager()
                        .openAirdrop(player);
                break;

            case 12:
                plugin.getGuiManager()
                        .openKit(player);
                break;

            case 14:
                plugin.getGuiManager()
                        .openKoth(player);
                break;

            case 16:
                plugin.getGuiManager()
                        .openVillager(player);
                break;

            case 22:
                player.closeInventory();
                break;

            default:
                break;
        }
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            List<String> lines =
                    new ArrayList<>();

            for (String line : lore) {
                lines.add(line);
            }

            meta.setLore(lines);

            item.setItemMeta(meta);
        }

        return item;
    }
}