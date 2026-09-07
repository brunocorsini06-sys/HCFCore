package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class HCFCore extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupWorldBorder();

        getLogger().info("HCFCore 1.0.0 enabled.");
    }

    private void setupWorldBorder() {
        if (!getConfig().getBoolean("world-border.enabled", true)) {
            return;
        }

        World world = Bukkit.getWorlds().get(0);
        WorldBorder border = world.getWorldBorder();

        border.setCenter(
                getConfig().getDouble("world-border.center-x"),
                getConfig().getDouble("world-border.center-z")
        );

        border.setSize(
                getConfig().getDouble("world-border.size", 5000)
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("hcf")) {
            return false;
        }

        if (!sender.hasPermission("hcfcore.admin")) {
            sender.sendMessage(
                    ChatColor.RED + "No tienes permiso."
            );
            return true;
        }

        if (args.length == 1 &&
                args[0].equalsIgnoreCase("reload")) {

            reloadConfig();
            setupWorldBorder();

            sender.sendMessage(
                    ChatColor.GREEN +
                    "HCFCore recargada correctamente."
            );

            return true;
        }

        sender.sendMessage(
                ChatColor.GOLD +
                "HCFCore " +
                getDescription().getVersion()
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                "/hcf reload"
        );

        return true;
    }
}