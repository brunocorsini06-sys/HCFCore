package com.hcfcore;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final HCFCore plugin;

    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void setupPlayer(Player player) {

        if (player == null) {
            return;
        }

        balances.putIfAbsent(
                player.getUniqueId(),
                0.0
        );
    }

    public double getBalance(Player player) {

        setupPlayer(player);

        return balances.getOrDefault(
                player.getUniqueId(),
                0.0
        );
    }

    public void setBalance(
            Player player,
            double amount
    ) {

        if (player == null) {
            return;
        }

        balances.put(
                player.getUniqueId(),
                Math.max(0.0, amount)
        );
    }

    public boolean deposit(
            Player player,
            double amount
    ) {

        if (player == null || amount <= 0) {
            return false;
        }

        setBalance(
                player,
                getBalance(player) + amount
        );

        return true;
    }

    public boolean withdraw(
            Player player,
            double amount
    ) {

        if (player == null || amount <= 0) {
            return false;
        }

        double balance =
                getBalance(player);

        if (balance < amount) {
            return false;
        }

        setBalance(
                player,
                balance - amount
        );

        return true;
    }

    public boolean transfer(
            Player sender,
            Player receiver,
            double amount
    ) {

        if (sender == null
                || receiver == null
                || sender.equals(receiver)
                || amount <= 0) {

            return false;
        }

        if (!withdraw(sender, amount)) {
            return false;
        }

        deposit(receiver, amount);

        return true;
    }

    public Map<UUID, Double> getBalances() {
        return balances;
    }

    public void saveAll() {
        /*
         * La persistencia permanente de la economía
         * se conectará en el sistema de almacenamiento.
         */
    }
}