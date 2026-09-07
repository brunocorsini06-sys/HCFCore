package com.hcfcore;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final HCFCore plugin;

    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(HCFCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    /**
     * Configura el jugador si todavía no existe.
     */
    public void setupPlayer(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (balances.containsKey(uuid)) {
            return;
        }

        balances.put(
                uuid,
                plugin.getConfig()
                        .getDouble("economy.starting-balance", 0.0)
        );

        savePlayer(uuid);
    }

    /**
     * Obtiene el balance de un jugador.
     */
    public double getBalance(Player player) {

        if (player == null) {
            return 0.0;
        }

        setupPlayer(player);

        return balances.getOrDefault(
                player.getUniqueId(),
                0.0
        );
    }

    /**
     * Obtiene el balance mediante UUID.
     */
    public double getBalance(UUID uuid) {

        if (uuid == null) {
            return 0.0;
        }

        return balances.getOrDefault(uuid, 0.0);
    }

    /**
     * Establece el balance.
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

        UUID uuid = player.getUniqueId();

        balances.put(
                uuid,
                Math.max(0.0, amount)
        );

        savePlayer(uuid);
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

        balances.put(
                uuid,
                Math.max(0.0, amount)
        );

        savePlayer(uuid);
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

        UUID uuid = player.getUniqueId();

        double current =
                balances.getOrDefault(uuid, 0.0);

        balances.put(
                uuid,
                current + amount
        );

        savePlayer(uuid);
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

        double current =
                balances.getOrDefault(uuid, 0.0);

        balances.put(
                uuid,
                current + amount
        );

        savePlayer(uuid);
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

        UUID uuid = player.getUniqueId();

        double current =
                balances.getOrDefault(uuid, 0.0);

        if (current < amount) {
            return false;
        }

        balances.put(
                uuid,
                current - amount
        );

        savePlayer(uuid);

        return true;
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

        double current =
                balances.getOrDefault(uuid, 0.0);

        if (current < amount) {
            return false;
        }

        balances.put(
                uuid,
                current - amount
        );

        savePlayer(uuid);

        return true;
    }

    /**
     * Transfiere dinero entre jugadores.
     *
     * @return true si la transferencia fue realizada.
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

        UUID senderUUID =
                sender.getUniqueId();

        UUID receiverUUID =
                receiver.getUniqueId();

        double senderBalance =
                balances.getOrDefault(
                        senderUUID,
                        0.0
                );

        if (senderBalance < amount) {
            return false;
        }

        balances.put(
                senderUUID,
                senderBalance - amount
        );

        double receiverBalance =
                balances.getOrDefault(
                        receiverUUID,
                        0.0
                );

        balances.put(
                receiverUUID,
                receiverBalance + amount
        );

        savePlayer(senderUUID);
        savePlayer(receiverUUID);

        return true;
    }

    /**
     * Devuelve todos los balances.
     */
    public Map<UUID, Double> getBalances() {
        return balances;
    }

    /**
     * Guarda un jugador concreto.
     */
    private void savePlayer(UUID uuid) {

        if (uuid == null) {
            return;
        }

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        String sql = """
                INSERT INTO economy
                (uuid, balance)
                VALUES (?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET balance = excluded.balance
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setDouble(
                    2,
                    balances.getOrDefault(
                            uuid,
                            0.0
                    )
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudo guardar la economía del jugador "
                            + uuid
            );

            e.printStackTrace();
        }
    }

    /**
     * Carga toda la economía desde SQLite.
     */
    public void loadAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            plugin.getLogger().warning(
                    "No se pudo cargar la economía: SQLite no está conectado."
            );

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        balances.clear();

        String sql = """
                SELECT uuid, balance
                FROM economy
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result =
                     statement.executeQuery()) {

            int loaded = 0;

            while (result.next()) {

                String uuidString =
                        result.getString("uuid");

                double balance =
                        result.getDouble("balance");

                try {

                    UUID uuid =
                            UUID.fromString(uuidString);

                    balances.put(
                            uuid,
                            Math.max(
                                    0.0,
                                    balance
                            )
                    );

                    loaded++;

                } catch (IllegalArgumentException ignored) {

                    plugin.getLogger().warning(
                            "UUID inválido en economy: "
                                    + uuidString
                    );
                }
            }

            plugin.getLogger().info(
                    "Balances cargados desde SQLite: "
                            + loaded
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando la economía desde SQLite."
            );

            e.printStackTrace();
        }
    }

    /**
     * Guarda toda la economía.
     */
    public void saveAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        String sql = """
                INSERT INTO economy
                (uuid, balance)
                VALUES (?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET balance = excluded.balance
                """;

        try {

            boolean previousAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                for (Map.Entry<UUID, Double> entry
                        : balances.entrySet()) {

                    UUID uuid =
                            entry.getKey();

                    Double balance =
                            entry.getValue();

                    if (uuid == null
                            || balance == null
                            || !isValidAmount(balance)) {

                        continue;
                    }

                    statement.setString(
                            1,
                            uuid.toString()
                    );

                    statement.setDouble(
                            2,
                            Math.max(
                                    0.0,
                                    balance
                            )
                    );

                    statement.addBatch();
                }

                statement.executeBatch();

                connection.commit();

            } catch (SQLException e) {

                connection.rollback();

                throw e;

            } finally {

                connection.setAutoCommit(
                        previousAutoCommit
                );
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error guardando la economía en SQLite."
            );

            e.printStackTrace();
        }
    }

    /**
     * Comprueba que el número sea válido.
     */
    private boolean isValidAmount(double amount) {

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