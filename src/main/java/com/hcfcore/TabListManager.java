package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

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

        if (!plugin.getConfig().getBoolean(
                "tablist.enabled",
                true
        )) {
            return;
        }

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            update(player);
        }
    }

    public void update(Player player) {

        if (player == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "tablist.enabled",
                true
        )) {
            player.setPlayerListHeaderFooter(
                    "",
                    ""
            );

            player.setPlayerListName(
                    player.getName()
            );

            return;
        }

        String header =
                buildLines(
                        "tablist.header",
                        player
                );

        String footer =
                buildLines(
                        "tablist.footer",
                        player
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
                                "%combat% &f%player% &7[%faction%] &8| &a%ping%ms"
                        );

        format =
                replace(
                        player,
                        format
                );

        format =
                format.replace(
                        "%combat%",
                        combat
                );

        format =
                format.replace(
                        "%faction%",
                        factionName
                );

        player.setPlayerListName(
                color(format)
        );
    }

    private String buildLines(
            String path,
            Player player
    ) {

        List<String> lines =
                plugin.getConfig()
                        .getStringList(path);

        if (lines.isEmpty()) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < lines.size();
             i++) {

            String line =
                    lines.get(i);

            line =
                    replace(
                            player,
                            line
                    );

            builder.append(line);

            if (i < lines.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
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