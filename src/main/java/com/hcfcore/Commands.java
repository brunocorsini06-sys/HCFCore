package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        String cmd =
                command.getName().toLowerCase(Locale.ROOT);

        switch (cmd) {

            case "hcf":
                return hcfCommand(sender, args);

            case "airdrop":
                return airdropCommand(sender, args);

            case "kit":
                return kitCommand(sender, args);

            case "koth":
                return kothCommand(sender, args);

            case "claim":
                return claimCommand(sender, args);

            case "combat":
                return combatCommand(sender, args);

            case "pay":
                return payCommand(sender, args);

            case "balance":
                return balanceCommand(sender);

            case "lives":
                return livesCommand(sender, args);

            case "deathban":
                return deathbanCommand(sender, args);

            case "class":
                return classCommand(sender, args);

            case "spawn":
                return spawnCommand(sender);

            case "f":
                return factionCommand(sender, args);

            default:
                return false;
        }
    }

    /*
     * =========================
     * HCF
     * =========================
     */

    private boolean hcfCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {

            sender.sendMessage(
                    ChatColor.RED +
                            "Este comando requiere un jugador."
            );

            return true;
        }

        Player player = (Player) sender;

        if (!hasAdmin(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "HCFCore"
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "/hcf editor"
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "/hcf reload"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "━━━━━━━━━━━━━━━━━━━━"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("editor")) {

            if (plugin.getGuiManager() != null) {

                plugin.getGuiManager()
                        .openMain(player);
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            plugin.reloadConfig();

            player.sendMessage(
                    ChatColor.GREEN +
                            "✓ Configuración recargada."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                        "Subcomando desconocido."
        );

        return true;
    }

    /*
     * =========================
     * AIRDROP
     * =========================
     */

    private boolean airdropCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.GOLD +
                            "/airdrop edit"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "/airdrop spawn"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "/airdrop remove"
            );

            return true;
        }

        if (!hasAdmin(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("edit")) {

            plugin.getGuiManager()
                    .openAirdrop(player);

            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {

            if (plugin.getAirdropManager()
                    .spawnAirdrop()) {

                player.sendMessage(
                        ChatColor.GREEN +
                                "✓ Airdrop creado."
                );

            } else {

                player.sendMessage(
                        ChatColor.RED +
                                "No se pudo crear el airdrop."
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {

            plugin.getAirdropManager()
                    .removeActiveDrop();

            player.sendMessage(
                    ChatColor.YELLOW +
                            "✓ Airdrop eliminado."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                        "Uso: /airdrop <edit|spawn|remove>"
        );

        return true;
    }

    /*
     * =========================
     * KIT
     * =========================
     */

    private boolean kitCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/kit <nombre>"
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/kit edit"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("edit")) {

            if (!hasAdmin(player)) {

                player.sendMessage(
                        ChatColor.RED +
                                "No tienes permiso."
                );

                return true;
            }

            plugin.getGuiManager()
                    .openKit(player);

            return true;
        }

        String kit =
                args[0].toLowerCase(Locale.ROOT);

        if (!plugin.getKitManager()
                .kitExists(kit)) {

            player.sendMessage(
                    ChatColor.RED +
                            "Ese kit no existe."
            );

            return true;
        }

        if (!plugin.getKitManager()
                .giveKit(player, kit)) {

            long remaining =
                    plugin.getKitManager()
                            .getRemainingSeconds(
                                    player,
                                    kit
                            );

            player.sendMessage(
                    ChatColor.RED +
                            "No puedes usar este kit todavía."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "Tiempo restante: "
                            + formatTime(remaining)
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Kit recibido."
        );

        return true;
    }

    /*
     * =========================
     * KOTH
     * =========================
     */

    private boolean kothCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.GOLD +
                            "/koth edit"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "/koth start"
            );

            player.sendMessage(
                    ChatColor.GOLD +
                            "/koth stop"
            );

            return true;
        }

        if (!hasAdmin(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("edit")) {

            plugin.getGuiManager()
                    .openKoth(player);

            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {

            if (plugin.getKothManager()
                    .isActive()) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "El KOTH ya está activo."
                );

                return true;
            }

            if (plugin.getKothManager()
                    .getLocation() == null) {

                player.sendMessage(
                        ChatColor.RED +
                                "No hay ubicación configurada."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Usa /koth edit"
                );

                return true;
            }

            plugin.getKothManager()
                    .startKoth();

            if (plugin.getKothManager()
                    .isActive()) {

                player.sendMessage(
                        ChatColor.GREEN +
                                "✓ KOTH iniciado."
                );

            } else {

                player.sendMessage(
                        ChatColor.RED +
                                "No se pudo iniciar el KOTH."
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {

            if (!plugin.getKothManager()
                    .isActive()) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "El KOTH no está activo."
                );

                return true;
            }

            plugin.getKothManager()
                    .stopKoth();

            player.sendMessage(
                    ChatColor.YELLOW +
                            "✓ KOTH detenido."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                        "Uso: /koth <edit|start|stop>"
        );

        return true;
    }

    /*
     * =========================
     * CLAIM
     * =========================
     */

    private boolean claimCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0
                && args[0].equalsIgnoreCase("unclaim")) {

            if (plugin.getClaimManager()
                    .unclaim(player)) {

                player.sendMessage(
                        ChatColor.GREEN +
                                "✓ Chunk desprotegido."
                );

            } else {

                player.sendMessage(
                        ChatColor.RED +
                                "No puedes desproteger este chunk."
                );
            }

            return true;
        }

        if (plugin.getClaimManager()
                .claim(player)) {

            player.sendMessage(
                    ChatColor.GREEN +
                            "✓ Chunk protegido."
            );

        } else {

            player.sendMessage(
                    ChatColor.RED +
                            "No puedes reclamar este chunk."
            );
        }

        return true;
    }

    /*
     * =========================
     * COMBAT
     * =========================
     */

    private boolean combatCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        long remaining =
                plugin.getCombatManager()
                        .getRemainingSeconds(player);

        if (remaining > 0) {

            player.sendMessage(
                    ChatColor.RED +
                            "⚔ Estás en combate: "
                            + remaining
                            + "s"
            );

        } else {

            player.sendMessage(
                    ChatColor.GREEN +
                            "✓ No estás en combate."
            );
        }

        return true;
    }

    /*
     * =========================
     * ECONOMÍA
     * =========================
     */

    private boolean payCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {

            player.sendMessage(
                    ChatColor.YELLOW +
                            "/pay <jugador> <cantidad>"
            );

            return true;
        }

        Player target =
                Bukkit.getPlayer(args[0]);

        if (target == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Jugador no encontrado."
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

        if (!plugin.getEconomyManager()
                .transfer(
                        player,
                        target,
                        amount
                )) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes suficiente dinero."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Enviaste $"
                        + amount
                        + " a "
                        + target.getName()
        );

        target.sendMessage(
                ChatColor.GREEN +
                        "✓ Recibiste $"
                        + amount
                        + " de "
                        + player.getName()
        );

        return true;
    }

    private boolean balanceCommand(
            CommandSender sender
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        double balance =
                plugin.getEconomyManager()
                        .getBalance(player);

        player.sendMessage(
                ChatColor.GREEN +
                        "💰 Balance: $"
                        + balance
        );

        return true;
    }

    /*
     * =========================
     * LIVES
     * =========================
     */

    private boolean livesCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.YELLOW +
                            "❤ Vidas: "
                            + plugin.getDeathbanManager()
                            .getLives(player)
            );

            return true;
        }

        if (!hasAdmin(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        Player target =
                Bukkit.getPlayer(args[0]);

        if (target == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Jugador no encontrado."
            );

            return true;
        }

        int amount = 1;

        if (args.length >= 2) {

            try {

                amount =
                        Integer.parseInt(
                                args[1]
                        );

            } catch (NumberFormatException e) {

                player.sendMessage(
                        ChatColor.RED +
                                "Cantidad inválida."
                );

                return true;
            }
        }

        if (amount <= 0) {

            player.sendMessage(
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

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Agregaste "
                        + amount
                        + " vida(s) a "
                        + target.getName()
        );

        return true;
    }

    /*
     * =========================
     * DEATHBAN
     * =========================
     */

    private boolean deathbanCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            long remaining =
                    plugin.getDeathbanManager()
                            .getRemainingSeconds(
                                    player
                            );

            if (remaining == -1) {

                player.sendMessage(
                        ChatColor.RED +
                                "☠ Deathban permanente."
                );

            } else if (remaining > 0) {

                player.sendMessage(
                        ChatColor.RED +
                                "☠ Deathban: "
                                + formatTime(
                                        remaining
                                )
                );

            } else {

                player.sendMessage(
                        ChatColor.GREEN +
                                "✓ No estás deathbaneado."
                );
            }

            return true;
        }

        if (!hasAdmin(player)) {

            player.sendMessage(
                    ChatColor.RED +
                            "No tienes permiso."
            );

            return true;
        }

        Player target =
                Bukkit.getPlayer(args[0]);

        if (target == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Jugador no encontrado."
            );

            return true;
        }

        if (args.length >= 2
                && args[1].equalsIgnoreCase("revive")) {

            plugin.getDeathbanManager()
                    .revive(target);

            player.sendMessage(
                    ChatColor.GREEN +
                            "✓ Jugador revivido."
            );

            return true;
        }

        plugin.getDeathbanManager()
                .deathban(target);

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Deathban aplicado."
        );

        return true;
    }

    /*
     * =========================
     * CLASES
     * =========================
     */

    private boolean classCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            String current =
                    plugin.getClassManager()
                            .getClass(player);

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Clase actual: "
                            + (current == null
                            ? "ninguna"
                            : current)
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "/class archer"
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "/class bard"
            );

            return true;
        }

        String className =
                args[0].toLowerCase(Locale.ROOT);

        if (!className.equals("archer")
                && !className.equals("bard")) {

            player.sendMessage(
                    ChatColor.RED +
                            "Clase inválida."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "Clases disponibles: archer, bard"
            );

            return true;
        }

        if (!plugin.getClassManager()
                .setClass(
                        player,
                        className
                )) {

            player.sendMessage(
                    ChatColor.RED +
                            "No se pudo seleccionar la clase."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Clase seleccionada: "
                        + className
        );

        return true;
    }

    /*
     * =========================
     * SPAWN
     * =========================
     */

    private boolean spawnCommand(
            CommandSender sender
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        player.teleport(
                player.getWorld()
                        .getSpawnLocation()
        );

        player.sendMessage(
                ChatColor.GREEN +
                        "✓ Teletransportado al spawn."
        );

        return true;
    }

    /*
     * =========================
     * FACTIONS
     * =========================
     */

    private boolean factionCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {

            Faction faction =
                    plugin.getFactionManager()
                            .getFaction(player);

            if (faction == null) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "No perteneces a una faction."
                );

            } else {

                player.sendMessage(
                        ChatColor.GREEN +
                                "Faction: "
                                + faction.getName()
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "DTR: "
                                + faction.getDtr()
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Kills: "
                                + plugin.getFactionManager()
                                .getKills(player)
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Deaths: "
                                + plugin.getFactionManager()
                                .getDeaths(player)
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("create")
                && args.length >= 2) {

            Faction faction =
                    plugin.getFactionManager()
                            .createFaction(
                                    player,
                                    args[1]
                            );

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                                "No se pudo crear la faction."
                );

            } else {

                player.sendMessage(
                        ChatColor.GREEN +
                                "✓ Faction creada: "
                                + faction.getName()
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {

            plugin.getFactionManager()
                    .leaveFaction(player);

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Has salido de la faction."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                        "Subcomando desconocido."
        );

        return true;
    }

    /*
     * =========================
     * UTILIDADES
     * =========================
     */

    private boolean isPlayer(
            CommandSender sender
    ) {

        return sender instanceof Player;
    }

    private boolean hasAdmin(
            Player player
    ) {

        return player != null
                && player.hasPermission(
                "hcf.admin"
        );
    }

    private String formatTime(
            long seconds
    ) {

        if (seconds <= 0) {
            return "0s";
        }

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        long secs =
                seconds % 60;

        if (days > 0) {
            return days + "d "
                    + hours + "h";
        }

        if (hours > 0) {
            return hours + "h "
                    + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m "
                    + secs + "s";
        }

        return secs + "s";
    }
}