package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class HCFCore extends JavaPlugin {

    private static HCFCore instance;

    private FactionManager factionManager;
    private ClaimManager claimManager;
    private CombatManager combatManager;
    private DeathbanManager deathbanManager;
    private EconomyManager economyManager;
    private KitManager kitManager;
    private ClassManager classManager;
    private AirdropManager airdropManager;
    private KothManager kothManager;
    private ScoreboardManager scoreboardManager;
    private VillagerManager villagerManager;
    private TabListManager tabListManager;
    private GUIManager guiManager;

    /*
     * Tareas principales de la Core.
     */
    private BukkitTask scoreboardTask;
    private BukkitTask dtrTask;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        /*
         * Managers.
         */
        factionManager = new FactionManager(this);
        claimManager = new ClaimManager(this);
        combatManager = new CombatManager(this);
        deathbanManager = new DeathbanManager(this);
        economyManager = new EconomyManager(this);
        kitManager = new KitManager(this);
        classManager = new ClassManager(this);
        airdropManager = new AirdropManager(this);
        kothManager = new KothManager(this);
        scoreboardManager = new ScoreboardManager(this);
        villagerManager = new VillagerManager(this);
        tabListManager = new TabListManager(this);

        /*
         * WorldBorder.
         */
        setupWorldBorder();

        /*
         * Listener principal.
         */
        getServer()
                .getPluginManager()
                .registerEvents(
                        new HCFListener(this),
                        this
                );

        /*
         * GUIs.
         */
        guiManager = new GUIManager(this);
        guiManager.register();

        /*
         * Comandos.
         */
        Commands commands = new Commands(this);

        registerCommand("f", commands);
        registerCommand("hcf", commands);
        registerCommand("claim", commands);
        registerCommand("combat", commands);
        registerCommand("pay", commands);
        registerCommand("kit", commands);
        registerCommand("class", commands);
        registerCommand("spawn", commands);
        registerCommand("koth", commands);
        registerCommand("airdrop", commands);
        registerCommand("lives", commands);
        registerCommand("deathban", commands);
        registerCommand("balance", commands);

        /*
         * SCOREBOARD
         *
         * Actualización cada segundo.
         */
        scoreboardTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                this,
                                () -> scoreboardManager.updateAll(),
                                20L,
                                20L
                        );

        /*
         * DTR
         *
         * Actualización cada segundo.
         */
        dtrTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                this,
                                () -> factionManager.regenerateDtr(),
                                20L,
                                20L
                        );

        /*
         * CLASES
         */
        classManager.startTask();

        /*
         * COMBAT TAG
         */
        combatManager.startCleanupTask();

        /*
         * AIRDROPS
         */
        if (getConfig().getBoolean(
                "airdrop.enabled",
                true
        )) {

            airdropManager.startScheduler();
        }

        /*
         * Mensaje de inicio.
         */
        getLogger().info(
                "================================="
        );

        getLogger().info(
                "        HCFCore ACTIVADO"
        );

        getLogger().info(
                "        Version 2.0.0"
        );

        getLogger().info(
                "        Paper 1.20.4"
        );

        getLogger().info(
                "        EditGUI ACTIVADO"
        );

        getLogger().info(
                "        TabList ACTIVADO"
        );

        getLogger().info(
                "        Scoreboard ACTIVADO"
        );

        getLogger().info(
                "        Airdrops ACTIVADOS"
        );

        getLogger().info(
                "================================="
        );
    }

    /**
     * Registra un comando definido
     * en plugin.yml.
     */
    private void registerCommand(
            String name,
            Commands commands
    ) {

        if (getCommand(name) == null) {

            getLogger().warning(
                    "Comando no encontrado en plugin.yml: "
                            + name
            );

            return;
        }

        getCommand(name)
                .setExecutor(commands);
    }

    /**
     * Configura el WorldBorder.
     */
    private void setupWorldBorder() {

        String worldName =
                getConfig().getString(
                        "world.name",
                        "world"
                );

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {

            getLogger().warning(
                    "No se encontró el mundo: "
                            + worldName
            );

            return;
        }

        WorldBorder border =
                world.getWorldBorder();

        double centerX =
                getConfig().getDouble(
                        "world.center-x",
                        0
                );

        double centerZ =
                getConfig().getDouble(
                        "world.center-z",
                        0
                );

        double size =
                getConfig().getDouble(
                        "world.border-size",
                        5000
                );

        if (size <= 0) {
            size = 5000;
        }

        border.setCenter(
                centerX,
                centerZ
        );

        border.setSize(size);

        getLogger().info(
                "WorldBorder configurada: "
                        + size
                        + "x"
                        + size
                        + " | Centro: "
                        + centerX
                        + ", "
                        + centerZ
        );
    }

    @Override
    public void onDisable() {

        /*
         * Cancelar tareas propias.
         */
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
        }

        if (dtrTask != null) {
            dtrTask.cancel();
        }

        /*
         * Detener Airdrops.
         */
        if (airdropManager != null) {
            airdropManager.shutdown();
        }

        /*
         * Guardar datos.
         */
        if (factionManager != null) {
            factionManager.saveAll();
        }

        if (economyManager != null) {
            economyManager.saveAll();
        }

        if (claimManager != null) {
            claimManager.saveAll();
        }

        instance = null;

        getLogger().info(
                "HCFCore desactivada correctamente."
        );
    }

    public static HCFCore getInstance() {
        return instance;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public DeathbanManager getDeathbanManager() {
        return deathbanManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public AirdropManager getAirdropManager() {
        return airdropManager;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public VillagerManager getVillagerManager() {
        return villagerManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}