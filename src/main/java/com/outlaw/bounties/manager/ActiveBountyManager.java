package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.item.ConfiguredItem;
import com.outlaw.bounties.model.ActiveBounty;
import com.outlaw.bounties.model.Bounty;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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

    public boolean startBounty(Player p, Bounty b) {
        if (hasActive(p.getUniqueId())) return false;
        var spawnLoc = plugin.spawnManager().pickRandomSpawn(plugin.getTargetWorld());
        if (spawnLoc == null) return false;

        LivingEntity mob = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, b.entityType, CreatureSpawnEvent.SpawnReason.CUSTOM);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setCustomNameVisible(true);
        mob.setCustomName(ChatColor.YELLOW + b.display);
        double targetHealth = b.health;
        if (b.attributes.containsKey(Attribute.GENERIC_MAX_HEALTH)) {
            targetHealth = b.attributes.get(Attribute.GENERIC_MAX_HEALTH);
        }
        var attr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(Math.max(1.0, targetHealth));
            mob.setHealth(Math.max(1.0, targetHealth));
        }
        for (var entry : b.attributes.entrySet()) {
            if (entry.getKey() == Attribute.GENERIC_MAX_HEALTH) continue;
            var attribute = mob.getAttribute(entry.getKey());
            if (attribute != null) {
                attribute.setBaseValue(entry.getValue());
            }
        }
        if (b.glowingSeconds > 0) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20*b.glowingSeconds, 0, true, false, false));
        }
        if (!b.effects.isEmpty()) {
            for (PotionEffect effect : b.effects) {
                mob.addPotionEffect(effect);
            }
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

        scheduleTeleport(p, spawnLoc);
        return true;
    }

    private void scheduleTeleport(Player p, org.bukkit.Location spawnLoc) {
        int countdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown_seconds", 3));
        if (countdown <= 0) {
            p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.teleport_now"));
            teleportNear(p, spawnLoc);
            return;
        }

        p.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.teleport_countdown_start", "seconds", countdown));
        p.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.teleport_countdown", "seconds", countdown));

        new BukkitRunnable() {
            int seconds = countdown - 1;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    return;
                }

                if (seconds <= 0) {
                    p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.teleport_now"));
                    teleportNear(p, spawnLoc);
                    cancel();
                    return;
                }

                p.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.teleport_countdown", "seconds", seconds));
                seconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void teleportNear(Player p, org.bukkit.Location center) {
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
        if (b.hand != null) eq.setItemInMainHand(b.hand.createStack(1));
        if (b.offHand != null) eq.setItemInOffHand(b.offHand.createStack(1));
        if (b.head != null) eq.setHelmet(b.head.createStack(1));
        if (b.chest != null) eq.setChestplate(b.chest.createStack(1));
        if (b.legs != null) eq.setLeggings(b.legs.createStack(1));
        if (b.feet != null) eq.setBoots(b.feet.createStack(1));
    }

    public void giveRewards(Player p) {
        var ab = active.get(p.getUniqueId());
        if (ab == null) return;
        var bounty = plugin.bountyManager().get(ab.bountyId);
        if (bounty == null) return;
        for (var r : bounty.rewards) {
            if (Math.random() <= r.chance) {
                int amount = r.min + (int) Math.floor(Math.random() * (r.max - r.min + 1));
                if (r.item == null) continue;
                giveItemStack(p, r.item, amount);
            }
        }
    }

    private void giveItemStack(Player player, ConfiguredItem item, int amount) {
        if (amount <= 0) amount = item.baseAmount();
        int maxPerStack = item.prototype().getMaxStackSize();
        if (maxPerStack <= 0) maxPerStack = 64;
        int remaining = Math.max(1, amount);
        while (remaining > 0) {
            int take = item.isStackable() ? Math.min(maxPerStack, remaining) : 1;
            var stack = item.createStack(take);
            var overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
            remaining -= take;
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
