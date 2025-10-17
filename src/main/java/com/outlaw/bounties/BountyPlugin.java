package com.outlaw.bounties;

import com.outlaw.bounties.commands.BountyCommand;
import com.outlaw.bounties.commands.BountyCompleteCommand;
import com.outlaw.bounties.gui.GuiListener;
import com.outlaw.bounties.locale.LocaleManager;
import com.outlaw.bounties.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class BountyPlugin extends JavaPlugin {

    private static BountyPlugin instance;

    private LocaleManager locale;
    private BountyManager bountyManager;
    private SafeZoneManager safeZoneManager;
    private NPCManager npcManager;
    private ActiveBountyManager activeBountyManager;
    private SpawnManager spawnManager;
    private GuiManager guiManager;
    private SafeZoneSelectionManager safeZoneSelectionManager;
    private SafeZoneVisualizer safeZoneVisualizer;
    private PointsManager pointsManager;
    private ShopManager shopManager;

    private File zonesFile, dataFile, npcFile;
    private FileConfiguration zonesCfg, dataCfg, npcCfg;

    public static BountyPlugin get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        setupFiles();

        this.locale = new LocaleManager(this);
        this.bountyManager = new BountyManager(this);
        this.safeZoneManager = new SafeZoneManager(this);
        this.npcManager = new NPCManager(this);
        this.pointsManager = new PointsManager(this);
        this.shopManager = new ShopManager(this);
        this.activeBountyManager = new ActiveBountyManager(this);
        this.spawnManager = new SpawnManager(this);
        this.guiManager = new GuiManager(this);
        this.safeZoneSelectionManager = new SafeZoneSelectionManager(this);
        this.safeZoneVisualizer = new SafeZoneVisualizer(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new com.outlaw.bounties.listener.NPCListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.outlaw.bounties.listener.KillListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(safeZoneSelectionManager, this);

        // Commands
        Objects.requireNonNull(getCommand("bounty")).setExecutor(new BountyCommand(this));
        Objects.requireNonNull(getCommand("bountycomplete")).setExecutor(new BountyCompleteCommand(this));

        // Respawn NPC on load if defined
        npcManager.respawnNPCIfNeeded();
        getLogger().info("OutlawBounties enabled.");
    }

    @Override
    public void onDisable() {
        npcManager.despawnNPC();
        getLogger().info("OutlawBounties disabled.");
    }

    private void setupFiles() {
        zonesFile = new File(getDataFolder(), "zones.yml");
        dataFile = new File(getDataFolder(), "data.yml");
        npcFile = new File(getDataFolder(), "npc.yml");
        try {
            if (!zonesFile.exists()) { saveResource("zones.yml", false); }
            if (!dataFile.exists())  { saveResource("data.yml", false); }
            if (!npcFile.exists())   { saveResource("npc.yml", false); }
        } catch (Exception ignored) {}
        zonesCfg = YamlConfiguration.loadConfiguration(zonesFile);
        dataCfg = YamlConfiguration.loadConfiguration(dataFile);
        npcCfg = YamlConfiguration.loadConfiguration(npcFile);
    }

    public LocaleManager locale() { return locale; }
    public BountyManager bountyManager() { return bountyManager; }
    public SafeZoneManager safeZoneManager() { return safeZoneManager; }
    public SafeZoneSelectionManager safeZoneSelectionManager() { return safeZoneSelectionManager; }
    public SafeZoneVisualizer safeZoneVisualizer() { return safeZoneVisualizer; }
    public NPCManager npcManager() { return npcManager; }
    public ActiveBountyManager activeBountyManager() { return activeBountyManager; }
    public SpawnManager spawnManager() { return spawnManager; }
    public GuiManager guiManager() { return guiManager; }
    public PointsManager pointsManager() { return pointsManager; }
    public ShopManager shopManager() { return shopManager; }

    public FileConfiguration zonesCfg() { return zonesCfg; }
    public FileConfiguration dataCfg() { return dataCfg; }
    public FileConfiguration npcCfg() { return npcCfg; }

    public void saveZones() { try { zonesCfg.save(zonesFile); } catch (Exception e) { e.printStackTrace(); } }
    public void saveData()  { try { dataCfg.save(dataFile); }  catch (Exception e) { e.printStackTrace(); } }
    public void saveNpc()   { try { npcCfg.save(npcFile); }    catch (Exception e) { e.printStackTrace(); } }

    public World getTargetWorld() {
        String w = getConfig().getString("world", "world");
        World world = getServer().getWorld(w);
        if (world == null && !getServer().getWorlds().isEmpty()) {
            world = getServer().getWorlds().get(0);
        }
        return world;
    }
}
