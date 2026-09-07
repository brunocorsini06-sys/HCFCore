package com.hcfcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitManager {

    private final HCFCore plugin;

    private final Map<String, Map<UUID, Long>> cooldowns =
            new HashMap<>();

    public KitManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Comprueba si el kit existe.
     */
    public boolean kitExists(String kitName) {

        if (kitName == null) {
            return false;
        }

        return plugin.getConfig()
                .contains("kits." + kitName.toLowerCase());
    }

    /**
     * Comprueba si el jugador tiene el kit disponible.
     */
    public boolean canUseKit(
            Player player,
            String kitName
    ) {

        if (player == null || kitName == null) {
            return false;
        }

        if (!kitExists(kitName)) {
            return false;
        }

        Map<UUID, Long> kitCooldown =
                cooldowns.computeIfAbsent(
                        kitName.toLowerCase(),
                        key -> new HashMap<>()
                );

        Long expires =
                kitCooldown.get(
                        player.getUniqueId()
                );

        if (expires == null) {
            return true;
        }

        if (System.currentTimeMillis() >= expires) {
            kitCooldown.remove(
                    player.getUniqueId()
            );

            return true;
        }

        return false;
    }

    /**
     * Devuelve los segundos restantes.
     */
    public long getRemainingSeconds(
            Player player,
            String kitName
    ) {

        if (player == null || kitName == null) {
            return 0;
        }

        Map<UUID, Long> kitCooldown =
                cooldowns.get(
                        kitName.toLowerCase()
                );

        if (kitCooldown == null) {
            return 0;
        }

        Long expires =
                kitCooldown.get(
                        player.getUniqueId()
                );

        if (expires == null) {
            return 0;
        }

        long remaining =
                expires - System.currentTimeMillis();

        if (remaining <= 0) {
            kitCooldown.remove(
                    player.getUniqueId()
            );

            return 0;
        }

        return (remaining + 999) / 1000;
    }

    /**
     * Entrega el kit al jugador.
     */
    public boolean giveKit(
            Player player,
            String kitName
    ) {

        if (player == null || kitName == null) {
            return false;
        }

        kitName = kitName.toLowerCase();

        if (!kitExists(kitName)) {
            return false;
        }

        if (!canUseKit(player, kitName)) {
            return false;
        }

        ConfigurationSection section =
                plugin.getConfig()
                        .getConfigurationSection(
                                "kits." + kitName
                        );

        if (section == null) {
            return false;
        }

        if (!section.contains("items")) {
            return false;
        }

        for (String itemString :
                section.getStringList("items")) {

            ItemStack item =
                    parseItem(itemString);

            if (item != null) {

                player.getInventory()
                        .addItem(item);
            }
        }

        long cooldown =
                section.getLong(
                        "cooldown-seconds",
                        86400
                );

        cooldowns
                .computeIfAbsent(
                        kitName,
                        key -> new HashMap<>()
                )
                .put(
                        player.getUniqueId(),
                        System.currentTimeMillis()
                                + cooldown * 1000L
                );

        return true;
    }

    /**
     * Convierte:
     *
     * DIAMOND_SWORD:1
     *
     * en ItemStack.
     */
    private ItemStack parseItem(
            String value
    ) {

        if (value == null || value.isEmpty()) {
            return null;
        }

        String[] parts =
                value.split(":");

        String materialName =
                parts[0].toUpperCase();

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material == null) {
            return null;
        }

        int amount = 1;

        if (parts.length >= 2) {

            try {
                amount =
                        Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        return new ItemStack(
                material,
                Math.max(1, amount)
        );
    }

    /**
     * Devuelve todos los cooldowns.
     */
    public Map<String, Map<UUID, Long>> getCooldowns() {
        return cooldowns;
    }
}