package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.Locale;

public class ScoreboardManager {

    private final HCFCore plugin;

    public ScoreboardManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {

        if (!plugin.getConfig().getBoolean(
                "scoreboard.enabled",
                true
        )) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {

        if (player == null) {
            return;
        }

        boolean enabled = plugin.getConfig().getBoolean(
                "scoreboard.enabled",
                true
        );

        if (!enabled) {
            player.setScoreboard(
                    Bukkit.getScoreboardManager()
                            .getMainScoreboard()
            );
            return;
        }

        Scoreboard board =
                Bukkit.getScoreboardManager()
                        .getNewScoreboard();

        String title =
                plugin.getConfig()
                        .getString(
                                "scoreboard.title",
                                "&6&lHCF"
                        );

        Objective objective =
                board.registerNewObjective(
                        "hcf",
                        "dummy",
                        color(title)
                );

        objective.setDisplaySlot(
                DisplaySlot.SIDEBAR
        );

        List<String> lines =
                plugin.getConfig()
                        .getStringList(
                                "scoreboard.lines"
                        );

        int score = lines.size();

        for (String configuredLine : lines) {

            if (configuredLine == null) {
                continue;
            }

            String line = replace(
                    player,
                    configuredLine
            );

            line = color(line);

            /*
             * Evita líneas duplicadas.
             */
            String uniqueLine =
                    makeUnique(board, line);

            objective.getScore(uniqueLine)
                    .setScore(score);

            score--;
        }

        player.setScoreboard(board);
    }

    private String replace(
            Player player,
            String line
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

        double balance =
                plugin.getEconomyManager()
                        .getBalance(player);

        int kills =
                plugin.getFactionManager()
                        .getKills(player);

        int deaths =
                plugin.getFactionManager()
                        .getDeaths(player);

        String claim =
                plugin.getClaimManager()
                        .getClaimName(player);

        if (claim == null || claim.isEmpty()) {
            claim = "Wilderness";
        }

        line = line.replace(
                "%player%",
                player.getName()
        );

        line = line.replace(
                "%faction%",
                factionName
        );

        line = line.replace(
                "%dtr%",
                String.format(
                        Locale.US,
                        "%.2f",
                        dtr
                )
        );

        line = line.replace(
                "%balance%",
                String.format(
                        Locale.US,
                        "%.2f",
                        balance
                )
        );

        line = line.replace(
                "%kills%",
                String.valueOf(kills)
        );

        line = line.replace(
                "%deaths%",
                String.valueOf(deaths)
        );

        line = line.replace(
                "%claim%",
                claim
        );

        return line;
    }

    private String makeUnique(
            Scoreboard board,
            String line
    ) {

        String result = line;

        while (board.getEntries().contains(result)) {
            result = result + " ";
        }

        return result;
    }

    private String color(String text) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}