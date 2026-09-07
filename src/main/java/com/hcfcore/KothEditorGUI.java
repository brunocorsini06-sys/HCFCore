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

public class KothEditorGUI implements Listener {

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_RED + "🏰 KOTH Editor";

    private static final String REWARD_TITLE =
            ChatColor.DARK_PURPLE + "🎁 KOTH Recompensas";

    private static final String CONFIG_TITLE =
            ChatColor.DARK_RED + "⚙ KOTH Config";

    public KothEditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

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

        inv.setItem(
                10,
                item(
                        Material.COMPASS,
                        ChatColor.AQUA + "📍 Ubicación",
                        ChatColor.GRAY +
                                "Guardar tu ubicación actual"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⏱ Tiempo de captura",
                        ChatColor.GRAY +
                                "Actualmente: " +
                                plugin.getConfig().getInt(
                                        "koth.capture-seconds",
                                        120
                                ) +
                                " segundos"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.SLIME_BALL,
                        ChatColor.GREEN + "📏 Radio",
                        ChatColor.GRAY +
                                "Configurar radio del KOTH"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.CHEST,
                        ChatColor.GOLD + "🎁 Recompensas",
                        ChatColor.GRAY +
                                "Editar las recompensas"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar configuración"
                )
        );

        player.openInventory(inv);
    }

    private void openRewards(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                REWARD_TITLE
        );

        String materialName =
                plugin.getConfig().getString(
                        "koth.reward-item",
                        "DIAMOND"
                );

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material != null) {

            int amount =
                    plugin.getConfig().getInt(
                            "koth.reward-item-amount",
                            4
                    );

            inv.setItem(
                    0,
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
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar recompensas"
                )
        );

        inv.setItem(
                53,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Volver",
                        ChatColor.GRAY +
                                "Volver al editor"
                )
        );

        player.openInventory(inv);
    }

    private void saveRewards(Player player) {

        Inventory inv =
                player.getOpenInventory()
                        .getTopInventory();

        ItemStack reward =
                inv.getItem(0);

        if (reward == null
                || reward.getType() == Material.AIR) {

            plugin.getConfig().set(
                    "koth.reward-item",
                    null
            );

            plugin.getConfig().set(
                    "koth.reward-item-amount",
                    0
            );

        } else {

            plugin.getConfig().set(
                    "koth.reward-item",
                    reward.getType().name()
            );

            plugin.getConfig().set(
                    "koth.reward-item-amount",
                    reward.getAmount()
            );
        }

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Recompensa del KOTH guardada."
        );
    }

    private void openConfig(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                CONFIG_TITLE
        );

        inv.setItem(
                11,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW +
                                "⏱ Tiempo de captura",
                        ChatColor.GRAY +
                                "Actual: " +
                                plugin.getConfig().getInt(
                                        "koth.capture-seconds",
                                        120
                                ) +
                                " segundos"
                )
        );

        inv.setItem(
                13,
                item(
                        Material.SLIME_BALL,
                        ChatColor.GREEN +
                                "📏 Radio",
                        ChatColor.GRAY +
                                "Actual: configurable"
                )
        );

        inv.setItem(
                15,
                item(
                        Material.GOLD_INGOT,
                        ChatColor.GOLD +
                                "💰 Dinero",
                        ChatColor.GRAY +
                                "Actual: $" +
                                plugin.getConfig().getDouble(
                                        "koth.reward-money",
                                        500
                                )
                )
        );

        inv.setItem(
                22,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW +
                                "← Volver",
                        ChatColor.GRAY +
                                "Volver"
                )
        );

        player.openInventory(inv);
    }

    private void saveLocation(Player player) {

        LocationStorage.setLocation(player);

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Ubicación del KOTH establecida "
                        + "en tu posición actual."
        );
    }

    private void saveMain(Player player) {

        if (!LocationStorage.hasLocation()) {

            player.sendMessage(
                    ChatColor.RED +
                            "Primero establece una ubicación."
            );

            return;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Configuración del KOTH guardada."
        );
    }

    private ItemStack item(
            Material material,
            String name,
            String lore
    ) {

        ItemStack stack =
                new ItemStack(material);

        ItemMeta meta =
                stack.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            List<String> loreList =
                    new ArrayList<>();

            loreList.add(lore);

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
                    saveLocation(player);
                    break;

                case 12:
                    openConfig(player);
                    break;

                case 14:
                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "El radio se configurará "
                                    + "en la siguiente versión "
                                    + "del menú."
                    );
                    break;

                case 16:
                    openRewards(player);
                    break;

                case 22:
                    saveMain(player);
                    break;

                default:
                    break;
            }

            return;
        }

        if (title.equals(REWARD_TITLE)) {

            if (event.getSlot() == 49) {

                event.setCancelled(true);
                saveRewards(player);

                return;
            }

            if (event.getSlot() == 53) {

                event.setCancelled(true);
                open(player);
            }

            return;
        }

        if (title.equals(CONFIG_TITLE)) {

            event.setCancelled(true);

            if (event.getSlot() == 22) {
                open(player);
            }
        }
    }

    @EventHandler
    public void onClose(
            InventoryCloseEvent event
    ) {

        // Los cambios solamente se guardan
        // cuando se pulsa el botón GUARDAR.
    }
}