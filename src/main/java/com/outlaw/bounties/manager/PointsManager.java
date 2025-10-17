package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final BountyPlugin plugin;
    private final Map<UUID, Integer> cache = new HashMap<>();

    public PointsManager(BountyPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        cache.clear();
        ConfigurationSection sec = plugin.dataCfg().getConfigurationSection("points");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                cache.put(uuid, sec.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public int getPoints(UUID playerId) {
        return cache.getOrDefault(playerId, 0);
    }

    public int addPoints(UUID playerId, int amount) {
        if (amount <= 0) return getPoints(playerId);
        int total = getPoints(playerId) + amount;
        cache.put(playerId, total);
        save(playerId, total);
        return total;
    }

    public boolean spendPoints(UUID playerId, int cost) {
        if (cost <= 0) return true;
        int current = getPoints(playerId);
        if (current < cost) return false;
        int total = current - cost;
        cache.put(playerId, total);
        save(playerId, total);
        return true;
    }

    private void save(UUID playerId, int total) {
        plugin.dataCfg().set("points." + playerId, total);
        plugin.saveData();
    }
}
