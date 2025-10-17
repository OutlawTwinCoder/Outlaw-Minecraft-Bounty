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

        Player killer = le.getKiller();
        Player targetPlayer = killer != null && killer.getUniqueId().equals(ownerId)
                ? killer
                : Bukkit.getPlayer(ownerId);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.killed_monster"));
        }

        var active = plugin.activeBountyManager().get(ownerId);
        if (active == null) return;
        var bounty = plugin.bountyManager().get(active.bountyId);
        if (bounty == null || bounty.pointsReward <= 0) return;

        int total = plugin.pointsManager().addPoints(ownerId, bounty.pointsReward);
        if (targetPlayer != null) {
            java.util.Map<String, String> awardedVars = new java.util.HashMap<>();
            awardedVars.put("points", String.valueOf(bounty.pointsReward));
            awardedVars.put("total", String.valueOf(total));
            targetPlayer.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.points_awarded", awardedVars));

            java.util.Map<String, String> balanceVars = new java.util.HashMap<>();
            balanceVars.put("points", String.valueOf(total));
            balanceVars.put("total", String.valueOf(total));
            targetPlayer.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.points_balance", balanceVars));
        }
    }
}
