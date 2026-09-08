package com.hcfcore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class VaultHook {

    private final HCFCore plugin;
    private Economy economy;

    public VaultHook(HCFCore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {

        RegisteredServiceProvider<Economy> provider =
                plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null) {

            plugin.getLogger().severe(
                    "No se encontró un proveedor de economía para Vault."
            );

            throw new IllegalStateException(
                    "Vault no tiene un proveedor de economía disponible."
            );
        }

        economy = provider.getProvider();

        if (economy == null) {

            plugin.getLogger().severe(
                    "Vault encontró un proveedor, pero la economía es NULL."
            );

            throw new IllegalStateException(
                    "Proveedor de economía de Vault inválido."
            );
        }

        plugin.getLogger().info(
                "Vault conectado correctamente. Economía: "
                        + economy.getName()
        );
    }

    public Economy getEconomy() {
        return economy;
    }

    public String getName() {

        if (economy == null) {
            return "Unknown";
        }

        return economy.getName();
    }

    public double getBalance(Player player) {

        if (player == null || economy == null) {
            return 0.0;
        }

        return economy.getBalance(player);
    }

    public double getBalance(UUID uuid) {

        if (uuid == null || economy == null) {
            return 0.0;
        }

        Player player =
                plugin.getServer()
                        .getPlayer(uuid);

        if (player == null) {
            return 0.0;
        }

        return getBalance(player);
    }

    public boolean has(Player player, double amount) {

        if (player == null || economy == null || amount < 0.0) {
            return false;
        }

        return economy.has(player, amount);
    }

    public boolean deposit(Player player, double amount) {

        if (player == null || economy == null || amount <= 0.0) {
            return false;
        }

        return economy.depositPlayer(player, amount)
                .transactionSuccess();
    }

    public boolean withdraw(Player player, double amount) {

        if (player == null || economy == null || amount <= 0.0) {
            return false;
        }

        return economy.withdrawPlayer(player, amount)
                .transactionSuccess();
    }

    public boolean transfer(
            Player sender,
            Player receiver,
            double amount
    ) {

        if (sender == null
                || receiver == null
                || economy == null
                || amount <= 0.0) {

            return false;
        }

        if (!economy.has(sender, amount)) {
            return false;
        }

        if (!economy.withdrawPlayer(sender, amount)
                .transactionSuccess()) {

            return false;
        }

        if (!economy.depositPlayer(receiver, amount)
                .transactionSuccess()) {

            // Intentar devolver el dinero al jugador
            // si el depósito del receptor falla.
            economy.depositPlayer(sender, amount);

            return false;
        }

        return true;
    }

    public boolean isConnected() {
        return economy != null;
    }
}