package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ShopOffer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ShopManager {
    private final BountyPlugin plugin;
    private final Map<String, ShopOffer> offers = new LinkedHashMap<>();

    public ShopManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        offers.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("shop");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            ShopOffer offer = new ShopOffer();
            offer.id = id;
            offer.display = sec.getString("display", id);
            offer.description = sec.getString("description", "");
            offer.cost = sec.getInt("cost", 1);
            offer.icon = parseMaterial(sec.getString("icon", "CHEST"), Material.CHEST);
            offer.rewardItem = parseMaterial(sec.getString("item", "STONE"), Material.STONE);
            offer.rewardAmount = Math.max(1, sec.getInt("amount", 1));
            offers.put(id, offer);
        }
    }

    private Material parseMaterial(String value, Material def) {
        if (value == null) return def;
        Material mat = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        return mat != null ? mat : def;
    }

    public Collection<ShopOffer> all() {
        return offers.values();
    }

    public ShopOffer get(String id) {
        return offers.get(id);
    }
}
