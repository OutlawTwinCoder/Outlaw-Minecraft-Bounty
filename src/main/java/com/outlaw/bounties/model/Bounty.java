package com.outlaw.bounties.model;

import com.outlaw.bounties.item.ConfiguredItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Bounty {
    public String id;
    public String tierId;
    public String tierDisplay;
    public int tierLevel;
    public String display;
    public String description;
    public EntityType entityType;
    public double health;
    public int glowingSeconds;
    public ConfiguredItem hand, offHand, head, chest, legs, feet;
    public final Map<Attribute, Double> attributes = new EnumMap<>(Attribute.class);
    public final List<PotionEffect> effects = new ArrayList<>();
    public int pointsReward;
    public List<LootReward> rewards = new ArrayList<>();
}
