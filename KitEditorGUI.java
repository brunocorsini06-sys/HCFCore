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

    private static final String PREFIX =
            ChatColor.DARK_GREEN + "Editando Kit: ";

    private static final String COOLDOWN_TITLE =
            ChatColor.DARK_AQUA + "⏱ Kit Cooldown";

    public KitEditorGUI(HCFCore plugin) {
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

        int slot = 10;

        if (plugin.getConfig().getConfigurationSection("kits") != null) {

            for (String kit :
                    plugin.getConfig()
                            .getConfigurationSection("kits")
                            .getKeys(false)) {

                if (slot >= 17) {
                    break;
                }

                Material icon = getKitIcon(kit);

                inv.setItem(
                        slot,
                        item(
                                icon,
                                ChatColor.GREEN + kit,
                                ChatColor.GRAY +
                                        "Click para editar"
                        )
                );

                slot++;
            }
        }

        inv.setItem(
                22,
                item(
                        Material.EMERALD,
                        ChatColor.GREEN + "➕ Crear Kit",
                        ChatColor.GRAY +
                                "Crear un nuevo kit"
                )
        );

        inv.setItem(
                26,
                item(
                        Material.BARRIER,
                        ChatColor.RED + "✖ Cerrar",
                        ChatColor.GRAY +
                                "Cerrar editor"
                )
        );

        player.openInventory(inv);
    }

    public void openEditor(
            Player player,
            String kitName
    ) {

        if (!isAdmin(player)) {
            deny(player);
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

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        PREFIX + kitName
                );

        loadKit(inv, kitName);

        inv.setItem(
                45,
                item(
                        Material.CLOCK,
                        ChatColor.YELLOW + "⏱ Cooldown",
                        ChatColor.GRAY +
                                "Actual: "
                                + formatTime(
                                        plugin.getConfig()
                                                .getLong(
                                                        path +
                                                                ".cooldown-seconds",
                                                        86400
                                                )
                                ),
                        ChatColor.GRAY +
                                "Click para modificar"
                )
        );

        inv.setItem(
                49,
                item(
                        Material.LIME_WOOL,
                        ChatColor.GREEN + "💾 GUARDAR",
                        ChatColor.GRAY +
                                "Guardar objetos del kit"
                )
        );

        inv.setItem(
                53,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Volver",
                        ChatColor.GRAY +
                                "Volver a los kits"
                )
        );

        player.openInventory(inv);
    }

    private void loadKit(
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
                "kits."
                        + kitName.toLowerCase();

        Inventory inv =
                player.getOpenInventory()
                        .getTopInventory();

        List<String> items =
                new ArrayList<>();

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
                path + ".items",
                items
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Kit "
                        + kitName
                        + " guardado."
        );
    }

    private void openCooldown(
            Player player,
            String kitName
    ) {

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        27,
                        COOLDOWN_TITLE
                                + " | "
                                + kitName
                );

        inv.setItem(
                10,
                item(
                        Material.RED_CONCRETE,
                        ChatColor.RED + "-1 hora",
                        ChatColor.GRAY +
                                "Reducir 1 hora"
                )
        );

        inv.setItem(
                12,
                item(
                        Material.RED_WOOL,
                        ChatColor.RED + "-10 minutos",
                        ChatColor.GRAY +
                                "Reducir 10 minutos"
                )
        );

        inv.setItem(
                14,
                item(
                        Material.GREEN_WOOL,
                        ChatColor.GREEN + "+10 minutos",
                        ChatColor.GRAY +
                                "Agregar 10 minutos"
                )
        );

        inv.setItem(
                16,
                item(
                        Material.GREEN_CONCRETE,
                        ChatColor.GREEN + "+1 hora",
                        ChatColor.GRAY +
                                "Agregar 1 hora"
                )
        );

        inv.setItem(
                22,
                item(
                        Material.ARROW,
                        ChatColor.YELLOW + "← Volver",
                        ChatColor.GRAY +
                                "Volver al kit"
                )
        );

        player.openInventory(inv);
    }

    private void changeCooldown(
            Player player,
            String kitName,
            long seconds
    ) {

        String path =
                "kits."
                        + kitName.toLowerCase()
                        + ".cooldown-seconds";

        long current =
                plugin.getConfig()
                        .getLong(
                                path,
                                86400
                        );

        long newValue =
                Math.max(
                        0,
                        current + seconds
                );

        plugin.getConfig().set(
                path,
                newValue
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Cooldown de "
                        + kitName
                        + ": "
                        + formatTime(newValue)
        );

        openEditor(
                player,
                kitName
        );
    }

    private String getKitFromTitle(
            String title
    ) {

        String prefix =
                COOLDOWN_TITLE + " | ";

        if (!title.startsWith(prefix)) {
            return null;
        }

        return title.substring(
                prefix.length()
        );
    }

    private Material getKitIcon(
            String kit
    ) {

        if (kit.equalsIgnoreCase("starter")) {
            return Material.CHEST;
        }

        if (kit.equalsIgnoreCase("archer")) {
            return Material.BOW;
        }

        if (kit.equalsIgnoreCase("bard")) {
            return Material.GOLD_INGOT;
        }

        if (kit.equalsIgnoreCase("diamond")) {
            return Material.DIAMOND_CHESTPLATE;
        }

        return Material.CHEST;
    }

    private String formatTime(
            long seconds
    ) {

        if (seconds <= 0) {
            return "Sin cooldown";
        }

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        if (days > 0) {
            return days + "d "
                    + hours + "h";
        }

        if (hours > 0) {
            return hours + "h "
                    + minutes + "m";
        }

        return minutes + "m";
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

    private boolean isAdmin(
            Player player
    ) {

        return player.hasPermission(
                "hcf.admin"
        );
    }

    private void deny(
            Player player
    ) {

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
                event.getView()
                        .getTitle();

        if (title.equals(MAIN_TITLE)) {

            event.setCancelled(true);

            if (!isAdmin(player)) {
                player.closeInventory();
                return;
            }

            if (event.getSlot() == 22) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "Para crear un kit nuevo:"
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Usaremos un sistema de creación "
                                + "por nombre en el siguiente paso."
                );

                return;
            }

            if (event.getSlot() == 26) {
                player.closeInventory();
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

            if (plugin.getConfig()
                    .contains(
                            "kits."
                                    + kit.toLowerCase()
                    )) {

                openEditor(
                        player,
                        kit
                );
            }

            return;
        }

        if (title.startsWith(PREFIX)) {

            if (event.getSlot() == 45) {

                event.setCancelled(true);

                String kitName =
                        ChatColor.stripColor(
                                title.substring(
                                        PREFIX.length()
                                )
                        );

                openCooldown(
                        player,
                        kitName
                );

                return;
            }

            if (event.getSlot() == 49) {

                event.setCancelled(true);

                saveKit(player);

                return;
            }

            if (event.getSlot() == 53) {

                event.setCancelled(true);

                open(player);

                return;
            }

            /*
             * Slots 0-44 son editables.
             * El jugador puede colocar y quitar
             * objetos libremente.
             */

            return;
        }

        if (title.startsWith(
                COOLDOWN_TITLE + " | "
        )) {

            event.setCancelled(true);

            String kitName =
                    getKitFromTitle(title);

            if (kitName == null) {
                return;
            }

            switch (event.getSlot()) {

                case 10:

                    changeCooldown(
                            player,
                            kitName,
                            -3600
                    );

                    break;

                case 12:

                    changeCooldown(
                            player,
                            kitName,
                            -600
                    );

                    break;

                case 14:

                    changeCooldown(
                            player,
                            kitName,
                            600
                    );

                    break;

                case 16:

                    changeCooldown(
                            player,
                            kitName,
                            3600
                    );

                    break;

                case 22:

                    openEditor(
                            player,
                            kitName
                    );

                    break;

                default:
                    break;
            }
        }
    }
}