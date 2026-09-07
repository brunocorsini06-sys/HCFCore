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
                