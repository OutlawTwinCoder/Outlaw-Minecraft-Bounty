package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class NPCManager {
    private final BountyPlugin plugin;
    private UUID npcUUID;

    public static final String NPC_META = "OUTLAW_BOUNTY_NPC";

    public NPCManager(BountyPlugin plugin) {
        this.plugin = plugin;
        loadNpcUuid();
    }

    private void loadNpcUuid() {
        var idStr = plugin.npcCfg().getString("npc.uuid", null);
        if (idStr != null) {
            try { npcUUID = UUID.fromString(idStr); } catch (Exception ignored) {}
        }
    }

    public boolean hasNPC() { return getNPCLocation() != null; }

    public Location getNPCLocation() {
        FileConfiguration cfg = plugin.npcCfg();
        if (!cfg.isConfigurationSection("npc")) return null;
        String world = cfg.getString("npc.world", "world");
        double x = cfg.getDouble("npc.x"), y = cfg.getDouble("npc.y"), z = cfg.getDouble("npc.z");
        float yaw = (float) cfg.getDouble("npc.yaw");
        var w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, 0f);
    }

    public Villager spawnNPC(Location at) {
        despawnNPC();
        var w = at.getWorld();
        if (w == null) return null;
        Villager v = w.spawn(at, Villager.class, CreatureSpawnEvent.SpawnReason.CUSTOM);
        v.setAI(false);
        v.setInvulnerable(true);
        v.setPersistent(true);
        v.setCustomNameVisible(true);
        v.setCustomName(org.bukkit.ChatColor.GOLD + plugin.getConfig().getString("menu.npc_name", "Bounty Master"));
        v.setProfession(Villager.Profession.NONE);
        v.setVillagerType(Villager.Type.PLAINS);
        v.setGravity(false);
        v.addScoreboardTag(NPC_META);
        v.setMetadata(NPC_META, new FixedMetadataValue(plugin, true));
        npcUUID = v.getUniqueId();

        var cfg = plugin.npcCfg();
        cfg.set("npc.world", w.getName());
        cfg.set("npc.x", at.getX());
        cfg.set("npc.y", at.getY());
        cfg.set("npc.z", at.getZ());
        cfg.set("npc.yaw", at.getYaw());
        cfg.set("npc.uuid", npcUUID.toString());
        plugin.saveNpc();
        return v;
    }

    public void despawnNPC() {
        if (npcUUID == null) return;
        for (var w : Bukkit.getWorlds()) {
            Entity e = w.getEntity(npcUUID);
            if (e != null) { e.remove(); }
        }
    }

    public void respawnNPCIfNeeded() {
        Location l = getNPCLocation();
        if (l == null) return;
        if (npcUUID != null) {
            for (var w : Bukkit.getWorlds()) {
                var e = w.getEntity(npcUUID);
                if (e instanceof Villager) return;
            }
        }
        spawnNPC(l);
    }
}
