package com.outlaw.bounties.listener;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.manager.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCListener implements Listener {
    private final BountyPlugin plugin;
    public NPCListener(BountyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Villager v) {
            if (v.hasMetadata(NPCManager.NPC_META) || v.getScoreboardTags().contains(NPCManager.NPC_META)) {
                e.setCancelled(true);
                Player p = e.getPlayer();
                plugin.guiManager().openMain(p);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Villager v) {
            if (v.hasMetadata(NPCManager.NPC_META) || v.getScoreboardTags().contains(NPCManager.NPC_META)) {
                e.setCancelled(true);
            }
        }
    }
}
