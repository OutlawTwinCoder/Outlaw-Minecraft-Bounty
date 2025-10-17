package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ActiveBounty;
import com.outlaw.bounties.model.Bounty;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ActiveBountyManager {
    private final BountyPlugin plugin;
    private final Map<java.util.UUID, ActiveBounty> active = new HashMap<>();
    private final Map<java.util.UUID, java.util.UUID> mobToPlayer = new HashMap<>();

    public static final String MOB_TAG_PREFIX = "OBOUNTY_";

    public ActiveBountyManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public ActiveBounty get(java.util.UUID player) { return active.get(player); }
    public boolean hasActive(java.util.UUID player) { return active.containsKey(player); }
    public java.util.Collection<ActiveBounty> all() { return active.values(); }
    public void clear(java.util.UUID player) {
        var ab = active.remove(player);
        if (ab != null && ab.mobUUID != null) {
            mobToPlayer.remove(ab.mobUUID);
            ab.mobUUID = null;
        }
        persist();
    }
    public void markKilled(java.util.UUID player) {
        var ab = active.get(player);
        if (ab != null) {
            if (ab.mobUUID != null) {
                mobToPlayer.remove(ab.mobUUID);
                ab.mobUUID = null;
            }
            ab.state = ActiveBounty.State.KILLED;
            persist();
        }
    }
    public void markClaimed(java.util.UUID player) {
        var ab = active.get(player);
        if (ab != null) {
            if (ab.mobUUID != null) {
                mobToPlayer.remove(ab.mobUUID);
                ab.mobUUID = null;
            }
            ab.state = ActiveBounty.State.CLAIMED;
            persist();
        }
    }

    public java.util.UUID getPlayerForMob(java.util.UUID mob) { return mobToPlayer.get(mob); }

    private void registerMob(java.util.UUID mob, java.util.UUID player) { mobToPlayer.put(mob, player); }

    public org.bukkit.Location startBounty(Player p, Bounty b) {
        if (hasActive(p.getUniqueId())) return null;
        var spawnLoc = plugin.spawnManager().pickRandomSpawn(plugin.getTargetWorld());
        if (spawnLoc == null) return null;

        LivingEntity mob = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, b.entityType, CreatureSpawnEvent.SpawnReason.CUSTOM);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setCustomNameVisible(true);
        mob.setCustomName(ChatColor.YELLOW + b.display);
        var attr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(Math.max(1.0, b.health));
            mob.setHealth(Math.max(1.0, b.health));
        }
        if (b.glowingSeconds > 0) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20*b.glowingSeconds, 0, true, false, false));
        }
        equip(mob, b);

        String tag = MOB_TAG_PREFIX + p.getUniqueId();
        mob.addScoreboardTag(tag);

        ActiveBounty ab = new ActiveBounty();
        ab.bountyId = b.id;
        ab.playerUUID = p.getUniqueId();
        ab.mobUUID = mob.getUniqueId();
        ab.mobLocation = spawnLoc.clone();
        ab.npcLocation = plugin.npcManager().getNPCLocation();
        ab.state = ActiveBounty.State.STARTED;

        active.put(p.getUniqueId(), ab);
        registerMob(mob.getUniqueId(), p.getUniqueId());
        persist();

        return spawnLoc;
    }

    public void teleportNear(Player p, org.bukkit.Location center) {
        int min = plugin.getConfig().getInt("teleport.ring_min", 36);
        int max = plugin.getConfig().getInt("teleport.ring_max", 64);
        double r = min + (Math.random() * (max - min));
        double ang = Math.random() * Math.PI * 2.0;
        double x = center.getX() + Math.cos(ang) * r;
        double z = center.getZ() + Math.sin(ang) * r;
        org.bukkit.Location candidate = new org.bukkit.Location(center.getWorld(), x, center.getY(), z);
        candidate.setY(center.getWorld().getHighestBlockYAt(candidate) + 1.0);
        p.teleport(candidate);
    }

    private void equip(LivingEntity mob, Bounty b) {
        var eq = mob.getEquipment();
        if (eq == null) return;
        if (b.hand != null && !b.hand.equalsIgnoreCase("null")) eq.setItemInMainHand(new ItemStack(Material.matchMaterial(b.hand)));
        if (b.head != null && !b.head.equalsIgnoreCase("null")) eq.setHelmet(new ItemStack(Material.matchMaterial(b.head)));
        if (b.chest != null && !b.chest.equalsIgnoreCase("null")) eq.setChestplate(new ItemStack(Material.matchMaterial(b.chest)));
        if (b.legs != null && !b.legs.equalsIgnoreCase("null")) eq.setLeggings(new ItemStack(Material.matchMaterial(b.legs)));
        if (b.feet != null && !b.feet.equalsIgnoreCase("null")) eq.setBoots(new ItemStack(Material.matchMaterial(b.feet)));
    }

    public void giveRewards(Player p) {
        var ab = active.get(p.getUniqueId());
        if (ab == null) return;
        var bounty = plugin.bountyManager().get(ab.bountyId);
        if (bounty == null) return;
        for (var r : bounty.rewards) {
            if (Math.random() <= r.chance) {
                int amount = r.min + (int) Math.floor(Math.random() * (r.max - r.min + 1));
                var m = Material.matchMaterial(r.item);
                if (m == null) continue;
                var stack = new ItemStack(m, Math.max(1, amount));
                var rest = p.getInventory().addItem(stack);
                if (!rest.isEmpty()) for (var leftover : rest.values()) p.getWorld().dropItemNaturally(p.getLocation(), leftover);
            }
        }
    }

    private void persist() {
        var cfg = plugin.dataCfg();
        var players = new java.util.HashMap<String, Object>();
        for (var e : active.entrySet()) {
            var ab = e.getValue();
            var map = new java.util.HashMap<String, Object>();
            map.put("bountyId", ab.bountyId);
            map.put("state", ab.state.toString());
            map.put("mobUUID", ab.mobUUID != null ? ab.mobUUID.toString() : null);
            if (ab.npcLocation != null) {
                var l = ab.npcLocation;
                var lmap = new java.util.HashMap<String, Object>();
                lmap.put("world", l.getWorld()!=null?l.getWorld().getName():"world");
                lmap.put("x", l.getX()); lmap.put("y", l.getY()); lmap.put("z", l.getZ()); lmap.put("yaw", l.getYaw());
                map.put("npc", lmap);
            }
            players.put(e.getKey().toString(), map);
        }
        cfg.set("players", players);
        plugin.saveData();
    }
}
