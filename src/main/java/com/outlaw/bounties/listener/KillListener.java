package com.outlaw.bounties.listener;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.manager.ActiveBountyManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillListener implements Listener {
    private final BountyPlugin plugin;
    public KillListener(BountyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        var killer = le.getKiller();
        if (killer == null) return;
        String tag = ActiveBountyManager.MOB_TAG_PREFIX + killer.getUniqueId();
        if (le.getScoreboardTags().contains(tag)) {
            plugin.activeBountyManager().markKilled(killer.getUniqueId());
            killer.sendMessage(org.bukkit.ChatColor.GREEN + plugin.locale().tr("messages.killed_monster"));
        }
    }
}
