package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final HCFCore plugin;

    public EconomyManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Obtiene el proveedor económico de Vault.
     */
    private Economy getEconomy() {

        VaultHook vaultHook = plugin.getVaultHook();

        if (vaultHook == null || !vaultHook.isConnected()) {
            return null;
        }

        return vaultHook.getEconomy();
    }

    /**
     * Configura la cuenta del jugador.
     *
     * Si no existe, la crea y aplica el balance inicial
     * configurado en HCFCore.
     */
    public void setupPlayer(Player player) {

        if (player == null) {
            return;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        OfflinePlayer offlinePlayer = player;

        if (!economy.hasAccount(offlinePlayer)) {

            boolean created =
                    economy.createPlayerAccount(offlinePlayer);

            if (!created) {
                plugin.getLogger().warning(
                        "No se pudo crear la cuenta económica de "
                                + player.getName()
                );

                return;
            }

            double startingBalance =
                    plugin.getConfig()
                            .getDouble(
                                    "economy.starting-balance",
                                    0.0
                            );

            if (isValidAmount(startingBalance)
                    && startingBalance > 0.0) {

                economy.depositPlayer(
                        offlinePlayer,
                        startingBalance
                );
            }
        }
    }

    /**
     * Obtiene el balance de un jugador.
     */
    public double getBalance(Player player) {

        if (player == null) {
            return 0.0;
        }

        setupPlayer(player);

        Economy economy = getEconomy();

        if (economy == null) {
            return 0.0;
        }

        return Math.max(
                0.0,
                economy.getBalance(player)
        );
    }

    /**
     * Obtiene el balance mediante UUID.
     */
    public double getBalance(UUID uuid) {

        if (uuid == null) {
            return 0.0;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return 0.0;
        }

        OfflinePlayer player =
                Bukkit.getOfflinePlayer(uuid);

        if (!economy.hasAccount(player)) {
            return 0.0;
        }

        return Math.max(
                0.0,
                economy.getBalance(player)
        );
    }

    /**
     * Establece el balance de un jugador.
     */
    public void setBalance(
            Player player,
            double amount
    ) {

        if (player == null) {
            return;
        }

        if (!isValidAmount(amount)) {
            return;
        }

        setupPlayer(player);

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        double current =
                economy.getBalance(player);

        double target =
                Math.max(0.0, amount);

        if (current < target) {

            economy.depositPlayer(
                    player,
                    target - current
            );

        } else if (current > target) {

            economy.withdrawPlayer(
                    player,
                    current - target
            );
        }
    }

    /**
     * Establece el balance mediante UUID.
     */
    public void setBalance(
            UUID uuid,
            double amount
    ) {

        if (uuid == null) {
            return;
        }

        if (!isValidAmount(amount)) {
            return;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        OfflinePlayer player =
                Bukkit.getOfflinePlayer(uuid);

        if (!economy.hasAccount(player)) {

            if (!economy.createPlayerAccount(player)) {
                return;
            }
        }

        double current =
                economy.getBalance(player);

        double target =
                Math.max(0.0, amount);

        if (current < target) {

            economy.depositPlayer(
                    player,
                    target - current
            );

        } else if (current > target) {

            economy.withdrawPlayer(
                    player,
                    current - target
            );
        }
    }

    /**
     * Deposita dinero.
     */
    public void deposit(
            Player player,
            double amount
    ) {

        if (player == null) {
            return;
        }

        if (!isValidPositiveAmount(amount)) {
            return;
        }

        setupPlayer(player);

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        economy.depositPlayer(
                player,
                amount
        );
    }

    /**
     * Deposita dinero mediante UUID.
     */
    public void deposit(
            UUID uuid,
            double amount
    ) {

        if (uuid == null) {
            return;
        }

        if (!isValidPositiveAmount(amount)) {
            return;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        OfflinePlayer player =
                Bukkit.getOfflinePlayer(uuid);

        if (!economy.hasAccount(player)) {

            if (!economy.createPlayerAccount(player)) {
                return;
            }
        }

        economy.depositPlayer(
                player,
                amount
        );
    }

    /**
     * Retira dinero.
     *
     * @return true si la operación fue realizada.
     */
    public boolean withdraw(
            Player player,
            double amount
    ) {

        if (player == null) {
            return false;
        }

        if (!isValidPositiveAmount(amount)) {
            return false;
        }

        setupPlayer(player);

        Economy economy = getEconomy();

        if (economy == null) {
            return false;
        }

        if (!economy.has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(
                player,
                amount
        ).transactionSuccess();
    }

    /**
     * Retira dinero mediante UUID.
     */
    public boolean withdraw(
            UUID uuid,
            double amount
    ) {

        if (uuid == null) {
            return false;
        }

        if (!isValidPositiveAmount(amount)) {
            return false;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return false;
        }

        OfflinePlayer player =
                Bukkit.getOfflinePlayer(uuid);

        if (!economy.hasAccount(player)) {
            return false;
        }

        if (!economy.has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(
                player,
                amount
        ).transactionSuccess();
    }

    /**
     * Transfiere dinero entre jugadores.
     */
    public boolean transfer(
            Player sender,
            Player receiver,
            double amount
    ) {

        if (sender == null || receiver == null) {
            return false;
        }

        if (sender.getUniqueId()
                .equals(receiver.getUniqueId())) {

            return false;
        }

        if (!isValidPositiveAmount(amount)) {
            return false;
        }

        setupPlayer(sender);
        setupPlayer(receiver);

        Economy economy = getEconomy();

        if (economy == null) {
            return false;
        }

        if (!economy.has(sender, amount)) {
            return false;
        }

        if (!economy.withdrawPlayer(
                sender,
                amount
        ).transactionSuccess()) {

            return false;
        }

        if (!economy.depositPlayer(
                receiver,
                amount
        ).transactionSuccess()) {

            /*
             * Intentamos devolver el dinero
             * al jugador si el depósito falla.
             */
            economy.depositPlayer(
                    sender,
                    amount
            );

            return false;
        }

        return true;
    }

    /**
     * Devuelve una vista de los balances
     * de los jugadores actualmente conectados.
     *
     * La economía real pertenece a Vault.
     */
    public Map<UUID, Double> getBalances() {

        Map<UUID, Double> balances =
                new HashMap<>();

        Economy economy = getEconomy();

        if (economy == null) {
            return balances;
        }

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            balances.put(
                    player.getUniqueId(),
                    Math.max(
                            0.0,
                            economy.getBalance(player)
                    )
            );
        }

        return balances;
    }

    /**
     * Guarda la economía.
     *
     * Vault y el plugin económico son responsables
     * de la persistencia del balance.
     */
    public void saveAll() {

        /*
         * No hacemos nada aquí.
         *
         * HCFCore ya no guarda balances de jugadores
         * en SQLite porque el proveedor de Vault es
         * responsable de almacenar la economía.
         */
    }

    /**
     * Comprueba que el número sea válido.
     */
    private boolean isValidAmount(
            double amount
    ) {

        return !Double.isNaN(amount)
                && !Double.isInfinite(amount);
    }

    /**
     * Comprueba que el número sea positivo.
     */
    private boolean isValidPositiveAmount(
            double amount
    ) {

        return isValidAmount(amount)
                && amount > 0.0;
    }
}