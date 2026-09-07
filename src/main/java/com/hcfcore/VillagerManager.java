package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class VillagerManager {

    private final HCFCore plugin;

    public VillagerManager(HCFCore plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {

        if (!plugin.getConfig()
                .getBoolean("villager.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateVillagers,
                40L,
                200L
        );
    }

    private void updateVillagers() {

        for (World world : Bukkit.getWorlds()) {

            for (Entity entity : world.getEntities()) {

                if (!(entity instanceof Villager)) {
                    continue;
                }

                Villager villager =
                        (Villager) entity;

                if (villager.getProfession()
                        != Villager.Profession.LIBRARIAN) {
                    continue;
                }

                setupTrades(villager);
            }
        }
    }

    private void setupTrades(
            Villager villager
    ) {

        List<String> configured =
                plugin.getConfig()
                        .getStringList(
                                "villager.librarian.trades"
                        );

        if (configured.isEmpty()) {
            return;
        }

        List<MerchantRecipe> recipes =
                new ArrayList<>();

        for (String entry : configured) {

            String[] parts =
                    entry.split(":");

            if (parts.length < 3) {
                continue;
            }

            String enchantmentName =
                    parts[0].trim();

            int level;

            int emeraldCost;

            try {

                level =
                        Integer.parseInt(
                                parts[1].trim()
                        );

                emeraldCost =
                        Integer.parseInt(
                                parts[2].trim()
                        );

            } catch (NumberFormatException ex) {

                continue;
            }

            Enchantment enchantment =
                    Enchantment.getByName(
                            enchantmentName
                    );

            if (enchantment == null) {

                /*
                 * Compatibilidad con nombres
                 * de Bukkit/Paper.
                 */
                enchantment =
                        findEnchantment(
                                enchantmentName
                        );
            }

            if (enchantment == null) {
                continue;
            }

            level =
                    Math.max(
                            1,
                            Math.min(
                                    level,
                                    enchantment.getMaxLevel()
                            )
                    );

            emeraldCost =
                    Math.max(
                            1,
                            emeraldCost
                    );

            ItemStack book =
                    new ItemStack(
                            Material.ENCHANTED_BOOK
                    );

            book.addUnsafeEnchantment(
                    enchantment,
                    level
            );

            MerchantRecipe recipe =
                    new MerchantRecipe(
                            book,
                            999999
                    );

            recipe.setIngredients(
                    List.of(
                            new ItemStack(
                                    Material.EMERALD,
                                    emeraldCost
                            )
                    )
            );

            recipe.setMaxUses(999999);

            recipe.setUses(0);

            recipe.setExperienceReward(false);

            recipes.add(recipe);
        }

        if (!recipes.isEmpty()) {

            villager.setRecipes(
                    recipes
            );
        }
    }

    private Enchantment findEnchantment(
            String name
    ) {

        String normalized =
                name.toLowerCase()
                        .replace(
                                "minecraft:",
                                ""
                        )
                        .replace(
                                "_",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        )
                        .replace(
                                " ",
                                ""
                        );

        for (Enchantment enchantment :
                Enchantment.values()) {

            String key =
                    enchantment.getKey()
                            .getKey()
                            .toLowerCase()
                            .replace(
                                    "_",
                                    ""
                            )
                            .replace(
                                    "-",
                                    ""
                            );

            if (key.equals(normalized)) {
                return enchantment;
            }
        }

        return null;
    }

    public void refresh() {

        if (!plugin.getConfig()
                .getBoolean(
                        "villager.enabled",
                        true
                )) {
            return;
        }

        updateVillagers();
    }
}