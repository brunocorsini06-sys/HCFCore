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
    private StaffManager staffManager;
    private StaffToolsManager staffToolsManager;
    private VanishManager vanishManager;

    private BukkitTask scoreboardTask;
    private BukkitTask dtrTask;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        getLogger().info("Iniciando HCFCore...");

        /*
         * ==============================
         * EXTERNAL HOOKS
         * ==============================
         */

        try {

            luckPermsHook =
                    new LuckPermsHook(this);

            vaultHook =
                    new VaultHook(this);

            protocolLibHook =
                    new ProtocolLibHook(this);

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

        databaseManager =
                new DatabaseManager(this);

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

        factionManager =
                new FactionManager(this);

        claimManager =
                new ClaimManager(this);

        combatManager =
                new CombatManager(this);

        deathbanManager =
                new DeathbanManager(this);

        economyManager =
                new EconomyManager(this);

        kitManager =
                new KitManager(this);

        classManager =
                new ClassManager(this);

        airdropManager =
                new AirdropManager(this);

        kothManager =
                new KothManager(this);

        scoreboardManager =
                new ScoreboardManager(this);

        villagerManager =
                new VillagerManager(this);

        tabListManager =
                new TabListManager(this);

        staffManager =
                new StaffManager(this);

        staffToolsManager =
                new StaffToolsManager(this);

        vanishManager =
                new VanishManager(this);

        /*
         * ==============================
         * WORLD BORDER
         * ==============================
         */

        setupWorldBorder();

        /*
         * ==============================
         * EVENTS
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
         * GUI
         * ==============================
         */

        guiManager =
                new GUIManager(this);

        guiManager.register();

        /*
         * ==============================
         * COMMANDS
         * ==============================
         */

        Commands commands =
                new Commands(this);

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
        registerCommand("staff", commands);

        /*
         * ==============================
         * SCOREBOARD TASK
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
         * DTR TASK
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
         * CLASS TASK
         * ==============================
         */

        if (classManager != null) {

            classManager.startTask();
        }

        /*
         * ==============================
         * COMBAT TASK
         * ==============================
         */

        if (combatManager != null) {

            combatManager.startCleanupTask();
        }

        /*
         * ==============================
         * AIRDROP
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
                "        Staff Mode ACTIVADO"
        );

        getLogger().info(
                "        Staff Tools ACTIVADO"
        );

        getLogger().info(
                "        Vanish ACTIVADO"
        );

        getLogger().info(
                "================================="
        );
    }

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

        if (scoreboardTask != null) {

            scoreboardTask.cancel();
            scoreboardTask = null;
        }

        if (dtrTask != null) {

            dtrTask.cancel();
            dtrTask = null;
        }

        /*
         * Desactivar Vanish.
         */

        if (vanishManager != null) {

            vanishManager.disableAll();
        }

        /*
         * Desactivar Staff Mode.
         */

        if (staffManager != null) {

            staffManager.disableAll();
        }

        if (airdropManager != null) {

            airdropManager.shutdown();
        }

        if (kothManager != null
                && kothManager.isActive()) {

            kothManager.stopKoth();
        }

        if (factionManager != null) {

            factionManager.saveAll();
        }

        if (economyManager != null) {

            economyManager.saveAll();
        }

        if (claimManager != null) {

            claimManager.saveAll();
        }

        if (databaseManager != null) {

            databaseManager.disconnect();
        }

        instance = null;

        getLogger().info(
                "HCFCore desactivada correctamente."
        );
    }

    public static HCFCore getInstance() {
        return instance;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

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

    public StaffManager getStaffManager() {
        return staffManager;
    }

    public StaffToolsManager getStaffToolsManager() {
        return staffToolsManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }
}