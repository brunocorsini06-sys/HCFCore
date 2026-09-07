package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

        Location location =
                plugin.getKothManager().getLocation();

        String locationText =
                location == null
                        ? "No configurada"
                        : location.getBlockX()
                        + ", "
                        + location.getBlockY()
                        + ", "
                        + location.getBlockZ();

        inv.setItem(
                10,
                item(
                        Material.COMPASS,
                        ChatColor.AQUA + "📍 Ubicación",
                        ChatColor.GRAY +
                                "Usar tu ubicación actual",
                        ChatColor.GRAY +
                                "Actual: " + locationText
                )
        );

        inv.setItem(
                12,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⏱ Tiempo de captura",
                        ChatColor.GRAY +
                                "Actual: "
                                + getCaptureSeconds()
                                + " segundos",
                        ChatColor.GRAY +
                                "Click izquierdo: +10s",
                        ChatColor.GRAY +
                                "Click derecho: -10s"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.SLIME_BALL,
                        ChatColor.GREEN + "📏 Radio",
                        ChatColor.GRAY +
                                "Actual: "
                                + getRadius()
                                + " bloques",
                        ChatColor.GRAY +
                                "Click izquierdo: +1",
                        ChatColor.GRAY +
                                "Click derecho: -1"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.CHEST,
                        ChatColor.GOLD + "🎁 Recompensas",
                        ChatColor.GRAY +
                                "Editar dinero e ítem"
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
                Material.matchMaterial(materialName);

        int amount =
                plugin.getConfig().getInt(
                        "koth.reward-item-amount",
                        4
                );

        if (material != null && amount > 0) {

            inv.setItem(
                    0,
                    new ItemStack(
                            material,
                            Math.max(1, amount)
                    )
            );
        }

        inv.setItem(
                45,
                item(
                        Material.GOLD_INGOT,
                        ChatColor.GOLD + "💰 Dinero",
                        ChatColor.GRAY +
                                "Actual: $"
                                + getRewardMoney(),
                        ChatColor.GRAY +
                                "Click izquierdo: +100",
                        ChatColor.GRAY +
                                "Click derecho: -100"
                )
        );

        inv.setItem(
                49,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar recompensa"
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

    private void saveMain(Player player) {

        Location location =
                plugin.getKothManager().getLocation();

        if (location == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Primero establece una ubicación."
            );

            return;
        }

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ KOTH guardado correctamente."
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Ubicación: "
                        + location.getBlockX()
                        + ", "
                        + location.getBlockY()
                        + ", "
                        + location.getBlockZ()
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Captura: "
                        + getCaptureSeconds()
                        + "s | Radio: "
                        + getRadius()
                        + " bloques"
        );
    }

    private void setLocation(Player player) {

        Location location =
                player.getLocation().clone();

        plugin.getKothManager()
                .setLocation(location);

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Ubicación del KOTH establecida."
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "X: " + location.getBlockX()
                        + " Y: " + location.getBlockY()
                        + " Z: " + location.getBlockZ()
        );
    }

    private void changeCaptureTime(
            Player player,
            int amount
    ) {

        int current =
                getCaptureSeconds();

        int newValue =
                Math.max(
                        10,
                        current + amount
                );

        plugin.getConfig().set(
                "koth.capture-seconds",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Tiempo de captura: "
                        + newValue
                        + " segundos."
        );

        open(player);
    }

    private void changeRadius(
            Player player,
            double amount
    ) {

        double current =
                getRadius();

        double newValue =
                Math.max(
                        1.0,
                        current + amount
                );

        plugin.getConfig().set(
                "koth.radius",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Radio del KOTH: "
                        + newValue
                        + " bloques."
        );

        open(player);
    }

    private void changeMoney(
            Player player,
            double amount
    ) {

        double current =
                getRewardMoney();

        double newValue =
                Math.max(
                        0,
                        current + amount
                );

        plugin.getConfig().set(
                "koth.reward-money",
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Recompensa: $"
                        + newValue
        );

        openRewards(player);
    }

    private int getCaptureSeconds() {

        return plugin.getConfig().getInt(
                "koth.capture-seconds",
                120
        );
    }

    private double getRadius() {

        return plugin.getConfig().getDouble(
                "koth.radius",
                10.0
        );
    }

    private double getRewardMoney() {

        return plugin.getConfig().getDouble(
                "koth.reward-money",
                500
        );
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

                    setLocation(player);
                    open(player);
                    break;

                case 12:

                    if (event.isLeftClick()) {

                        changeCaptureTime(
                                player,
                                10
                        );

                    } else if (event.isRightClick()) {

                        changeCaptureTime(
                                player,
                                -10
                        );
                    }

                    break;

                case 14:

                    if (event.isLeftClick()) {

                        changeRadius(
                                player,
                                1
                        );

                    } else if (event.isRightClick()) {

                        changeRadius(
                                player,
                                -1
                        );
                    }

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

            if (event.getSlot() == 45) {

                event.setCancelled(true);

                if (event.isLeftClick()) {

                    changeMoney(
                            player,
                            100
                    );

                } else if (event.isRightClick()) {

                    changeMoney(
                            player,
                            -100
                    );
                }

                return;
            }

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
    }
}