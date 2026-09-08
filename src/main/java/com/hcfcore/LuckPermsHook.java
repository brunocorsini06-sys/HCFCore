package com.hcfcore;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LuckPermsHook {

    private final HCFCore plugin;
    private LuckPerms luckPerms;

    public LuckPermsHook(HCFCore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {

        try {

            luckPerms = LuckPermsProvider.get();

            plugin.getLogger().info(
                    "LuckPerms conectado correctamente."
            );

        } catch (IllegalStateException exception) {

            plugin.getLogger().severe(
                    "No se pudo conectar con LuckPerms."
            );

            throw exception;
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public boolean hasPermission(Player player, String permission) {

        if (player == null || permission == null || permission.isBlank()) {
            return false;
        }

        return player.hasPermission(permission);
    }

    public boolean hasPermission(UUID uuid, String permission) {

        if (uuid == null || permission == null || permission.isBlank()) {
            return false;
        }

        User user = luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return false;
        }

        return user.getCachedData()
                .getPermissionData()
                .checkPermission(permission)
                .asBoolean();
    }

    public String getPrimaryGroup(Player player) {

        if (player == null) {
            return "default";
        }

        return getPrimaryGroup(player.getUniqueId());
    }

    public String getPrimaryGroup(UUID uuid) {

        if (uuid == null) {
            return "default";
        }

        User user = luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return "default";
        }

        String group = user.getPrimaryGroup();

        if (group == null || group.isBlank()) {
            return "default";
        }

        return group;
    }

    public String getPrefix(Player player) {

        if (player == null) {
            return "";
        }

        User user = luckPerms.getUserManager().getUser(
                player.getUniqueId()
        );

        if (user == null) {
            return "";
        }

        String prefix = user.getCachedData()
                .getMetaData()
                .getPrefix();

        return prefix == null ? "" : prefix;
    }

    public String getSuffix(Player player) {

        if (player == null) {
            return "";
        }

        User user = luckPerms.getUserManager().getUser(
                player.getUniqueId()
        );

        if (user == null) {
            return "";
        }

        String suffix = user.getCachedData()
                .getMetaData()
                .getSuffix();

        return suffix == null ? "" : suffix;
    }

    public void setPermission(UUID uuid, String permission, boolean value) {

        if (uuid == null || permission == null || permission.isBlank()) {
            return;
        }

        User user = luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return;
        }

        Node node = Node.builder(permission)
                .value(value)
                .build();

        user.data().add(node);

        luckPerms.getUserManager().saveUser(user);
    }

    public void removePermission(UUID uuid, String permission) {

        if (uuid == null || permission == null || permission.isBlank()) {
            return;
        }

        User user = luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return;
        }

        Node node = Node.builder(permission)
                .build();

        user.data().remove(node);

        luckPerms.getUserManager().saveUser(user);
    }

    public boolean isConnected() {
        return luckPerms != null;
    }
}