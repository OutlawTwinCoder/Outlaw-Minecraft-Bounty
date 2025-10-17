package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ShopOffer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private final BountyPlugin plugin;
    private final Map<String, ShopOffer> offers = new LinkedHashMap<>();

    public ShopManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        offers.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection raw = sec.getConfigurationSection(id);
            if (raw == null) continue;

            Material icon = Material.matchMaterial(raw.getString("icon", "EMERALD"));
            if (icon == null) icon = Material.EMERALD;
            Material reward = Material.matchMaterial(raw.getString("reward.item", raw.getString("reward", "EMERALD")));
            if (reward == null) reward = Material.EMERALD;
            int amount = raw.getInt("reward.amount", 1);
            int cost = raw.getInt("cost", 10);

            List<String> description = new ArrayList<>(raw.getStringList("description"));
            List<String> commands = raw.getStringList("commands");

            ShopOffer offer = new ShopOffer();
            offer.id = id;
            offer.display = raw.getString("display", id);
            offer.icon = icon;
            offer.rewardItem = reward;
            offer.rewardAmount = Math.max(1, amount);
            offer.cost = Math.max(1, cost);
            offer.description = description;
            offer.commands = commands;

            offers.put(id, offer);
        }
    }

    public List<ShopOffer> offers() {
        return new ArrayList<>(offers.values());
    }

    public ShopOffer get(String id) {
        return offers.get(id);
    }
}
