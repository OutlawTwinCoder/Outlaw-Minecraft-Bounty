package com.outlaw.bounties.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory() == null) return;
        String t = e.getView().getTitle();
        if (t == null) return;
        if (t.startsWith("§8") || t.startsWith("§0") || t.contains("Bounty")) {
            e.setCancelled(true);
        }
    }
}
