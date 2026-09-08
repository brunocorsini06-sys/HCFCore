package com.hcfcore;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathbanManager {

    private final HCFCore plugin;

    private final Map<UUID, Long> deathbans = new HashMap<>();
    private final Map<UUID, Integer> lives = new HashMap<>();

    public DeathbanManager(HCFCore plugin) {
        this.plugin = plugin;

        loadAll();
    }

    /**
     * Configura al jugador.
     */
    public void setupPlayer(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        lives.putIfAbsent(
                uuid,
                plugin.getConfig()
                        .getInt(
                                "deathban.starting-lives",
                                0
                        )
        );

        saveLives(uuid);
    }

    /**
     * Obtiene las vidas del jugador.
     */
    public int getLives(Player player) {

        if (player == null) {
            return 0;
        }

        setupPlayer(player);

        return lives.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    /**
     * Obtiene las vidas mediante UUID.
     */
    public int getLives(UUID uuid) {

        if (uuid == null) {
            return 0;
        }

        return lives.getOrDefault(
                uuid,
                0
        );
    }

    /**
     * Agrega una vida.
     */
    public void addLife(Player player) {

        if (player == null) {
            return;
        }

        addLife(player.getUniqueId());
    }

    /**
     * Agrega una vida mediante UUID.
     */
    public void addLife(UUID uuid) {

        if (uuid == null) {
            return;
        }

        int current =
                lives.getOrDefault(
                        uuid,
                        0
                );

        lives.put(
                uuid,
                current + 1
        );

        saveLives(uuid);
    }

    /**
     * Elimina una vida.
     *
     * @return true si pudo quitarse.
     */
    public boolean removeLife(Player player) {

        if (player == null) {
            return false;
        }

        return removeLife(
                player.getUniqueId()
        );
    }

    /**
     * Elimina una vida mediante UUID.
     */
    public boolean removeLife(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        int current =
                lives.getOrDefault(
                        uuid,
                        0
                );

        if (current <= 0) {
            return false;
        }

        lives.put(
                uuid,
                current - 1
        );

        saveLives(uuid);

        return true;
    }

    /**
     * Aplica un deathban.
     */
    public void deathban(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        /*
         * Si el sistema de vidas está habilitado,
         * primero consume una vida.
         */
        boolean useLives =
                plugin.getConfig()
                        .getBoolean(
                                "deathban.use-lives",
                                true
                        );

        if (useLives) {

            int currentLives =
                    lives.getOrDefault(
                            uuid,
                            0
                    );

            if (currentLives > 0) {

                lives.put(
                        uuid,
                        currentLives - 1
                );

                saveLives(uuid);

                /*
                 * Si todavía tiene vidas,
                 * no recibe deathban.
                 */
                return;
            }
        }

        long seconds =
                plugin.getConfig()
                        .getLong(
                                "deathban.duration-seconds",
                                86400
                        );

        /*
         * 0 = permanente.
         */
        long expiresAt;

        if (seconds <= 0) {

            expiresAt = 0L;

        } else {

            expiresAt =
                    System.currentTimeMillis()
                            + (seconds * 1000L);
        }

        deathbans.put(
                uuid,
                expiresAt
        );

        saveDeathban(uuid);
    }

    /**
     * Comprueba si el jugador está deathbaneado.
     */
    public boolean isDeathbanned(Player player) {

        if (player == null) {
            return false;
        }

        return isDeathbanned(
                player.getUniqueId()
        );
    }

    /**
     * Comprueba deathban mediante UUID.
     */
    public boolean isDeathbanned(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        Long expiresAt =
                deathbans.get(uuid);

        if (expiresAt == null) {
            return false;
        }

        /*
         * 0 = permanente.
         */
        if (expiresAt == 0L) {
            return true;
        }

        /*
         * Deathban expirado.
         */
        if (System.currentTimeMillis()
                >= expiresAt) {

            deathbans.remove(uuid);

            deleteDeathban(uuid);

            return false;
        }

        return true;
    }

    /**
     * Obtiene los segundos restantes.
     *
     * -1 = permanente
     *  0 = no deathban
     */
    public long getRemainingSeconds(Player player) {

        if (player == null) {
            return 0L;
        }

        return getRemainingSeconds(
                player.getUniqueId()
        );
    }

    /**
     * Obtiene los segundos restantes mediante UUID.
     */
    public long getRemainingSeconds(UUID uuid) {

        if (uuid == null) {
            return 0L;
        }

        Long expiresAt =
                deathbans.get(uuid);

        if (expiresAt == null) {
            return 0L;
        }

        if (expiresAt == 0L) {
            return -1L;
        }

        long remaining =
                expiresAt
                        - System.currentTimeMillis();

        if (remaining <= 0L) {

            deathbans.remove(uuid);

            deleteDeathban(uuid);

            return 0L;
        }

        return (remaining + 999L) / 1000L;
    }

    /**
     * Revive a un jugador.
     */
    public boolean revive(Player player) {

        if (player == null) {
            return false;
        }

        return revive(
                player.getUniqueId()
        );
    }

    /**
     * Revive mediante UUID.
     */
    public boolean revive(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        if (!deathbans.containsKey(uuid)) {
            return false;
        }

        deathbans.remove(uuid);

        deleteDeathban(uuid);

        return true;
    }

    /**
     * Devuelve todos los deathbans.
     */
    public Map<UUID, Long> getDeathbans() {
        return deathbans;
    }

    /**
     * Devuelve todas las vidas.
     */
    public Map<UUID, Integer> getLivesMap() {
        return lives;
    }

    /**
     * Carga deathbans y vidas desde SQLite.
     */
    public void loadAll() {

        DatabaseManager database =
                plugin.getDatabaseManager();

        if (database == null
                || !database.isConnected()) {

            plugin.getLogger().warning(
                    "No se pudieron cargar deathbans/lives: SQLite no está conectado."
            );

            return;
        }

        Connection connection =
                database.getConnection();

        if (connection == null) {
            return;
        }

        deathbans.clear();
        lives.clear();

        /*
         * =========================
         * DEATHBANS
         * =========================
         */

        String deathbanSql = """
                SELECT uuid, expires_at
                FROM deathbans
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             deathbanSql
                     );
             ResultSet result =
                     statement.executeQuery()) {

            int loaded = 0;

            while (result.next()) {

                String uuidString =
                        result.getString("uuid");

                long expiresAt =
                        result.getLong("expires_at");

                try {

                    UUID uuid =
                            UUID.fromString(
                                    uuidString
                            );

                    /*
                     * Si ya expiró, no lo cargamos.
                     */
                    if (expiresAt != 0L
                            && System.currentTimeMillis()
                            >= expiresAt) {

                        continue;
                    }

                    deathbans.put(
                            uuid,
                            expiresAt
                    );

                    loaded++;

                } catch (IllegalArgumentException ignored) {

                    plugin.getLogger().warning(
                            "UUID inválido en deathbans: "
                                    + uuidString
                    );
                }
            }

            plugin.getLogger().info(
                    "Deathbans cargados desde SQLite: "
                            + loaded
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando deathbans desde SQLite."
            );

            e.printStackTrace();
        }

        /*
         * =========================
         * LIVES
         * =========================
         */

        String livesSql = """
                SELECT uuid, amount
                FROM lives
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             livesSql
                     );
             ResultSet result =
                     statement.executeQuery()) {

            int loaded = 0;

            while (result.next()) {

                String uuidString =
                        result.getString("uuid");

                int amount =
                        result.getInt("amount");

                try {

                    UUID uuid =
                            UUID.fromString(
                                    uuidString
                            );

                    lives.put(
                            uuid,
                            Math.max(
                                    0,
                                    amount
                            )
                    );

                    loaded++;

                } catch (IllegalArgumentException ignored) {

                    plugin.getLogger().warning(
                            "UUID inválido en lives: "
                                    + uuidString
                    );
                }
            }

            plugin.getLogger().info(
                    "Lives cargadas desde SQLite: "
                            + loaded
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Error cargando lives desde SQLite."
            );

            e.printStackTrace();
        }
    }

    /**
     * Guarda un deathban.
     */
    private void saveDeathban(UUID uuid) {

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
                INSERT INTO deathbans
                (uuid, expires_at)
                VALUES (?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET expires_at = excluded.expires_at
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setLong(
                    2,
                    deathbans.getOrDefault(
                            uuid,
                            0L
                    )
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudo guardar el deathban de "
                            + uuid
            );

            e.printStackTrace();
        }
    }

    /**
     * Elimina un deathban de SQLite.
     */
    private void deleteDeathban(UUID uuid) {

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

        String sql =
                "DELETE FROM deathbans WHERE uuid = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudo eliminar el deathban de "
                            + uuid
            );

            e.printStackTrace();
        }
    }

    /**
     * Guarda las vidas de un jugador.
     */
    private void saveLives(UUID uuid) {

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
                INSERT INTO lives
                (uuid, amount)
                VALUES (?, ?)
                ON CONFLICT(uuid)
                DO UPDATE SET amount = excluded.amount
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setInt(
                    2,
                    lives.getOrDefault(
                            uuid,
                            0
                    )
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().warning(
                    "No se pudieron guardar las lives de "
                            + uuid
            );

            e.printStackTrace();
        }
    }

    /**
     * Guarda todo.
     */
    public void saveAll() {

        for (UUID uuid : deathbans.keySet()) {
            saveDeathban(uuid);
        }

        for (UUID uuid : lives.keySet()) {
            saveLives(uuid);
        }
    }
}