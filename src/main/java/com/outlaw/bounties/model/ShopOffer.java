package com.outlaw.bounties.model;

import com.outlaw.bounties.item.ConfiguredItem;

public class ShopOffer {
    public String id;
    public String display;
    public String description;
    public ConfiguredItem icon;
    public ConfiguredItem reward;
    public int rewardAmount;
    public int cost;
    public String tierId;
}
