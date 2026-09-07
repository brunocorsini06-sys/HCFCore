package com.hcfcore;

import org.bukkit.entity.Player;

public class GUIManager {

    private final HCFCore plugin;

    private final EditorGUI editorGUI;
    private final AirdropEditorGUI airdropEditorGUI;
    private final KitEditorGUI kitEditorGUI;
    private final KothEditorGUI kothEditorGUI;
    private final VillagerEditorGUI villagerEditorGUI;

    public GUIManager(HCFCore plugin) {

        this.plugin = plugin;

        this.editorGUI =
                new EditorGUI(plugin);

        this.airdropEditorGUI =
                new AirdropEditorGUI(plugin);

        this.kitEditorGUI =
                new KitEditorGUI(plugin);

        this.kothEditorGUI =
                new KothEditorGUI(plugin);

        this.villagerEditorGUI =
                new VillagerEditorGUI(plugin);
    }

    public void register() {

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        editorGUI,
                        plugin
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        airdropEditorGUI,
                        plugin
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        kitEditorGUI,
                        plugin
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        kothEditorGUI,
                        plugin
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        villagerEditorGUI,
                        plugin
                );
    }

    public void openMain(Player player) {

        editorGUI.openMain(player);
    }

    public void openAirdrop(Player player) {

        airdropEditorGUI.open(player);
    }

    public void openKit(Player player) {

        kitEditorGUI.open(player);
    }

    public void openKoth(Player player) {

        kothEditorGUI.open(player);
    }

    public void openVillager(Player player) {

        villagerEditorGUI.open(player);
    }

    public EditorGUI getEditorGUI() {

        return editorGUI;
    }

    public AirdropEditorGUI getAirdropEditorGUI() {

        return airdropEditorGUI;
    }

    public KitEditorGUI getKitEditorGUI() {

        return kitEditorGUI;
    }

    public KothEditorGUI getKothEditorGUI() {

        return kothEditorGUI;
    }

    public VillagerEditorGUI getVillagerEditorGUI() {

        return villagerEditorGUI;
    }
}