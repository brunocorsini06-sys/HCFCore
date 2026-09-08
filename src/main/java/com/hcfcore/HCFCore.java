package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class HCFCore extends JavaPlugin {

    private static HCFCore instance;

    private DatabaseManager databaseManager;

    /*
     * ==============================
     * EXTERNAL HOOKS
     * ==============================
     */

    private LuckPermsHook luckPermsHook;
    private VaultHook vaultHook;
    private ProtocolLibHook protocolLibHook;

    /*
     * ==============================
     * MANAGERS
     * ==============================
     */

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

    private BukkitTask scoreboardTask;
    private BukkitTask dtrTask;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        getLogger().info("Iniciando HCFCore...");

        /*
         * ==============================
         * EXTERNAL DEPENDENCIES
         * ==============================
         */

        try {

            /*
             * LuckPerms
             */

            luckPermsHook = new LuckPermsHook(this);

            /*
             * Vault
             */

            vaultHook = new VaultHook(this);

            /*
             * ProtocolLib
             */

            protocolLibHook = new ProtocolLibHook(this);

        } catch (Exception exception) {

            getLogger().severe(
                    "No se pudieron inicializar las dependencias externas."
            );

            exception.printStackTrace();

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }

        /*
         * ==============================
         * DATABASE
         * ==============================
         */

        databaseManager = new DatabaseManager(this);

        if (!databaseManager.connect()) {

            getLogger().severe(
                    "No se pudo iniciar la base de datos."
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }

        /*
         * ==============================
         * MANAGERS
         * ==============================
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
         * ==============================
         * WORLD BORDER
         * ==============================
         */

        setupWorldBorder();

        /*
         * ==============================
         * LISTENERS
         * ==============================
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new HCFListener(this),
                        this
                );

        /*
         * ==============================
         * GUIS
         * ==============================
         */

        guiManager = new GUIManager(this);
        guiManager.register();

        /*
         * ==============================
         * COMMANDS
         * ==============================
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
         * ==============================
         * SCOREBOARD
         * ==============================
         */

        if (getConfig().getBoolean(
                "scoreboard.enabled",
                true
        )) {

            scoreboardTask =
                    Bukkit.getScheduler()
                            .runTaskTimer(
                                    this,
                                    () -> {

                                        if (scoreboardManager != null) {
                                            scoreboardManager.updateAll();
                                        }

                                    },
                                    20L,
                                    20L
                            );

            getLogger().info(
                    "Scoreboard activado."
            );
        }

        /*
         * ==============================
         * DTR
         * ==============================
         */

        dtrTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                this,
                                () -> {

                                    if (factionManager != null) {
                                        factionManager.regenerateDtr();
                                    }

                                },
                                20L,
                                20L
                        );

        /*
         * ==============================
         * CLASSES
         * ==============================
         */

        if (classManager != null) {
            classManager.startTask();
        }

        /*
         * ==============================
         * COMBAT TAG
         * ==============================
         */

        if (combatManager != null) {
            combatManager.startCleanupTask();
        }

        /*
         * ==============================
         * AIRDROPS
         * ==============================
         */

        if (getConfig().getBoolean(
                "airdrop.enabled",
                true
        )) {

            if (airdropManager != null) {
                airdropManager.startScheduler();
            }

            getLogger().info(
                    "Airdrops activados."
            );
        }

        /*
         * ==============================
         * VILLAGERS
         * ==============================
         */

        if (getConfig().getBoolean(
                "villager.enabled",
                true
        )) {

            getLogger().info(
                    "Sistema de aldeanos activado."
            );
        }

        /*
         * ==============================
         * TABLIST
         * ==============================
         */

        if (getConfig().getBoolean(
                "tablist.enabled",
                true
        )) {

            getLogger().info(
                    "TabList activado."
            );
        }

        /*
         * ==============================
         * STARTUP MESSAGE
         * ==============================
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
                "        Java 17"
        );

        getLogger().info(
                "        SQLite ACTIVADO"
        );

        getLogger().info(
                "        LuckPerms CONECTADO"
        );

        getLogger().info(
                "        Vault CONECTADO"
        );

        getLogger().info(
                "        ProtocolLib CONECTADO"
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
                "        KOTH ACTIVADO"
        );

        getLogger().info(
                "        Airdrops ACTIVADOS"
        );

        getLogger().info(
                "================================="
        );
    }

    /**
     * Registra un comando definido en plugin.yml.
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
                    "No se encontró el mundo configurado: "
                            + worldName
            );

            return;
        }

        WorldBorder border =
                world.getWorldBorder();

        double centerX =
                getConfig().getDouble(
                        "world.center-x",
                        0.0
                );

        double centerZ =
                getConfig().getDouble(
                        "world.center-z",
                        0.0
                );

        double size =
                getConfig().getDouble(
                        "world.border-size",
                        5000.0
                );

        if (size <= 0) {
            size = 5000.0;
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

        getLogger().info(
                "Apagando HCFCore..."
        );

        /*
         * ==============================
         * TAREAS
         * ==============================
         */

        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }

        if (dtrTask != null) {
            dtrTask.cancel();
            dtrTask = null;
        }

        /*
         * ==============================
         * AIRDROPS
         * ==============================
         */

        if (airdropManager != null) {
            airdropManager.shutdown();
        }

        /*
         * ==============================
         * KOTH
         * ==============================
         */

        if (kothManager != null
                && kothManager.isActive()) {

            kothManager.stopKoth();
        }

        /*
         * ==============================
         * DATOS
         * ==============================
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

        /*
         * ==============================
         * DATABASE
         * ==============================
         */

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        /*
         * ==============================
         * INSTANCE
         * ==============================
         */

        instance = null;

        getLogger().info(
                "HCFCore desactivada correctamente."
        );
    }

    public static HCFCore getInstance() {
        return instance;
    }

    /*
     * ==============================
     * EXTERNAL HOOK GETTERS
     * ==============================
     */

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    /*
     * ==============================
     * MANAGER GETTERS
     * ==============================
     */

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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