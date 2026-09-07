package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TabListManager {

    private final HCFCore plugin;

    public TabListManager(HCFCore plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {

        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateAll,
                20L,
                20L
        );
    }

    public void updateAll() {

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            update(player);
        }
    }

    public void update(Player player) {

        if (player == null) {
            return;
        }

        String header =
                plugin.getConfig()
                        .getString(
                                "tablist.header",
                                "&6&lHCFCore"
                        );

        String footer =
                plugin.getConfig()
                        .getString(
                                "tablist.footer",
                                "&7Online: &f%online%"
                        );

        header =
                replace(
                        player,
                        header
                );

        footer =
                replace(
                        player,
                        footer
                );

        player.setPlayerListHeaderFooter(
                color(header),
                color(footer)
        );

        updatePlayerName(player);
    }

    private void updatePlayerName(
            Player player
    ) {

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        String factionName =
                faction == null
                        ? "Ninguna"
                        : faction.getName();

        String combat =
                plugin.getCombatManager()
                        .isInCombat(player)
                        ? "&c⚔"
                        : "&a✓";

        String format =
                plugin.getConfig()
                        .getString(
                                "tablist.player-format",
                                "%combat% &f%player% &7[%faction%]"
                        );

        format =
                format.replace(
                        "%combat%",
                        combat
                );

        format =
                format.replace(
                        "%player%",
                        player.getName()
                );

        format =
                format.replace(
                        "%faction%",
                        factionName
                );

        format =
                format.replace(
                        "%ping%",
                        String.valueOf(
                                getPing(player)
                        )
                );

        player.setPlayerListName(
                color(format)
        );
    }

    private String replace(
            Player player,
            String text
    ) {

        Faction faction =
                plugin.getFactionManager()
                        .getFaction(player);

        String factionName =
                faction == null
                        ? "Ninguna"
                        : faction.getName();

        double dtr =
                faction == null
                        ? 0.0
                        : faction.getDtr();

        text =
                text.replace(
                        "%player%",
                        player.getName()
                );

        text =
                text.replace(
                        "%faction%",
                        factionName
                );

        text =
                text.replace(
                        "%dtr%",
                        String.format(
                                "%.2f",
                                dtr
                        )
                );

        text =
                text.replace(
                        "%ping%",
                        String.valueOf(
                                getPing(player)
                        )
                );

        text =
                text.replace(
                        "%online%",
                        String.valueOf(
                                Bukkit.getOnlinePlayers()
                                        .size()
                        )
                );

        text =
                text.replace(
                        "%combat%",
                        plugin.getCombatManager()
                                .isInCombat(player)
                                ? "⚔ COMBATE"
                                : "✓ LIBRE"
                );

        return text;
    }

    private int getPing(
            Player player
    ) {

        try {

            return player.getPing();

        } catch (Exception ignored) {

            return 0;
        }
    }

    private String color(
            String text
    ) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}