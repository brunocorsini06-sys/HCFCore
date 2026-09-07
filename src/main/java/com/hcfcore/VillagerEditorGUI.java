package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VillagerEditorGUI implements Listener {

    private final HCFCore plugin;

    private static final String MAIN_TITLE =
            ChatColor.DARK_GREEN + "📚 Librarian Editor";

    private static final String ENCHANT_TITLE =
            ChatColor.DARK_PURPLE + "📖 Encantamientos";

    public VillagerEditorGUI(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        if (!isAdmin(player)) {
            deny(player);
            return;
        }

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        MAIN_TITLE
                );

        List<String> trades =
                plugin.getConfig()
                        .getStringList(
                                "villager.librarian.trades"
                        );

        int slot = 0;

        for (String trade : trades) {

            if (slot >= 45) {
                break;
            }

            String[] parts =
                    trade.split(":");

            if (parts.length < 3) {
                continue;
            }

            String enchantmentName =
                    parts[0];

            int level =
                    parseInt(parts[1], 1);

            int price =
                    parseInt(parts[2], 1);

            Enchantment enchantment =
                    findEnchantment(
                            enchantmentName
                    );

            Material material =
                    enchantment == null
                            ? Material.BARRIER
                            : Material.ENCHANTED_BOOK;

            inv.setItem(
                    slot,
                    createItem(
                            material,
                            ChatColor.LIGHT_PURPLE
                                    + formatName(
                                    enchantmentName
                            ),
                            ChatColor.GRAY
                                    + "Nivel: "
                                    + level,
                            ChatColor.GRAY
                                    + "Precio: "
                                    + price
                                    + " esmeraldas",
                            "",
                            ChatColor.YELLOW
                                    + "Click izquierdo: editar",
                            ChatColor.RED
                                    + "Click derecho: eliminar"
                    )
            );

            slot++;
        }

        inv.setItem(
                45,
                createItem(
                        Material.EMERALD,
                        ChatColor.GREEN
                                + "💚 Activar / Desactivar",
                        ChatColor.GRAY
                                + "Estado actual: "
                                + (
                                plugin.getConfig()
                                        .getBoolean(
                                                "villager.enabled",
                                                true
                                        )
                                ? ChatColor.GREEN
                                        + "ACTIVADO"
                                : ChatColor.RED
                                        + "DESACTIVADO"
                        )
                )
        );

        inv.setItem(
                49,
                createItem(
                        Material.BOOK,
                        ChatColor.AQUA
                                + "➕ Agregar encantamiento",
                        ChatColor.GRAY
                                + "Abrir lista de encantamientos"
                )
        );

        inv.setItem(
                50,
                createItem(
                        Material.NETHER_STAR,
                        ChatColor.YELLOW
                                + "🔄 Aplicar",
                        ChatColor.GRAY
                                + "Aplicar trades a los aldeanos"
                )
        );

        inv.setItem(
                53,
                createItem(
                        Material.LIME_WOOL,
                        ChatColor.GREEN
                                + "💾 Guardar",
                        ChatColor.GRAY
                                + "Guardar configuración"
                )
        );

        player.openInventory(inv);
    }

    private void openEnchantments(Player player) {

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        ENCHANT_TITLE
                );

        Enchantment[] enchantments =
                Enchantment.values();

        int slot = 0;

        for (Enchantment enchantment :
                enchantments) {

            if (slot >= 45) {
                break;
            }

            String name =
                    enchantment.getKey()
                            .getKey();

            inv.setItem(
                    slot,
                    createItem(
                            Material.ENCHANTED_BOOK,
                            ChatColor.LIGHT_PURPLE
                                    + formatName(name),
                            ChatColor.GRAY
                                    + "Máximo nivel: "
                                    + enchantment
                                    .getMaxLevel(),
                            "",
                            ChatColor.GREEN
                                    + "Click para agregar"
                    )
            );

            slot++;
        }

        inv.setItem(
                49,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW
                                + "← Volver",
                        ChatColor.GRAY
                                + "Volver al editor"
                )
        );

        player.openInventory(inv);
    }

    private void addEnchantment(
            Player player,
            Enchantment enchantment
    ) {

        List<String> trades =
                new ArrayList<>(
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                )
                );

        String name =
                enchantment.getKey()
                        .getKey()
                        .toUpperCase();

        /*
         * Evitar duplicados.
         */
        for (String trade : trades) {

            if (trade.toUpperCase()
                    .startsWith(name + ":")) {

                player.sendMessage(
                        ChatColor.RED
                                + "Ese encantamiento ya está configurado."
                );

                open(player);

                return;
            }
        }

        /*
         * Nivel inicial = 1
         * Precio inicial = 20
         */
        trades.add(
                name + ":1:20"
        );

        plugin.getConfig().set(
                "villager.librarian.trades",
                trades
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Encantamiento agregado: "
                        + formatName(name)
        );

        open(player);
    }

    private void removeEnchantment(
            Player player,
            int index
    ) {

        List<String> trades =
                new ArrayList<>(
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                )
                );

        if (index < 0
                || index >= trades.size()) {
            return;
        }

        String removed =
                trades.remove(index);

        plugin.getConfig().set(
                "villager.librarian.trades",
                trades
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.YELLOW
                        + "✓ Encantamiento eliminado: "
                        + removed.split(":")[0]
        );

        open(player);
    }

    private void editEnchantment(
            Player player,
            int index
    ) {

        List<String> trades =
                new ArrayList<>(
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                )
                );

        if (index < 0
                || index >= trades.size()) {
            return;
        }

        String[] parts =
                trades.get(index)
                        .split(":");

        if (parts.length < 3) {
            return;
        }

        String name =
                parts[0];

        int level =
                parseInt(parts[1], 1);

        int price =
                parseInt(parts[2], 20);

        Enchantment enchantment =
                findEnchantment(name);

        if (enchantment == null) {
            player.sendMessage(
                    ChatColor.RED
                            + "Encantamiento inválido."
            );
            return;
        }

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        27,
                        ChatColor.DARK_PURPLE
                                + "Editar "
                                + formatName(name)
                );

        inv.setItem(
                11,
                createItem(
                        Material.EXPERIENCE_BOTTLE,
                        ChatColor.AQUA
                                + "Nivel: "
                                + level,
                        ChatColor.GRAY
                                + "Click izquierdo: +1",
                        ChatColor.GRAY
                                + "Click derecho: -1"
                )
        );

        inv.setItem(
                13,
                createItem(
                        Material.EMERALD,
                        ChatColor.GREEN
                                + "Precio: "
                                + price,
                        ChatColor.GRAY
                                + "Click izquierdo: +5",
                        ChatColor.GRAY
                                + "Click derecho: -5"
                )
        );

        inv.setItem(
                15,
                createItem(
                        Material.ENCHANTED_BOOK,
                        ChatColor.LIGHT_PURPLE
                                + formatName(name),
                        ChatColor.GRAY
                                + "Nivel máximo: "
                                + enchantment.getMaxLevel()
                )
        );

        inv.setItem(
                22,
                createItem(
                        Material.LIME_WOOL,
                        ChatColor.GREEN
                                + "💾 Guardar",
                        ChatColor.GRAY
                                + "Guardar cambios"
                )
        );

        /*
         * Guardamos temporalmente el índice
         * en el nombre del inventario mediante
         * un mapa simple por jugador.
         */
        EditSession.set(
                player,
                index
        );

        player.openInventory(inv);
    }

    private void changeLevel(
            Player player,
            int amount
    ) {

        Integer index =
                EditSession.get(player);

        if (index == null) {
            return;
        }

        List<String> trades =
                new ArrayList<>(
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                )
                );

        if (index >= trades.size()) {
            return;
        }

        String[] parts =
                trades.get(index)
                        .split(":");

        if (parts.length < 3) {
            return;
        }

        Enchantment enchantment =
                findEnchantment(parts[0]);

        if (enchantment == null) {
            return;
        }

        int level =
                parseInt(parts[1], 1);

        level =
                Math.max(
                        1,
                        Math.min(
                                enchantment.getMaxLevel(),
                                level + amount
                        )
                );

        parts[1] =
                String.valueOf(level);

        trades.set(
                index,
                parts[0]
                        + ":"
                        + parts[1]
                        + ":"
                        + parts[2]
        );

        plugin.getConfig().set(
                "villager.librarian.trades",
                trades
        );

        plugin.saveConfig();

        editEnchantment(
                player,
                index
        );
    }

    private void changePrice(
            Player player,
            int amount
    ) {

        Integer index =
                EditSession.get(player);

        if (index == null) {
            return;
        }

        List<String> trades =
                new ArrayList<>(
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                )
                );

        if (index >= trades.size()) {
            return;
        }

        String[] parts =
                trades.get(index)
                        .split(":");

        if (parts.length < 3) {
            return;
        }

        int price =
                parseInt(parts[2], 20);

        price =
                Math.max(
                        1,
                        price + amount
                );

        parts[2] =
                String.valueOf(price);

        trades.set(
                index,
                parts[0]
                        + ":"
                        + parts[1]
                        + ":"
                        + parts[2]
        );

        plugin.getConfig().set(
                "villager.librarian.trades",
                trades
        );

        plugin.saveConfig();

        editEnchantment(
                player,
                index
        );
    }

    private void applyTrades(
            Player player
    ) {

        plugin.getVillagerManager()
                .refresh();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Trades de los bibliotecarios actualizados."
        );

        open(player);
    }

    private void toggleEnabled(
            Player player
    ) {

        boolean current =
                plugin.getConfig()
                        .getBoolean(
                                "villager.enabled",
                                true
                        );

        plugin.getConfig().set(
                "villager.enabled",
                !current
        );

        plugin.saveConfig();

        player.sendMessage(
                ChatColor.GREEN
                        + "✓ Bibliotecarios personalizados: "
                        + (!current
                        ? "ACTIVADOS"
                        : "DESACTIVADOS")
        );

        open(player);
    }

    private Enchantment findEnchantment(
            String name
    ) {

        String normalized =
                name.toLowerCase()
                        .replace(
                                "minecraft:",
                                ""
                        )
                        .replace(
                                "_",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        );

        for (Enchantment enchantment :
                Enchantment.values()) {

            String key =
                    enchantment.getKey()
                            .getKey()
                            .toLowerCase()
                            .replace(
                                    "_",
                                    ""
                            )
                            .replace(
                                    "-",
                                    ""
                            );

            if (key.equals(normalized)) {
                return enchantment;
            }
        }

        return null;
    }

    private int parseInt(
            String value,
            int fallback
    ) {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String formatName(
            String name
    ) {

        String result =
                name.toLowerCase()
                        .replace(
                                "_",
                                " "
                        );

        String[] words =
                result.split(" ");

        StringBuilder builder =
                new StringBuilder();

        for (String word : words) {

            if (word.isEmpty()) {
                continue;
            }

            builder.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            if (word.length() > 1) {
                builder.append(
                        word.substring(1)
                );
            }

            builder.append(" ");
        }

        return builder.toString().trim();
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

            List<String> loreList =
                    new ArrayList<>();

            for (String line : lore) {
                loreList.add(line);
            }

            meta.setLore(loreList);

            item.setItemMeta(meta);
        }

        return item;
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

            int slot =
                    event.getRawSlot();

            if (slot >= 0
                    && slot < 45) {

                List<String> trades =
                        plugin.getConfig()
                                .getStringList(
                                        "villager.librarian.trades"
                                );

                if (slot < trades.size()) {

                    if (event.isLeftClick()) {

                        editEnchantment(
                                player,
                                slot
                        );

                    } else if (event.isRightClick()) {

                        removeEnchantment(
                                player,
                                slot
                        );
                    }
                }

                return;
            }

            if (slot == 45) {

                toggleEnabled(player);
                return;
            }

            if (slot == 49) {

                openEnchantments(player);
                return;
            }

            if (slot == 50) {

                applyTrades(player);
                return;
            }

            if (slot == 53) {

                plugin.saveConfig();

                player.sendMessage(
                        ChatColor.GREEN
                                + "✓ Configuración guardada."
                );

                return;
            }

            return;
        }

        if (title.equals(ENCHANT_TITLE)) {

            event.setCancelled(true);

            int slot =
                    event.getRawSlot();

            if (slot == 49) {

                open(player);
                return;
            }

            if (slot < 0 || slot >= 45) {
                return;
            }

            Enchantment[] enchantments =
                    Enchantment.values();

            if (slot >= enchantments.length) {
                return;
            }

            addEnchantment(
                    player,
                    enchantments[slot]
            );

            return;
        }

        if (title.startsWith(
                ChatColor.DARK_PURPLE
                        + "Editar "
        )) {

            event.setCancelled(true);

            if (event.getRawSlot() == 11) {

                if (event.isLeftClick()) {
                    changeLevel(player, 1);
                } else if (event.isRightClick()) {
                    changeLevel(player, -1);
                }

                return;
            }

            if (event.getRawSlot() == 13) {

                if (event.isLeftClick()) {
                    changePrice(player, 5);
                } else if (event.isRightClick()) {
                    changePrice(player, -5);
                }

                return;
            }

            if (event.getRawSlot() == 22) {

                player.sendMessage(
                        ChatColor.GREEN
                                + "✓ Cambios guardados."
                );

                open(player);
            }
        }
    }

    private static class EditSession {

        private static final java.util.Map<
                java.util.UUID,
                Integer
                > SESSIONS =
                new java.util.HashMap<>();

        private static void set(
                Player player,
                int index
        ) {

            SESSIONS.put(
                    player.getUniqueId(),
                    index
            );
        }

        private static Integer get(
                Player player
        ) {

            return SESSIONS.get(
                    player.getUniqueId()
            );
        }
    }
}