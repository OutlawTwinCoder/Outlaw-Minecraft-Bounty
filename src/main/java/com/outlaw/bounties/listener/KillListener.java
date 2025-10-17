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
    }
}
