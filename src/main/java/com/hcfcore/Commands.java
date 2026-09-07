package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

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

        String cmd = command.getName().toLowerCase(Locale.ROOT);

        switch (cmd) {

            case "hcf":
                return hcf(sender, args);

            case "f":
            case "faction":
            case "fac":
                return faction(sender, args);

            case "claim":
                return claim(sender, args);

            case "combat":
                return combat(sender);

            case "pay":
                return pay(sender, args);

            case "kit":
                return kit(sender, args);

            case "class":
                return classCommand(sender, args);

            case "spawn":
                return spawn(sender);

            case "koth":
                return koth(sender, args);

            case "airdrop":
                return airdrop(sender, args);

            case "lives":
                return lives(sender, args);

            case "deathban":
                return deathban(sender, args);

            case "balance":
                return balance(sender);

            default:
                return false;
        }
    }

    // =========================================================
    // /HCF
    // =========================================================

    private boolean hcf(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission("hcf.admin")) {
            sender.sendMessage(
                    ChatColor.RED + "No tienes permiso."
            );
            return true;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━"
            );
            sender.sendMessage(
                    ChatColor.YELLOW + "HCFCore " +
                    ChatColor.GRAY + "v" +
                    plugin.getDescription().getVersion()
            );
            sender.sendMessage("");
            sender.sendMessage(
                    ChatColor.WHITE + "/hcf reload"
            );
            sender.sendMessage(
                    ChatColor.WHITE + "/airdrop spawn"
            );
            sender.sendMessage(
                    ChatColor.WHITE + "/airdrop remove"
            );
            sender.sendMessage(
                    ChatColor.WHITE + "/koth start"
            );
            sender.sendMessage(
                    ChatColor.WHITE + "/koth stop"
            );
            sender.sendMessage(
                    ChatColor.WHITE + "/hcf editor"
            );
            sender.sendMessage(
                    ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            plugin.reloadConfig();

            sender.sendMessage(
                    ChatColor.GREEN +
                    "Configuración recargada correctamente."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("editor")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(
                        ChatColor.RED +
                        "Este comando solo puede usarse dentro del servidor."
                );
                return true;
            }

            Player player = (Player) sender;

            plugin.getGuiManager()
                    .openMain(player);

            return true;
        }

        sender.sendMessage(
                ChatColor.RED +
                "Uso: /hcf <reload|editor>"
        );

        return true;
    }

    // =========================================================
    // /F
    // =========================================================

    private boolean faction(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        if (faction == null) {

            player.sendMessage(
                    ChatColor.RED +
                    "No perteneces a ninguna faction."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Usa /f create <nombre>."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Faction: " +
                ChatColor.WHITE +
                faction.getName()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "DTR: " +
                ChatColor.RED +
                faction.getDtr()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Kills: " +
                ChatColor.GREEN +
                plugin.getFactionManager()
                        .getKills(player)
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Deaths: " +
                ChatColor.RED +
                plugin.getFactionManager()
                        .getDeaths(player)
        );

        player.sendMessage(
                ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━"
        );

        return true;
    }

    // =========================================================
    // /CLAIM
    // =========================================================

    private boolean claim(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Usos:"
            );

            player.sendMessage(
                    ChatColor.WHITE +
                    "/claim"
            );

            player.sendMessage(
                    ChatColor.WHITE +
                    "/claim unclaim"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("unclaim")) {

            if (plugin.getClaimManager()
                    .unclaim(player)) {

                player.sendMessage(
                        ChatColor.GREEN +
                        "Claim abandonado correctamente."
                );

            } else {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes abandonar este claim."
                );
            }

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                "Uso: /claim [unclaim]"
        );

        return true;
    }

    // =========================================================
    // /COMBAT
    // =========================================================

    private boolean combat(
            CommandSender sender
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        long remaining =
                plugin.getCombatManager()
                        .getRemainingSeconds(player);

        if (remaining <= 0) {

            player.sendMessage(
                    ChatColor.GREEN +
                    "No estás en combate."
            );

        } else {

            player.sendMessage(
                    ChatColor.RED +
                    "Estás en combate durante " +
                    ChatColor.WHITE +
                    remaining +
                    "s."
            );
        }

        return true;
    }

    // =========================================================
    // /PAY
    // =========================================================

    private boolean pay(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {

            player.sendMessage(
                    ChatColor.RED +
                    "Uso: /pay <jugador> <cantidad>"
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(args[0]);

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

            amount = Double.parseDouble(args[1]);

        } catch (NumberFormatException e) {

            player.sendMessage(
                    ChatColor.RED +
                    "La cantidad no es válida."
            );

            return true;
        }

        if (!Double.isFinite(amount) || amount <= 0) {

            player.sendMessage(
                    ChatColor.RED +
                    "La cantidad debe ser mayor que 0."
            );

            return true;
        }

        if (!plugin.getEconomyManager()
                .transfer(player, target, amount)) {

            player.sendMessage(
                    ChatColor.RED +
                    "No tienes suficiente dinero."
            );

            return true;
        }

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
    // /KIT
    // =========================================================

    private boolean kit(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Uso: /kit <nombre>"
            );

            return true;
        }

        String kitName = args[0];

        if (!plugin.getKitManager()
                .kitExists(kitName)) {

            player.sendMessage(
                    ChatColor.RED +
                    "Ese kit no existe."
            );

            return true;
        }

        if (!plugin.getKitManager()
                .canUseKit(player, kitName)) {

            long remaining =
                    plugin.getKitManager()
                            .getRemainingSeconds(
                                    player,
                                    kitName
                            );

            player.sendMessage(
                    ChatColor.RED +
                    "Debes esperar " +
                    ChatColor.WHITE +
                    remaining +
                    "s."
            );

            return true;
        }

        if (!plugin.getKitManager()
                .giveKit(player, kitName)) {

            player.sendMessage(
                    ChatColor.RED +
                    "No se pudo entregar el kit."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                "Has recibido el kit " +
                ChatColor.YELLOW +
                kitName +
                ChatColor.GREEN +
                "."
        );

        return true;
    }

    // =========================================================
    // /CLASS
    // =========================================================

    private boolean classCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            String current =
                    plugin.getClassManager()
                            .getClass(player);

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Clase actual: " +
                    ChatColor.WHITE +
                    (current == null ? "Ninguna" : current)
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Usa /class archer o /class bard."
            );

            return true;
        }

        String className =
                args[0].toLowerCase(Locale.ROOT);

        if (!className.equals("archer")
                && !className.equals("bard")) {

            player.sendMessage(
                    ChatColor.RED +
                    "Clases disponibles: Archer, Bard."
            );

            return true;
        }

        if (!plugin.getClassManager()
                .setClass(player, className)) {

            player.sendMessage(
                    ChatColor.RED +
                    "No se pudo seleccionar esa clase."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                "Ahora eres " +
                ChatColor.YELLOW +
                className +
                ChatColor.GREEN +
                "."
        );

        return true;
    }

    // =========================================================
    // /SPAWN
    // =========================================================

    private boolean spawn(
            CommandSender sender
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        String worldName =
                plugin.getConfig()
                        .getString(
                                "world.name",
                                "world"
                        );

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {

            player.sendMessage(
                    ChatColor.RED +
                    "El mundo configurado no existe."
            );

            return true;
        }

        Location spawn =
                world.getSpawnLocation();

        player.teleport(spawn);

        player.sendMessage(
                ChatColor.GREEN +
                "Has sido teletransportado al spawn."
        );

        return true;
    }

    // =========================================================
    // /KOTH
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
                    "Uso: /koth <start|stop|set>"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {

            if (plugin.getKothManager()
                    .isActive()) {

                sender.sendMessage(
                        ChatColor.RED +
                        "El KOTH ya está activo."
                );

                return true;
            }

            if (plugin.getKothManager()
                    .getLocation() == null) {

                sender.sendMessage(
                        ChatColor.RED +
                        "El KOTH no tiene una ubicación configurada."
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

            if (!plugin.getKothManager()
                    .isActive()) {

                sender.sendMessage(
                        ChatColor.RED +
                        "El KOTH no está activo."
                );

                return true;
            }

            plugin.getKothManager()
                    .stopKoth();

            sender.sendMessage(
                    ChatColor.GREEN +
                    "KOTH detenido."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {

            if (!(sender instanceof Player)) {

                sender.sendMessage(
                        ChatColor.RED +
                        "Este comando debe ejecutarse dentro del servidor."
                );

                return true;
            }

            Player player = (Player) sender;

            plugin.getKothManager()
                    .setLocation(
                            player.getLocation()
                    );

            player.sendMessage(
                    ChatColor.GREEN +
                    "Ubicación del KOTH guardada."
            );

            return true;
        }

        sender.sendMessage(
                ChatColor.RED +
                "Uso: /koth <start|stop|set>"
        );

        return true;
    }

    // =========================================================
    // /AIRDROP
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
                    "Uso: /airdrop <spawn|remove>"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {

            plugin.getAirdropManager()
                    .spawnAirdrop();

            sender.sendMessage(
                    ChatColor.GREEN +
                    "Airdrop creado."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {

            plugin.getAirdropManager()
                    .removeActiveDrop();

            sender.sendMessage(
                    ChatColor.GREEN +
                    "Airdrop eliminado."
            );

            return true;
        }

        sender.sendMessage(
                ChatColor.RED +
                "Uso: /airdrop <spawn|remove>"
        );

        return true;
    }

    // =========================================================
    // /LIVES
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
                    ChatColor.RED +
                    "Uso: /lives <jugador> <cantidad>"
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(args[0]);

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
                    Integer.parseInt(args[1]);

        } catch (NumberFormatException e) {

            sender.sendMessage(
                    ChatColor.RED +
                    "La cantidad debe ser un número entero."
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
                .addLife(target, amount);

        sender.sendMessage(
                ChatColor.GREEN +
                "Has añadido " +
                amount +
                " vida(s) a " +
                target.getName() +
                "."
        );

        target.sendMessage(
                ChatColor.GREEN +
                "Has recibido " +
                amount +
                " vida(s)."
        );

        return true;
    }

    // =========================================================
    // /DEATHBAN
    // =========================================================

    private boolean deathban(
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

        if (args.length < 1) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Uso: /deathban <jugador>"
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(args[0]);

        if (target == null) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Ese jugador no está conectado."
            );

            return true;
        }

        plugin.getDeathbanManager()
                .revive(target);

        sender.sendMessage(
                ChatColor.GREEN +
                "Deathban eliminado para " +
                target.getName() +
                "."
        );

        target.sendMessage(
                ChatColor.GREEN +
                "Tu deathban ha sido eliminado."
        );

        return true;
    }

    // =========================================================
    // /BALANCE
    // =========================================================

    private boolean balance(
            CommandSender sender
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede usarse por jugadores."
            );

            return true;
        }

        Player player = (Player) sender;

        double balance =
                plugin.getEconomyManager()
                        .getBalance(player);

        player.sendMessage(
                ChatColor.GREEN +
                "Tu balance: " +
                ChatColor.WHITE +
                "$" +
                formatMoney(balance)
        );

        return true;
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    private String formatMoney(double amount) {

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }
}