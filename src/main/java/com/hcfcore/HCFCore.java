package com.hcfcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("kits.yml", false);
        saveResource("airdrops.yml", false);
        saveResource("koth.yml", false);
        saveResource("factions.yml", false);

        setupWorldBorder();

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

        getServer().getPluginManager().registerEvents(
                new HCFListener(this), this
        );

        Commands commands = new Commands(this);

        getCommand("f").setExecutor(commands);
        getCommand("hcf").setExecutor(commands);
        getCommand("claim").setExecutor(commands);
        getCommand("combat").setExecutor(commands);
        getCommand("pay").setExecutor(commands);
        getCommand("kit").setExecutor(commands);
        getCommand("class").setExecutor(commands);
        getCommand("spawn").setExecutor(commands);
        getCommand("koth").setExecutor(commands);
        getCommand("airdrop").setExecutor(commands);
        getCommand("lives").setExecutor(commands);
        getCommand("deathban").setExecutor(commands);
        getCommand("balance").setExecutor(commands);

        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> scoreboardManager.updateAll(),
                20L,
                20L
        );

        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> factionManager.regenerateDtr(),
                20L,
                20L
        );

        if (getConfig().getBoolean("airdrop.enabled", true)) {
            airdropManager.startScheduler();
        }

        getLogger().info("=================================");
        getLogger().info("        HCFCore ACTIVADO");
        getLogger().info("        Version 2.0.0");
        getLogger().info("        Paper 1.20.4");
        getLogger().info("=================================");
    }

    private void setupWorldBorder() {
        String worldName = getConfig().getString("world.name", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            getLogger().warning(
                    "No se encontró el mundo: " + worldName
            );
            return;
        }

        WorldBorder border = world.getWorldBorder();

        border.setCenter(
                getConfig().getDouble("world.center-x", 0),
                getConfig().getDouble("world.center-z", 0)
        );

        border.setSize(
                getConfig().getDouble("world.border-size", 5000)
        );

        getLogger().info(
                "WorldBorder configurada en 5000x5000."
        );
    }

    @Override
    public void onDisable() {
        if (factionManager != null) {
            factionManager.saveAll();
        }

        if (economyManager != null) {
            economyManager.saveAll();
        }

        if (claimManager != null) {
            claimManager.saveAll();
        }

        getLogger().info("HCFCore desactivada correctamente.");
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
}