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

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_GRAY + "⚙ HCF Editor";

    public EditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {

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

        inv.setItem(
                10,
                item(
                        Material.CHEST,
                        ChatColor.GOLD
                                + "☁ Airdrops",
                        ChatColor.GRAY
                                + "Editar loot y configuración",
                        ChatColor.YELLOW
                                + "Click para abrir"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.DIAMOND_SWORD,
                        ChatColor.RED
                                + "⚔ Kits",
                        ChatColor.GRAY
                                + "Crear y editar kits",
                        ChatColor.YELLOW
                                + "Click para abrir"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.NETHER_STAR,
                        ChatColor.DARK_RED
                                + "🏰 KOTH",
                        ChatColor.GRAY
                                + "Editar ubicación y recompensas",
                        ChatColor.YELLOW
                                + "Click para abrir"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.ENCHANTED_BOOK,
                        ChatColor.LIGHT_PURPLE
                                + "📚 Aldeanos",
                        ChatColor.GRAY
                                + "Editar trades de bibliotecarios",
                        ChatColor.GRAY
                                + "Encantamientos, niveles y precios",
                        ChatColor.YELLOW
                                + "Click para abrir"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.BARRIER,
                        ChatColor.RED
                                + "✖ Cerrar"
                )
        );

        player.openInventory(inv);
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

        return player.hasPermission(
                "hcf.admin"
        );
    }

    private void deny(Player player) {

        player.sendMessage(
                ChatColor.RED
                        + "No tienes permiso para usar el editor."
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

        if (!event.getView()
                .getTitle()
                .equals(MAIN_TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player =
                (Player) event.getWhoClicked();

        if (!isAdmin(player)) {

            player.closeInventory();
            return;
        }

        int slot =
                event.getRawSlot();

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
}