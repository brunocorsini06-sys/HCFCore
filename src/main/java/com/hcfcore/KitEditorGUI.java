package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class KitEditorGUI implements Listener {

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_GREEN + "🎒 Kit Editor";

    private static final String PREFIX =
            ChatColor.DARK_GREEN + "Editando Kit: ";

    private static final String COOLDOWN_TITLE =
            ChatColor.DARK_AQUA + "⏱ Kit Cooldown";

    private final Set<UUID> waitingForKitName =
            new HashSet<>();

    public KitEditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

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

        if (plugin.getConfig()
                .getConfigurationSection("kits") != null) {

            for (String kit : plugin.getConfig()
                    .getConfigurationSection("kits")
                    .getKeys(false)) {

                if (slot >= 17) {
                    break;
                }

                inv.setItem(
                        slot,
                        item(
                                getKitIcon(kit),
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
                                "Crear un kit nuevo"
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

    // =========================================================
    // KIT EDITOR
    // =========================================================

    public void openEditor(
            Player player,
            String kitName
    ) {

        if (!isAdmin(player)) {
            deny(player);
            return;
        }

        if (kitName == null) {
            return;
        }

        kitName = kitName.toLowerCase(Locale.ROOT);

        String path = "kits." + kitName;

        if (!plugin.getConfig().contains(path)) {

            player.sendMessage(
                    ChatColor.RED +
                            "Ese kit no existe."
            );

            return;
        }

        Inventory inv = Bukkit.createInventory(
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

    // =========================================================
    // LOAD KIT
    // =========================================================

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

            if (entry == null
                    || entry.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    entry.split(":");

            Material material =
                    Material.matchMaterial(
                            parts[0]
                                    .trim()
                                    .toUpperCase()
                    );

            if (material == null
                    || material == Material.AIR) {
                continue;
            }

            int amount = 1;

            if (parts.length >= 2) {

                try {
                    amount = Integer.parseInt(
                            parts[1].trim()
                    );
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }

            amount = Math.max(1, amount);

            amount = Math.min(
                    amount,
                    material.getMaxStackSize()
            );

            inv.setItem(
                    slot++,
                    new ItemStack(
                            material,
                            amount
                    )
            );
        }
    }

    // =========================================================
    // SAVE KIT
    // =========================================================

    private void saveKit(Player player) {

        String title =
                player.getOpenInventory().getTitle();

        if (!title.startsWith(PREFIX)) {
            return;
        }

        String kitName =
                ChatColor.stripColor(
                        title.substring(
                                PREFIX.length()
                        )
                ).toLowerCase(Locale.ROOT);

        String path = "kits." + kitName;

        if (!plugin.getConfig().contains(path)) {
            return;
        }

        Inventory inv =
                player.getOpenInventory()
                        .getTopInventory();

        List<String> items =
                new ArrayList<>();

        for (int slot = 0; slot < 45; slot++) {

            ItemStack stack =
                    inv.getItem(slot);

            if (stack == null
                    || stack.getType()
                    == Material.AIR) {
                continue;
            }

            items.add(
                    stack.getType().name()
                            + ":"
                            + stack.getAmount()
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
                        + " guardado correctamente."
        );
    }

    // =========================================================
    // CREATE KIT
    // =========================================================

    private void createKit(
            Player player,
            String name
    ) {

        if (name == null) {
            return;
        }

        name = name.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "");

        if (!name.matches("[a-z0-9_-]+")) {

            player.sendMessage(
                    ChatColor.RED +
                            "Nombre inválido."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "Usá solamente letras, números, "
                            + "_ o -."
            );

            return;
        }

        if (name.length() < 2 || name.length() > 16) {

            player.sendMessage(
                    ChatColor.RED +
                            "El nombre debe tener entre "
                            + "2 y 16 caracteres."
            );

            return;
        }

        if (plugin.getConfig()
                .contains("kits." + name)) {

            player.sendMessage(
                    ChatColor.RED +
                            "Ese kit ya existe."
            );

            return;
        }

        plugin.getConfig().set(
                "kits."
                        + name
                        + ".cooldown-seconds",
                86400
        );

        plugin.getConfig().set(
                "kits."
                        + name
                        + ".items",
                new ArrayList<String>()
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Kit "
                        + name
                        + " creado."
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Cooldown inicial: 24 horas."
        );

        openEditor(
                player,
                name
        );
    }

    // =========================================================
    // COOLDOWN MENU
    // =========================================================

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

    // =========================================================
    // CHANGE COOLDOWN
    // =========================================================

    private void changeCooldown(
            Player player,
            String kitName,
            long seconds
    ) {

        String path =
                "kits."
                        + kitName.toLowerCase(Locale.ROOT)
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
                        "✓ Cooldown: "
                        + formatTime(newValue)
        );

        openEditor(
                player,
                kitName
        );
    }

    // =========================================================
    // TITLE
    // =========================================================

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

    // =========================================================
    // ICONS
    // =========================================================

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

    // =========================================================
    // FORMAT TIME
    // =========================================================

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

    // =========================================================
    // ITEM
    // =========================================================

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

    // =========================================================
    // PERMISSION
    // =========================================================

    private boolean isAdmin(
            Player player
    ) {

        return player != null
                && player.hasPermission(
                "hcf.admin"
        );
    }

    private void deny(
            Player player
    ) {

        if (player == null) {
            return;
        }

        player.sendMessage(
                ChatColor.RED +
                        "No tienes permiso para usar el editor."
        );
    }

    // =========================================================
    // INVENTORY CLICK
    // =========================================================

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

        // =====================================================
        // MAIN MENU
        // =====================================================

        if (title.equals(MAIN_TITLE)) {

            event.setCancelled(true);

            if (!isAdmin(player)) {
                player.closeInventory();
                return;
            }

            /*
             * Solo aceptamos clicks dentro del inventario
             * superior.
             */
            if (event.getRawSlot() < 0
                    || event.getRawSlot()
                    >= event.getView()
                    .getTopInventory()
                    .getSize()) {
                return;
            }

            if (event.getRawSlot() == 22) {

                player.closeInventory();

                waitingForKitName.add(
                        player.getUniqueId()
                );

                player.sendMessage(
                        ChatColor.GREEN +
                                "➕ Escribí en el chat el nombre "
                                + "del nuevo kit."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Ejemplo: "
                                + ChatColor.WHITE
                                + "Miner"
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Escribí "
                                + ChatColor.RED
                                + "cancelar"
                                + ChatColor.GRAY
                                + " para cancelar."
                );

                return;
            }

            if (event.getRawSlot() == 26) {

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
                                    + kit.toLowerCase(
                                    Locale.ROOT
                            )
                    )) {

                openEditor(
                        player,
                        kit
                );
            }

            return;
        }

        // =====================================================
        // KIT EDITOR
        // =====================================================

        if (title.startsWith(PREFIX)) {

            /*
             * Slots 45-53 son botones.
             */
            if (event.getRawSlot() >= 45) {

                event.setCancelled(true);

                if (event.getRawSlot() == 45) {

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

                if (event.getRawSlot() == 49) {

                    saveKit(player);
                    return;
                }

                if (event.getRawSlot() == 53) {

                    open(player);
                    return;
                }

                return;
            }

            /*
             * Slots 0-44 son completamente editables.
             */
            return;
        }

        // =====================================================
        // COOLDOWN MENU
        // =====================================================

        if (title.startsWith(
                COOLDOWN_TITLE + " | "
        )) {

            event.setCancelled(true);

            String kitName =
                    getKitFromTitle(title);

            if (kitName == null) {
                return;
            }

            switch (event.getRawSlot()) {

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

    // =========================================================
    // CHAT CREATE KIT
    // =========================================================

    @EventHandler
    public void onChat(
            AsyncPlayerChatEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        if (!waitingForKitName.contains(uuid)) {
            return;
        }

        event.setCancelled(true);

        String message =
                event.getMessage().trim();

        waitingForKitName.remove(uuid);

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    if (message.equalsIgnoreCase(
                            "cancelar"
                    )) {

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        "Creación de kit cancelada."
                        );

                        open(player);
                        return;
                    }

                    createKit(
                            player,
                            message
                    );
                }
        );
    }
}