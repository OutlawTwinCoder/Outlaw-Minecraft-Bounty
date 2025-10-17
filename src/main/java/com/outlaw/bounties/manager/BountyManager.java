package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.Bounty;
import com.outlaw.bounties.model.LootReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.*;

public class BountyManager {
    private final BountyPlugin plugin;
    private final Map<String, Bounty> bounties = new LinkedHashMap<>();

    public BountyManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        bounties.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("bounties");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection b = sec.getConfigurationSection(id);
            if (b == null) continue;
            Bounty bo = new Bounty();
            bo.id = id;
            bo.display = b.getString("display", id);
            bo.description = b.getString("description", "");
            bo.entityType = EntityType.valueOf(b.getString("entity", "ZOMBIE").toUpperCase(Locale.ROOT));
            bo.health = b.getDouble("health", 20.0);
            bo.glowingSeconds = b.getInt("glowing_seconds", 10);
            var eq = b.getConfigurationSection("equipment");
            if (eq != null) {
                bo.hand  = eq.getString("hand", null);
                bo.head  = eq.getString("head", null);
                bo.chest = eq.getString("chest", null);
                bo.legs  = eq.getString("legs", null);
                bo.feet  = eq.getString("feet", null);
            }
            for (var raw : b.getMapList("rewards")) {
                Map<String, Object> map = new HashMap<>();
                if (raw instanceof Map<?,?> r) {
                    for (var e : r.entrySet()) {
                        if (e.getKey() != null) map.put(e.getKey().toString(), e.getValue());
                    }
                }
                LootReward lr = new LootReward();
                lr.item   = String.valueOf(map.getOrDefault("item","STONE"));
                lr.min    = toInt(map.get("min"), 1);
                lr.max    = toInt(map.get("max"), 1);
                lr.chance = toDouble(map.get("chance"), 1.0);
                bo.rewards.add(lr);
            }
            bounties.put(id, bo);
        }
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }
    private double toDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }

    public Collection<Bounty> all() { return bounties.values(); }
    public Bounty get(String id) { return bounties.get(id); }
}
