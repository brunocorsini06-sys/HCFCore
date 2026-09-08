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

            case "staff":
                return staff(sender);

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
                    ChatColor.WHITE + "/hcf editor"
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

            plugin.getGuiManager().openMain(player);

            return true;
        }

        sender.sendMessage(
                ChatColor.RED +
                "Uso: /hcf <reload|editor>"
        );

        return true;
    }

    // =========================================================
    // /STAFF
    // =========================================================

    private boolean staff(
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

        /*
         * Verificación mediante LuckPerms.
         */

        if (plugin.getLuckPermsHook() == null
                || !plugin.getLuckPermsHook()
                .hasPermission(
                        player,
                        "hcf.staff"
                )) {

            player.sendMessage(
                    ChatColor.RED +
                    "No tienes permiso para usar Staff Mode."
            );

            return true;
        }

        StaffManager staffManager =
                plugin.getStaffManager();

        if (staffManager == null) {

            player.sendMessage(
                    ChatColor.RED +
                    "El sistema de Staff Mode no está disponible."
            );

            return true;
        }

        /*
         * Si ya está en Staff Mode,
         * lo desactivamos.
         */

        if (staffManager.isStaff(player)) {

            if (!staffManager.disable(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo desactivar Staff Mode."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "✘ Staff Mode desactivado."
            );

            return true;
        }

        /*
         * Activar Staff Mode.
         */

        if (!staffManager.enable(player)) {

            player.sendMessage(
                    ChatColor.RED +
                    "No se pudo activar Staff Mode."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                "✔ Staff Mode activado."
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Usa " +
                ChatColor.WHITE +
                "/staff" +
                ChatColor.GRAY +
                " nuevamente para desactivarlo."
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

        FactionManager manager =
                plugin.getFactionManager();

        Faction faction =
                manager.getFaction(player);

        if (args.length == 0) {

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

            sendFactionInfo(player, faction);
            return true;
        }

        String subCommand =
                args[0].toLowerCase(Locale.ROOT);

        if (subCommand.equals("create")) {

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f create <nombre>"
                );

                return true;
            }

            if (faction != null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ya perteneces a una faction."
                );

                return true;
            }

            String name = args[1];

            if (!manager.isValidFactionName(name)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Nombre inválido."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Debe tener entre 3 y 16 caracteres y solo usar letras, números y _."
                );

                return true;
            }

            if (manager.getFaction(name) != null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ya existe una faction con ese nombre."
                );

                return true;
            }

            Faction created =
                    manager.createFaction(player, name);

            if (created == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo crear la faction."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "¡Faction creada correctamente!"
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Nombre: " +
                    ChatColor.WHITE +
                    created.getName()
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "DTR inicial: " +
                    ChatColor.WHITE +
                    created.getDtr()
            );

            return true;
        }

        if (subCommand.equals("disband")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder puede disbandear la faction."
                );

                return true;
            }

            String factionName =
                    faction.getName();

            if (!manager.disband(faction)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo disbandear la faction."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "La faction " +
                    ChatColor.YELLOW +
                    factionName +
                    ChatColor.GREEN +
                    " fue disbanded."
            );

            return true;
        }

        if (subCommand.equals("invite")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.canManageFaction(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder u officer puede invitar jugadores."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f invite <jugador>"
                );

                return true;
            }

            Player target =
                    Bukkit.getPlayerExact(args[1]);

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
                        "No puedes invitarte a ti mismo."
                );

                return true;
            }

            if (manager.getFaction(target) != null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador ya pertenece a una faction."
                );

                return true;
            }

            if (!manager.invite(target, faction)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo enviar la invitación."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Invitaste a " +
                    ChatColor.YELLOW +
                    target.getName() +
                    ChatColor.GREEN +
                    " a tu faction."
            );

            target.sendMessage(
                    ChatColor.GREEN +
                    "Has sido invitado a la faction " +
                    ChatColor.YELLOW +
                    faction.getName()
            );

            target.sendMessage(
                    ChatColor.GRAY +
                    "Usa /f join " +
                    faction.getName() +
                    " para aceptar."
            );

            return true;
        }

        if (subCommand.equals("join")) {

            if (faction != null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ya perteneces a una faction."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f join <faction>"
                );

                return true;
            }

            String factionName = args[1];

            Faction targetFaction =
                    manager.getFaction(factionName);

            if (targetFaction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Esa faction no existe."
                );

                return true;
            }

            if (!manager.hasInvite(
                    player,
                    targetFaction.getName()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No tienes una invitación de esa faction."
                );

                return true;
            }

            if (!manager.joinFaction(
                    player,
                    targetFaction.getName()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes unirte a esa faction."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Te uniste a " +
                    ChatColor.YELLOW +
                    targetFaction.getName() +
                    ChatColor.GREEN +
                    "."
            );

            return true;
        }

        if (subCommand.equals("leave")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "El líder no puede abandonar la faction."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Usa /f disband."
                );

                return true;
            }

            if (!manager.leaveFaction(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No pudiste abandonar la faction."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Has abandonado la faction."
            );

            return true;
        }

        if (subCommand.equals("kick")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.canManageFaction(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder u officer puede expulsar jugadores."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f kick <jugador>"
                );

                return true;
            }

            Player target =
                    Bukkit.getPlayerExact(args[1]);

            if (target == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no está conectado."
                );

                return true;
            }

            Faction targetFaction =
                    manager.getFaction(target);

            if (targetFaction == null ||
                    !targetFaction.getName()
                            .equalsIgnoreCase(
                                    faction.getName()
                            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no pertenece a tu faction."
                );

                return true;
            }

            if (target.equals(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes expulsarte a ti mismo."
                );

                return true;
            }

            if (targetFaction.isLeader(
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes expulsar al líder."
                );

                return true;
            }

            if (manager.isOfficer(player)
                    && targetFaction.isOfficer(
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Un officer no puede expulsar a otro officer."
                );

                return true;
            }

            if (!manager.kick(
                    faction,
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo expulsar al jugador."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Expulsaste a " +
                    ChatColor.YELLOW +
                    target.getName() +
                    ChatColor.GREEN +
                    " de la faction."
            );

            target.sendMessage(
                    ChatColor.RED +
                    "Has sido expulsado de " +
                    ChatColor.YELLOW +
                    faction.getName() +
                    ChatColor.RED +
                    "."
            );

            return true;
        }

        if (subCommand.equals("promote")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder puede promover officers."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f promote <jugador>"
                );

                return true;
            }

            Player target =
                    Bukkit.getPlayerExact(args[1]);

            if (target == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no está conectado."
                );

                return true;
            }

            if (!faction.isMember(
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no pertenece a tu faction."
                );

                return true;
            }

            if (!manager.promote(
                    faction,
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo promover a ese jugador."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    target.getName() +
                    " ahora es Officer."
            );

            target.sendMessage(
                    ChatColor.GREEN +
                    "Has sido promovido a Officer."
            );

            return true;
        }

        if (subCommand.equals("demote")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder puede quitar el rango Officer."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f demote <jugador>"
                );

                return true;
            }

            Player target =
                    Bukkit.getPlayerExact(args[1]);

            if (target == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no está conectado."
                );

                return true;
            }

            if (!faction.isMember(
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no pertenece a tu faction."
                );

                return true;
            }

            if (!faction.isOfficer(
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ese jugador no es Officer."
                );

                return true;
            }

            if (!manager.demote(
                    faction,
                    target.getUniqueId()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo quitar el rango."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    target.getName() +
                    " ya no es Officer."
            );

            target.sendMessage(
                    ChatColor.YELLOW +
                    "Tu rango de Officer fue retirado."
            );

            return true;
        }

        if (subCommand.equals("sethome")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.canManageFaction(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder u officer puede establecer el home."
                );

                return true;
            }

            if (!manager.setHome(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo establecer el faction home."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Faction home establecido correctamente."
            );

            return true;
        }

        if (subCommand.equals("home")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            Location home =
                    manager.getHome(player);

            if (home == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Tu faction todavía no tiene un home."
                );

                return true;
            }

            player.teleport(home);

            player.sendMessage(
                    ChatColor.GREEN +
                    "Has sido teletransportado al faction home."
            );

            return true;
        }

        if (subCommand.equals("ally")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder puede gestionar alianzas."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f ally <faction>"
                );

                return true;
            }

            Faction target =
                    manager.getFaction(args[1]);

            if (target == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Esa faction no existe."
                );

                return true;
            }

            if (target == faction) {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes aliarte contigo mismo."
                );

                return true;
            }

            if (faction.isAlly(
                    target.getName()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ya son aliados."
                );

                return true;
            }

            if (!manager.addAlly(
                    faction,
                    target
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo crear la alianza."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                    "Ahora son aliados de " +
                    ChatColor.YELLOW +
                    target.getName() +
                    ChatColor.GREEN +
                    "."
            );

            return true;
        }

        if (subCommand.equals("enemy")) {

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No perteneces a ninguna faction."
                );

                return true;
            }

            if (!manager.isLeader(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "Solo el líder puede gestionar enemigos."
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.RED +
                        "Uso: /f enemy <faction>"
                );

                return true;
            }

            Faction target =
                    manager.getFaction(args[1]);

            if (target == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Esa faction no existe."
                );

                return true;
            }

            if (target == faction) {

                player.sendMessage(
                        ChatColor.RED +
                        "No puedes declararte enemigo de ti mismo."
                );

                return true;
            }

            if (faction.isEnemy(
                    target.getName()
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "Ya son enemigos."
                );

                return true;
            }

            if (!manager.addEnemy(
                    faction,
                    target
            )) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se pudo declarar la faction como enemiga."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "Ahora son enemigos de " +
                    ChatColor.YELLOW +
                    target.getName() +
                    ChatColor.RED +
                    "."
            );

            return true;
        }

        if (subCommand.equals("who")) {

            Faction targetFaction;

            if (args.length >= 2) {

                targetFaction =
                        manager.getFaction(args[1]);

            } else {

                targetFaction = faction;
            }

            if (targetFaction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "No se encontró esa faction."
                );

                return true;
            }

            sendFactionInfo(
                    player,
                    targetFaction
            );

            return true;
        }

        sendFactionHelp(player);

        return true;
    }

    // =========================================================
    // INFORMACIÓN DE FACTION
    // =========================================================

    private void sendFactionInfo(
            Player player,
            Faction faction
    ) {

        player.sendMessage(
                ChatColor.GOLD +
                "━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Faction: " +
                ChatColor.WHITE +
                faction.getName()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Líder: " +
                ChatColor.WHITE +
                getPlayerName(faction.getLeader())
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "DTR: " +
                ChatColor.RED +
                String.format(
                        Locale.US,
                        "%.2f",
                        faction.getDtr()
                )
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Balance: " +
                ChatColor.GREEN +
                "$" +
                formatMoney(faction.getBalance())
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Miembros: " +
                ChatColor.WHITE +
                faction.getMembers().size()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Officers: " +
                ChatColor.WHITE +
                faction.getOfficers().size()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Aliados: " +
                ChatColor.WHITE +
                faction.getAllies().size()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Enemigos: " +
                ChatColor.WHITE +
                faction.getEnemies().size()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Claims: " +
                ChatColor.WHITE +
                plugin.getClaimManager()
                        .getClaimCount(faction)
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.YELLOW +
                "Miembros:"
        );

        for (java.util.UUID uuid :
                faction.getMembers()) {

            String rank;

            if (faction.isLeader(uuid)) {
                rank = ChatColor.GOLD + "Líder";
            } else if (faction.isOfficer(uuid)) {
                rank = ChatColor.AQUA + "Officer";
            } else {
                rank = ChatColor.GRAY + "Miembro";
            }

            player.sendMessage(
                    ChatColor.WHITE +
                    "- " +
                    getPlayerName(uuid) +
                    ChatColor.GRAY +
                    " [" +
                    rank +
                    ChatColor.GRAY +
                    "]"
            );
        }

        player.sendMessage(
                ChatColor.GOLD +
                "━━━━━━━━━━━━━━━━━━━━━━━━"
        );
    }

    // =========================================================
    // AYUDA FACTIONS
    // =========================================================

    private void sendFactionHelp(
            Player player
    ) {

        player.sendMessage(
                ChatColor.GOLD +
                "━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Faction Commands"
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.WHITE +
                "/f create <nombre>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f who [faction]"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f invite <jugador>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f join <faction>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f leave"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f kick <jugador>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f promote <jugador>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f demote <jugador>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f sethome"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f home"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f ally <faction>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f enemy <faction>"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "/f disband"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "━━━━━━━━━━━━━━━━━━━━━━━━"
        );
    }

    private String getPlayerName(
            java.util.UUID uuid
    ) {

        if (uuid == null) {
            return "Desconocido";
        }

        Player player =
                Bukkit.getPlayer(uuid);

        if (player != null) {
            return player.getName();
        }

        return uuid.toString().substring(0, 8);
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

            if (plugin.getClaimManager()
                    .claim(player)) {

                player.sendMessage(
                        ChatColor.GREEN +
                        "Chunk reclamado correctamente."
                );

                return true;
            }

            Faction faction =
                    plugin.getFactionManager()
                            .getFaction(player);

            if (faction == null) {

                player.sendMessage(
                        ChatColor.RED +
                        "Necesitas pertenecer a una faction."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "No puedes reclamar este chunk."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Puede que ya esté reclamado, hayas alcanzado el límite o no sea adyacente."
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

            amount =
                    Double.parseDouble(args[1]);

        } catch (NumberFormatException e) {

            player.sendMessage(
                    ChatColor.RED +
                    "La cantidad no es válida."
            );

            return true;
        }

        if (!Double.isFinite(amount)
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
                .canUseKit(
                        player,
                        kitName
                )) {

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
                .giveKit(
                        player,
                        kitName
                )) {

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
                    (
                            current == null
                                    ? "Ninguna"
                                    : current
                    )
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
                .setClass(
                        player,
                        className
                )) {

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
                .addLife(
                        target,
                        amount
                );

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

    private String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }
} 