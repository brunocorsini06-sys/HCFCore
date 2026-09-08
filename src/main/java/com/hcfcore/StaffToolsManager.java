package com.hcfcore;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffManager {

    private final HCFCore plugin;

    private final Map<UUID, GameMode> previousGameModes =
            new HashMap<>();

    private final Map<UUID, ItemStack[]> previousInventories =
            new HashMap<>();

    private final Map<UUID, ItemStack[]> previousArmor =
            new HashMap<>();

    private final Map<UUID, ItemStack> previousOffhand =
            new HashMap<>();

    public StaffManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    public boolean isStaff(Player player) {

        if (player == null) {
            return false;
        }

        return previousGameModes.containsKey(
                player.getUniqueId()
        );
    }

    public boolean enable(Player player) {

        if (player == null || isStaff(player)) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        /*
         * Guardar GameMode.
         */

        previousGameModes.put(
                uuid,
                player.getGameMode()
        );

        /*
         * Guardar inventario.
         */

        previousInventories.put(
                uuid,
                player.getInventory()
                        .getContents()
                        .clone()
        );

        /*
         * Guardar armadura.
         */

        previousArmor.put(
                uuid,
                player.getInventory()
                        .getArmorContents()
                        .clone()
        );

        /*
         * Guardar offhand.
         */

        ItemStack offhand =
                player.getInventory()
                        .getItemInOffHand();

        previousOffhand.put(
                uuid,
                offhand == null
                        ? null
                        : offhand.clone()
        );

        /*
         * Limpiar inventario temporal.
         */

        player.getInventory().clear();

        player.getInventory().setArmorContents(
                new ItemStack[4]
        );

        player.getInventory().setItemInOffHand(
                null
        );

        /*
         * Activar Staff Mode.
         */

        player.setGameMode(
                GameMode.CREATIVE
        );

        player.setAllowFlight(true);
        player.setFlying(true);

        /*
         * Dar Staff Tools.
         */

        StaffToolsManager tools =
                plugin.getStaffToolsManager();

        if (tools != null) {

            tools.giveTools(player);
        }

        return true;
    }

    public boolean disable(Player player) {

        if (player == null || !isStaff(player)) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        /*
         * Limpiar Staff Tools.
         */

        player.getInventory().clear();

        player.getInventory().setArmorContents(
                new ItemStack[4]
        );

        player.getInventory().setItemInOffHand(
                null
        );

        /*
         * Restaurar inventario.
         */

        ItemStack[] inventory =
                previousInventories.remove(uuid);

        if (inventory != null) {

            player.getInventory().setContents(
                    inventory
            );
        }

        /*
         * Restaurar armadura.
         */

        ItemStack[] armor =
                previousArmor.remove(uuid);

        if (armor != null) {

            player.getInventory().setArmorContents(
                    armor
            );
        }

        /*
         * Restaurar offhand.
         */

        ItemStack offhand =
                previousOffhand.remove(uuid);

        player.getInventory().setItemInOffHand(
                offhand
        );

        /*
         * Restaurar GameMode.
         */

        GameMode previousGameMode =
                previousGameModes.remove(uuid);

        if (previousGameMode != null) {

            player.setGameMode(
                    previousGameMode
            );
        }

        player.setFlying(false);
        player.setAllowFlight(false);

        return true;
    }

    public void handleQuit(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        previousGameModes.remove(uuid);
        previousInventories.remove(uuid);
        previousArmor.remove(uuid);
        previousOffhand.remove(uuid);
    }

    public void disableAll() {

        for (Player player :
                plugin.getServer()
                        .getOnlinePlayers()) {

            if (isStaff(player)) {

                disable(player);
            }
        }

        previousGameModes.clear();
        previousInventories.clear();
        previousArmor.clear();
        previousOffhand.clear();
    }

    public int getStaffCount() {

        return previousGameModes.size();
    }
}