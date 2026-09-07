package com.hcfcore;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LocationStorage {

    private static Location kothLocation;

    public static void setLocation(Player player) {
        if (player == null) {
            return;
        }

        kothLocation = player.getLocation().clone();
    }

    public static Location getLocation() {
        if (kothLocation == null) {
            return null;
        }

        return kothLocation.clone();
    }

    public static boolean hasLocation() {
        return kothLocation != null;
    }

    public static void clear() {
        kothLocation = null;
    }
}