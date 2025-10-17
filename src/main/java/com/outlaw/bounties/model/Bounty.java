package com.outlaw.bounties.model;

import org.bukkit.entity.EntityType;
import java.util.ArrayList;
import java.util.List;

public class Bounty {
    public String id;
    public String display;
    public String description;
    public EntityType entityType;
    public double health;
    public int glowingSeconds;
    public String hand, head, chest, legs, feet;
    public List<LootReward> rewards = new ArrayList<>();
    public int points;
}
