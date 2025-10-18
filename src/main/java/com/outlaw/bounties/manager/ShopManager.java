package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ShopOffer;
import com.outlaw.bounties.util.ItemParser;
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
            offer.cost = Math.max(1, sec.getInt("cost", 1));
            offer.tierId = sec.getString("tier", null);

            Object rewardSpec = sec.contains("reward") ? sec.get("reward") : sec.get("item");
            int defaultAmount = sec.getInt("amount", -1);
            if (rewardSpec == null && sec.isConfigurationSection("reward")) {
                rewardSpec = sec.getConfigurationSection("reward");
            }
            offer.reward = ItemParser.parseItem(plugin, rewardSpec, defaultAmount);
            if (offer.reward == null) {
                plugin.getLogger().warning("Shop offer '" + id + "' is missing a reward item");
                continue;
            }

            offer.rewardAmount = defaultAmount > 0 ? defaultAmount : offer.reward.baseAmount();
            if (offer.rewardAmount <= 0) {
                offer.rewardAmount = offer.reward.baseAmount();
            }

            Object iconSpec = sec.get("icon");
            if (sec.isConfigurationSection("icon")) {
                iconSpec = sec.getConfigurationSection("icon");
            }
            offer.icon = ItemParser.parseItem(plugin, iconSpec != null ? iconSpec : offer.reward.prototype());

            offers.put(id, offer);
        }
    }

    public Collection<ShopOffer> all() {
        return offers.values();
    }

    public ShopOffer get(String id) {
        return offers.get(id);
    }
}
