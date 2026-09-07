package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClassManager {

    private final HCFCore plugin;

    private final Map<UUID, String> classes =
            new HashMap<>();

    public ClassManager(HCFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Selecciona una clase.
     */
    public boolean setClass(
            Player player,
            String className
    ) {

        if (player == null || className == null) {
            return false;
        }

        className = className.toLowerCase();

        if (!className.equals("archer")
                && !className.equals("bard")) {

            return false;
        }

        classes.put(
                player.getUniqueId(),
                className
        );

        applyEffects(player);

        return true;
    }

    /**
     * Devuelve la clase actual.
     */
    public String getClass(Player player) {

        if (player == null) {
            return null;
        }

        return classes.get(
                player.getUniqueId()
        );
    }

    /**
     * Elimina la clase.
     */
    public void removeClass(Player player) {

        if (player == null) {
            return;
        }

        classes.remove(
                player.getUniqueId()
        );

        removeEffects(player);
    }

    /**
     * Aplica los efectos correspondientes.
     */
    public void applyEffects(Player player) {

        if (player == null) {
            return;
        }

        removeEffects(player);

        String className =
                getClass(player);

        if (className == null) {
            return;
        }

        if (className.equals("archer")) {

            int speed =
                    plugin.getConfig()
                            .getInt(
                                    "classes.archer.speed-level",
                                    1
                            );

            if (speed > 0) {

                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SPEED,
                                Integer.MAX_VALUE,
                                speed - 1,
                                false,
                                false,
                                true
                        )
                );
            }
        }

        if (className.equals("bard")) {

            int speed =
                    plugin.getConfig()
                            .getInt(
                                    "classes.bard.speed-level",
                                    0
                            );

            if (speed > 0) {

                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SPEED,
                                Integer.MAX_VALUE,
                                speed - 1,
                                false,
                                false,
                                true
                        )
                );
            }
        }
    }

    /**
     * Elimina los efectos de clase.
     */
    private void removeEffects(Player player) {

        if (player == null) {
            return;
        }

        player.removePotionEffect(
                PotionEffectType.SPEED
        );
    }

    /**
     * Efectos del Bard para jugadores cercanos.
     */
    public void applyBardEffects(Player bard) {

        if (bard == null) {
            return;
        }

        if (!"bard".equalsIgnoreCase(
                getClass(bard)
        )) {
            return;
        }

        int radius =
                plugin.getConfig()
                        .getInt(
                                "classes.bard.radius",
                                12
                        );

        int strength =
                plugin.getConfig()
                        .getInt(
                                "classes.bard.strength-level",
                                0
                        );

        int resistance =
                plugin.getConfig()
                        .getInt(
                                "classes.bard.resistance-level",
                                0
                        );

        for (Player target :
                Bukkit.getOnlinePlayers()) {

            if (!target.getWorld().equals(
                    bard.getWorld()
            )) {
                continue;
            }

            if (target.getLocation()
                    .distanceSquared(
                            bard.getLocation()
                    )
                    > radius * radius) {

                continue;
            }

            /*
             * En Paper 1.20.4:
             * STRENGTH -> INCREASE_DAMAGE
             */
            if (strength > 0) {

                PotionEffectType strengthType =
                        PotionEffectType.getByName(
                                "INCREASE_DAMAGE"
                        );

                if (strengthType != null) {

                    target.addPotionEffect(
                            new PotionEffect(
                                    strengthType,
                                    40,
                                    strength - 1,
                                    false,
                                    false,
                                    true
                            )
                    );
                }
            }

            /*
             * En Paper 1.20.4:
             * RESISTANCE -> DAMAGE_RESISTANCE
             */
            if (resistance > 0) {

                PotionEffectType resistanceType =
                        PotionEffectType.getByName(
                                "DAMAGE_RESISTANCE"
                        );

                if (resistanceType != null) {

                    target.addPotionEffect(
                            new PotionEffect(
                                    resistanceType,
                                    40,
                                    resistance - 1,
                                    false,
                                    false,
                                    true
                            )
                    );
                }
            }
        }
    }

    /**
     * Ejecuta los efectos periódicamente.
     */
    public void startTask() {

        plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        () -> {

                            for (Player player :
                                    Bukkit.getOnlinePlayers()) {

                                applyBardEffects(player);
                            }

                        },
                        20L,
                        20L
                );
    }

    public Map<UUID, String> getClasses() {
        return classes;
    }
}