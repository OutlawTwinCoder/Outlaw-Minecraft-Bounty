package com.outlaw.bounties.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ShopOffer {
    public String id;
    public String display;
    public Material icon;
    public Material rewardItem;
    public int rewardAmount;
    public int cost;
    public List<String> description = new ArrayList<>();
    public List<String> commands = new ArrayList<>();
}
