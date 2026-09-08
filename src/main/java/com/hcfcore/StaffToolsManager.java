package com.hcfcore;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaffToolsManager {

    private final HCFCore plugin;

    public StaffToolsManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /*
     * =========================================================
     * STAFF TOOLS
     * =========================================================
     */

    public void giveTools(Player player) {

        if (player == null) {
            return;
        }

        player.getInventory().clear();

        /*
         * VANISH
         */

        player.getInventory().setItem(
                0,
                createItem(
                        Material.LIME_DYE,
                        ChatColor.GREEN + "Vanish",
                        ChatColor.GRAY + "Clic derecho para activar/desactivar."
                )
        );

        /*
         * INSPECT
         */

        player.getInventory().setItem(
                1,
                createItem(
                        Material.CHEST,
                        ChatColor.GOLD + "Inspect",
                        ChatColor.GRAY + "Clic derecho sobre un jugador."
                )
        );

        /*
         * TELEPORT
         */

        player.getInventory().setItem(
                2,
                createItem(
                        Material.COMPASS,
                        ChatColor.AQUA + "Teleport",
                        ChatColor.GRAY + "Herramienta de teleport."
                )
        );

        /*
         * FREEZE
         */

        player.getInventory().setItem(
                3,
                createItem(
                        Material.PACKED_ICE,
                        ChatColor.BLUE + "Freeze",
                        ChatColor.GRAY + "Clic derecho sobre un jugador."
                )
        );

        /*
         * RANDOM TP
         */

        player.getInventory().setItem(
                4,
                createItem(
                        Material.ENDER_EYE,
                        ChatColor.LIGHT_PURPLE + "Random TP",
                        ChatColor.GRAY + "Teleport aleatorio."
                )
        );

        /*
         * DESACTIVAR STAFF
         */

        player.getInventory().setItem(
                8,
                createItem(
                        Material.RED_DYE,
                        ChatColor.RED + "Desactivar Staff Mode",
                        ChatColor.GRAY + "Clic derecho para salir."
                )
        );
    }

    /*
     * =========================================================
     * ITEM CREATOR
     * =========================================================
     */

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);

        if (lore.length > 0) {

            java.util.List<String> loreList =
                    new java.util.ArrayList<>();

            for (String line : lore) {
                loreList.add(line);
            }

            meta.setLore(loreList);
        }

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES
        );

        item.setItemMeta(meta);

        return item;
    }

    /*
     * =========================================================
     * CHECK STAFF TOOL
     * =========================================================
     */

    public boolean isStaffTool(ItemStack item) {

        if (item == null
                || item.getType() == Material.AIR
                || !item.hasItemMeta()) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null
                || !meta.hasDisplayName()) {

            return false;
        }

        String name =
                meta.getDisplayName();

        return name.equals(
                ChatColor.GREEN + "Vanish"
        )
                || name.equals(
                ChatColor.GOLD + "Inspect"
        )
                || name.equals(
                ChatColor.AQUA + "Teleport"
        )
                || name.equals(
                ChatColor.BLUE + "Freeze"
        )
                || name.equals(
                ChatColor.LIGHT_PURPLE + "Random TP"
        )
                || name.equals(
                ChatColor.RED + "Desactivar Staff Mode"
        );
    }

    /*
     * =========================================================
     * GET PLUGIN
     * =========================================================
     */

    public HCFCore getPlugin() {
        return plugin;
    }
}