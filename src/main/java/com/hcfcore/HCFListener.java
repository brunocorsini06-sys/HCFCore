package com.hcfcore;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HCFListener implements Listener {

    private final HCFCore plugin;

    public HCFListener(HCFCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        /*
         * Actualizar scoreboard al entrar.
         */
        plugin.getScoreboardManager()
                .update(player);

        /*
         * Agregar jugador al BossBar del KOTH
         * si el evento está activo.
         */
        if (plugin.getKothManager().isActive()) {

            if (plugin.getKothManager().getBossBar() != null) {

                plugin.getKothManager()
                        .getBossBar()
                        .addPlayer(player);
            }
        }

        /*
         * Comprobar Deathban.
         */
        long deathban =
                plugin.getDeathbanManager()
                        .getRemainingSeconds(player);

        if (deathban == -1) {

            player.sendMessage(
                    ChatColor.RED +
                            "☠ Estás permanentemente deathbaneado."
            );

        } else if (deathban > 0) {

            player.sendMessage(
                    ChatColor.RED +
                            "☠ Estás deathbaneado por "
                            + formatTime(deathban)
            );
        }

        /*
         * Mensaje de bienvenida.
         */
        player.sendMessage(
                ChatColor.GOLD +
                        "━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "      Bienvenido a HCF"
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Usa /f para gestionar tu faction."
        );

        player.sendMessage(
                ChatColor.GOLD +
                        "━━━━━━━━━━━━━━━━━━━━"
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player =
                event.getPlayer();

        /*
         * CombatTag.
         */
        if (plugin.getCombatManager()
                .isInCombat(player)) {

            if (plugin.getConfig()
                    .getBoolean(
                            "combat.punish-logout",
                            true
                    )) {

                plugin.getCombatManager()
                        .handleQuit(player);
            }
        }

        /*
         * Quitar del BossBar del KOTH.
         */
        if (plugin.getKothManager()
                .getBossBar() != null) {

            plugin.getKothManager()
                    .getBossBar()
                    .removePlayer(player);
        }
    }

    @EventHandler
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getEntity()
                instanceof Player)) {

            return;
        }

        Player victim =
                (Player) event.getEntity();

        Player attacker = null;

        Entity damager =
                event.getDamager();

        if (damager instanceof Player) {

            attacker =
                    (Player) damager;
        }

        /*
         * CombatTag.
         */
        if (attacker != null) {

           plugin.getCombatManager()
                   .tag(attacker);

           plugin.getCombatManager()
                    .tag(victim);
        }

        /*
         * Friendly Fire.
         */
        if (attacker != null) {

            Faction attackerFaction =
                    plugin.getFactionManager()
                            .getFaction(attacker);

            Faction victimFaction =
                    plugin.getFactionManager()
                            .getFaction(victim);

            if (attackerFaction != null
                    && victimFaction != null
                    && attackerFaction
                    .getName()
                    .equalsIgnoreCase(
                            victimFaction.getName()
                    )) {

                boolean friendlyFire =
                        plugin.getConfig()
                                .getBoolean(
                                        "factions.friendly-fire",
                                        false
                                );

                if (!friendlyFire) {

                    event.setCancelled(true);

                    attacker.sendMessage(
                            ChatColor.RED +
                                    "No puedes atacar a "
                                    + "un miembro de tu faction."
                    );
                }
            }
        }
    }

    @EventHandler
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getEntity();

        /*
         * Registrar muerte.
         */
        plugin.getFactionManager()
                .addDeath(player);

        /*
         * Quitar vida.
         */
        if (plugin.getConfig()
                .getBoolean(
                        "deathban.remove-life",
                        true
                )) {

            plugin.getDeathbanManager()
                    .removeLife(player);
        }

        /*
         * Aplicar Deathban.
         */
        if (plugin.getConfig()
                .getBoolean(
                        "deathban.enabled",
                        true
                )) {

            plugin.getDeathbanManager()
                    .deathban(player);
        }

        /*
         * Registrar kill del atacante.
         */
        Player killer =
                player.getKiller();

        if (killer != null) {

            plugin.getFactionManager()
                    .addKill(killer);

            killer.sendMessage(
                    ChatColor.GREEN +
                            "⚔ ¡Has matado a "
                            + player.getName()
                            + "!"
            );
        }
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

        seconds %= 60;

        if (days > 0) {

            return days
                    + "d "
                    + hours
                    + "h";
        }

        if (hours > 0) {

            return hours
                    + "h "
                    + minutes
                    + "m";
        }

        if (minutes > 0) {

            return minutes
                    + "m "
                    + seconds
                    + "s";
        }

        return seconds + "s";
    }
}