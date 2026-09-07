package com.hcfcore;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final HCFCore plugin;

    public Commands(HCFCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        String cmd = command.getName().toLowerCase();

        switch (cmd) {

            case "hcf":
                return hcf(sender, args);

            case "airdrop":
                return airdrop(sender, args);

            case "koth":
                return koth(sender, args);

            case "pay":
                return pay(sender, args);

            case "lives":
                return lives(sender, args);

            case "class":
                return classCommand(sender, args);

            case "spawn":
                return spawn(sender);

            case "balance":
                return balance(sender);

            case "f":
            case "faction":
            case "fac":
                return faction(sender, args);

            default:
                return false;
        }
    }

    // =========================================================
    // HCF
    // =========================================================

    private boolean hcf(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission("hcf.admin")) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );
            return true;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "HCFCore"
            );

            sender.sendMessage(
                    ChatColor.GRAY +
                            "/hcf reload"
            );

            sender.sendMessage(
                    ChatColor.GRAY +
                            "/airdrop spawn"
            );

            sender.sendMessage(
                    ChatColor.GRAY +
                            "/koth start"
            );

            sender.sendMessage(
                    ChatColor.GRAY +
                            "/koth stop"
            );

            sender.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            plugin.reloadConfig();

            sender.sendMessage(
                    ChatColor.GREEN +
                            "Configuración recargada."
            );

            return true;
        }

        return true;
    }

    // =========================================================
    // AIRDROP
    // =========================================================

    private boolean airdrop(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission("hcf.admin")) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );
            return true;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/airdrop spawn"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {

            plugin.getAirdropManager()
                    .spawnAirdrop();

            sender.sendMessage(
                    ChatColor.GREEN +
                            "Airdrop generado."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {

            plugin.getAirdropManager()
                    .removeActiveDrop();

            sender.sendMessage(
                    ChatColor.RED +
                            "Airdrop eliminado."
            );

            return true;
        }

        return true;
    }

    // =========================================================
    // KOTH
    // =========================================================

    private boolean koth(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission("hcf.admin")) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );
            return true;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/koth start"
            );

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/koth stop"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {

            if (plugin.getKothManager().isActive()) {

                sender.sendMessage(
                        ChatColor.RED +
                                "El KOTH ya está activo."
                );

                return true;
            }

            if (plugin.getKothManager().getLocation() == null) {

                sender.sendMessage(
                        ChatColor.RED +
                                "El KOTH no tiene una ubicación configurada."
                );

                sender.sendMessage(
                        ChatColor.GRAY +
                                "Usa el editor para establecerla."
                );

                return true;
            }

            plugin.getKothManager()
                    .startKoth();

            sender.sendMessage(
                    ChatColor.GREEN +
                            "KOTH iniciado."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {

            if (!plugin.getKothManager().isActive()) {

                sender.sendMessage(
                        ChatColor.RED +
                                "El KOTH no está activo."
                );

                return true;
            }

            plugin.getKothManager()
                    .stopKoth();

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "KOTH detenido."
            );

            return true;
        }

        return true;
    }

    // =========================================================
    // PAY
    // =========================================================

    private boolean pay(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Este comando solo puede usarlo un jugador."
            );

            return true;
        }

        Player player =
                (Player) sender;

        if (args.length < 2) {

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/pay <jugador> <cantidad>"
            );

            return true;
        }

        Player target =
                plugin.getServer()
                        .getPlayerExact(args[0]);

        if (target == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Ese jugador no está conectado."
            );

            return true;
        }

        if (target.equals(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No puedes pagarte a ti mismo."
            );

            return true;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            args[1]
                    );

        } catch (NumberFormatException e) {

            player.sendMessage(
                    ChatColor.RED +
                            "Cantidad inválida."
            );

            return true;
        }

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0) {

            player.sendMessage(
                    ChatColor.RED +
                            "La cantidad debe ser mayor que 0."
            );

            return true;
        }

        double balance =
                plugin.getEconomyManager()
                        .getBalance(player);

        if (balance < amount) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes suficiente dinero."
            );

            return true;
        }

        plugin.getEconomyManager()
                .transfer(
                        player,
                        target,
                        amount
                );

        player.sendMessage(
                ChatColor.GREEN +
                        "Enviaste $" +
                        formatMoney(amount) +
                        " a " +
                        target.getName() +
                        "."
        );

        target.sendMessage(
                ChatColor.GREEN +
                        "Recibiste $" +
                        formatMoney(amount) +
                        " de " +
                        player.getName() +
                        "."
        );

        return true;
    }

    // =========================================================
    // LIVES
    // =========================================================

    private boolean lives(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission("hcf.admin")) {

            sender.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        if (args.length < 2) {

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/lives <jugador> <cantidad>"
            );

            return true;
        }

        Player target =
                plugin.getServer()
                        .getPlayerExact(args[0]);

        if (target == null) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Ese jugador no está conectado."
            );

            return true;
        }

        int amount;

        try {

            amount =
                    Integer.parseInt(
                            args[1]
                    );

        } catch (NumberFormatException e) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Cantidad inválida."
            );

            return true;
        }

        if (amount <= 0) {

            sender.sendMessage(
                    ChatColor.RED +
                            "La cantidad debe ser mayor que 0."
            );

            return true;
        }

        plugin.getDeathbanManager()
                .addLife(
                        target,
                        amount
                );

        sender.sendMessage(
                ChatColor.GREEN +
                        "Se agregaron " +
                        amount +
                        " vidas a " +
                        target.getName() +
                        "."
        );

        return true;
    }

    // =========================================================
    // CLASS
    // =========================================================

    private boolean classCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Solo jugadores."
            );

            return true;
        }

        Player player =
                (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/class archer"
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/class bard"
            );

            return true;
        }

        String selected =
                args[0].toLowerCase();

        if (!selected.equals("archer")
                && !selected.equals("bard")) {

            player.sendMessage(
                    ChatColor.RED +
                            "Clase inválida."
            );

            return true;
        }

        plugin.getClassManager()
                .setClass(
                        player,
                        selected
                );

        player.sendMessage(
                ChatColor.GREEN +
                        "Elegiste la clase " +
                        selected +
                        "."
        );

        return true;
    }

    // =========================================================
    // SPAWN
    // =========================================================

    private boolean spawn(
            CommandSender sender
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Solo jugadores."
            );

            return true;
        }

        Player player =
                (Player) sender;

        player.teleport(
                plugin.getServer()
                        .getWorld(
                                plugin.getConfig()
                                        .getString(
                                                "world.name",
                                                "world"
                                        )
                        )
                        .getSpawnLocation()
        );

        player.sendMessage(
                ChatColor.GREEN +
                        "Teletransportado al spawn."
        );

        return true;
    }

    // =========================================================
    // BALANCE
    // =========================================================

    private boolean balance(
            CommandSender sender
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Solo jugadores."
            );

            return true;
        }

        Player player =
                (Player) sender;

        double balance =
                plugin.getEconomyManager()
                        .getBalance(player);

        player.sendMessage(
                ChatColor.GREEN +
                        "Balance: $" +
                        formatMoney(balance)
        );

        return true;
    }

    // =========================================================
    // FACTION
    // =========================================================

    private boolean faction(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Solo jugadores."
            );

            return true;
        }

        Player player =
                (Player) sender;

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        if (args.length == 0) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "No perteneces a ninguna faction."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "/f create <nombre>"
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Faction: " +
                            faction.getName()
            );

            player.sendMessage(
                    ChatColor.RED +
                            "DTR: " +
                            faction.getDtr()
            );

            player.sendMessage(
                    ChatColor.GREEN +
                            "Kills: " +
                            plugin.getFactionManager()
                                    .getKills(player)
            );

            player.sendMessage(
                    ChatColor.RED +
                            "Deaths: " +
                            plugin.getFactionManager()
                                    .getDeaths(player)
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            return true;
        }

        return true;
    }

    // =========================================================
    // UTIL
    // =========================================================

    private String formatMoney(
            double amount
    ) {

        return String.format(
                java.util.Locale.US,
                "%.2f",
                amount
        );
    }
}