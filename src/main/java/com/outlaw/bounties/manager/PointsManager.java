package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;

import java.util.UUID;

public class PointsManager {
    private final BountyPlugin plugin;

    public PointsManager(BountyPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.dataCfg().isConfigurationSection("points")) {
            plugin.dataCfg().createSection("points");
            plugin.saveData();
        }
    }

    public int getPoints(UUID playerId) {
        return plugin.dataCfg().getInt(path(playerId), 0);
    }

    public int addPoints(UUID playerId, int amount) {
        if (amount <= 0) return getPoints(playerId);
        int updated = Math.max(0, getPoints(playerId) + amount);
        plugin.dataCfg().set(path(playerId), updated);
        plugin.saveData();
        return updated;
    }

    public boolean spendPoints(UUID playerId, int amount) {
        if (amount <= 0) return true;
        int current = getPoints(playerId);
        if (current < amount) {
            return false;
        }
        plugin.dataCfg().set(path(playerId), current - amount);
        plugin.saveData();
        return true;
    }

    private String path(UUID playerId) {
        return "points." + playerId;
    }
}
