package com.hcfcore;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ProtocolLibHook {

    private final HCFCore plugin;
    private ProtocolManager protocolManager;

    public ProtocolLibHook(HCFCore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {

        protocolManager =
                ProtocolLibrary.getProtocolManager();

        if (protocolManager == null) {

            plugin.getLogger().severe(
                    "No se pudo obtener el ProtocolManager de ProtocolLib."
            );

            throw new IllegalStateException(
                    "ProtocolLib no está disponible."
            );
        }

        plugin.getLogger().info(
                "ProtocolLib conectado correctamente."
        );
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public boolean isConnected() {
        return protocolManager != null;
    }

    public HCFCore getPlugin() {
        return plugin;
    }
}