package com.outlaw.bounties.listener;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillListener implements Listener {
    private final BountyPlugin plugin;
    public KillListener(BountyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        var ownerId = plugin.activeBountyManager().getPlayerForMob(le.getUniqueId());
        if (ownerId == null) return;

        plugin.activeBountyManager().markKilled(ownerId);
        var active = plugin.activeBountyManager().get(ownerId);
        int pointsEarned = 0;
        if (active != null) {
            var bounty = plugin.bountyManager().get(active.bountyId);
            if (bounty != null) {
                pointsEarned = Math.max(0, bounty.points);
                if (pointsEarned > 0) {
                    plugin.pointsManager().addPoints(ownerId, pointsEarned);
                }
            }
        }

        Player killer = le.getKiller();
        Player targetPlayer = killer != null && killer.getUniqueId().equals(ownerId)
                ? killer
                : Bukkit.getPlayer(ownerId);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.killed_monster"));
            if (pointsEarned > 0) {
                targetPlayer.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.points_awarded", java.util.Map.of(
                        "points", String.valueOf(pointsEarned),
                        "total", String.valueOf(plugin.pointsManager().getPoints(ownerId))
                )));
            }
        }
    }
}
