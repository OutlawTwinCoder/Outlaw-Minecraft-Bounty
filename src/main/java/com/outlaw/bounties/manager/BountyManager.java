package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.item.ConfiguredItem;
import com.outlaw.bounties.model.Bounty;
import com.outlaw.bounties.model.LootReward;
import com.outlaw.bounties.model.TierDefinition;
import com.outlaw.bounties.util.ItemParser;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class BountyManager {
    private final BountyPlugin plugin;
    private final Map<String, Bounty> bounties = new LinkedHashMap<>();
    private final Map<String, List<Bounty>> bountiesByTier = new LinkedHashMap<>();
    private final Map<String, TierDefinition> tiers = new LinkedHashMap<>();

    public BountyManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        bounties.clear();
        bountiesByTier.clear();
        tiers.clear();

        loadTiers();

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("bounties");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection section = sec.getConfigurationSection(key);
            if (section == null) continue;

            if (looksLikeBounty(section)) {
                loadBounty(section, key, null);
            } else {
                for (String id : section.getKeys(false)) {
                    ConfigurationSection child = section.getConfigurationSection(id);
                    if (child == null) continue;
                    loadBounty(child, id, key);
                }
            }
        }

        bounties.values().forEach(b -> bountiesByTier.computeIfAbsent(b.tierId, k -> new ArrayList<>()).add(b));
        for (List<Bounty> list : bountiesByTier.values()) {
            list.sort(Comparator.comparingInt((Bounty b) -> b.tierLevel).thenComparing(b -> b.display));
        }
    }

    private void loadTiers() {
        ConfigurationSection tierSection = plugin.getConfig().getConfigurationSection("tiers");
        if (tierSection == null) return;
        int idx = 1;
        for (String id : tierSection.getKeys(false)) {
            ConfigurationSection sec = tierSection.getConfigurationSection(id);
            if (sec == null) continue;
            TierDefinition tier = new TierDefinition();
            tier.id = id;
            tier.displayName = sec.getString("display", id);
            tier.description = sec.getString("description", "");
            tier.level = sec.getInt("level", idx);
            String iconName = sec.getString("icon", "NETHER_STAR");
            tier.icon = Optional.ofNullable(iconName)
                    .map(name -> org.bukkit.Material.matchMaterial(name.toUpperCase(Locale.ROOT)))
                    .orElse(org.bukkit.Material.NETHER_STAR);
            tiers.put(id, tier);
            idx++;
        }
    }

    private boolean looksLikeBounty(ConfigurationSection section) {
        return section.isString("entity") || section.isSet("points_reward");
    }

    private void loadBounty(ConfigurationSection section, String id, String defaultTier) {
        try {
            Bounty bo = new Bounty();
            bo.id = id;
            bo.display = section.getString("display", id);
            bo.description = section.getString("description", "");
            bo.entityType = EntityType.valueOf(section.getString("entity", "ZOMBIE").toUpperCase(Locale.ROOT));
            bo.health = section.getDouble("health", 20.0);
            bo.glowingSeconds = section.getInt("glowing_seconds", 10);
            bo.pointsReward = section.getInt("points_reward", 1);

            String tierId = section.getString("tier", defaultTier != null ? defaultTier : "tier1");
            TierDefinition tier = tierId != null ? tiers.get(tierId) : null;
            bo.tierId = tierId != null ? tierId : "tier1";
            if (tier != null) {
                bo.tierDisplay = ChatColor.translateAlternateColorCodes('&', tier.displayName);
                bo.tierLevel = tier.level;
            } else {
                bo.tierDisplay = tierId != null ? ChatColor.translateAlternateColorCodes('&', tierId) : "Tier";
                bo.tierLevel = tiers.size() + 1;
            }

            ConfigurationSection equipment = section.getConfigurationSection("equipment");
            if (equipment != null) {
                bo.hand = parseEquipmentItem(equipment, "hand");
                bo.offHand = parseEquipmentItem(equipment, "off_hand");
                bo.head = parseEquipmentItem(equipment, "head");
                bo.chest = parseEquipmentItem(equipment, "chest");
                bo.legs = parseEquipmentItem(equipment, "legs");
                bo.feet = parseEquipmentItem(equipment, "feet");
            }

            if (section.isConfigurationSection("attributes")) {
                ConfigurationSection attr = section.getConfigurationSection("attributes");
                for (String key : attr.getKeys(false)) {
                    try {
                        org.bukkit.attribute.Attribute attribute = org.bukkit.attribute.Attribute.valueOf(key.toUpperCase(Locale.ROOT));
                        double value = attr.getDouble(key);
                        bo.attributes.put(attribute, value);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (section.isList("effects")) {
                for (Object raw : section.getList("effects")) {
                    PotionEffect effect = ItemParser.parsePotionEffect(raw);
                    if (effect != null) {
                        bo.effects.add(effect);
                    }
                }
            }

            for (Map<?, ?> raw : section.getMapList("rewards")) {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    if (entry.getKey() != null) {
                        map.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                Object itemSpec = map.get("item");
                if (itemSpec == null && map.get("custom") != null) {
                    itemSpec = map.get("custom");
                }
                if (itemSpec == null) continue;
                LootReward reward = new LootReward();
                reward.min = toInt(map.get("min"), 1);
                reward.max = toInt(map.get("max"), reward.min);
                if (reward.max < reward.min) reward.max = reward.min;
                reward.chance = toDouble(map.get("chance"), 1.0);
                reward.item = ItemParser.parseItem(plugin, itemSpec, reward.max);
                if (reward.item != null) {
                    bo.rewards.add(reward);
                }
            }

            bounties.put(id, bo);
        } catch (Exception ex) {
            plugin.getLogger().warning("Unable to load bounty '" + id + "': " + ex.getMessage());
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

    public Collection<Bounty> all() {
        List<Bounty> list = new ArrayList<>(bounties.values());
        list.sort(Comparator.comparingInt((Bounty b) -> b.tierLevel).thenComparing(b -> b.display));
        return list;
    }
    public Bounty get(String id) { return bounties.get(id); }

    public TierDefinition getTier(String id) {
        return tiers.get(id);
    }

    public Collection<TierDefinition> tiers() {
        List<TierDefinition> list = new ArrayList<>(tiers.values());
        list.sort(Comparator.comparingInt(t -> t.level));
        return list;
    }

    public List<Bounty> byTier(String tierId) {
        return bountiesByTier.getOrDefault(tierId, Collections.emptyList());
    }

    private ConfiguredItem parseEquipmentItem(ConfigurationSection section, String path) {
        Object spec = section.get(path);
        if (spec == null) return null;
        if (spec instanceof String str && (str.equalsIgnoreCase("null") || str.equalsIgnoreCase("none"))) {
            return null;
        }
        return ItemParser.parseItem(plugin, spec, 1);
    }

}
