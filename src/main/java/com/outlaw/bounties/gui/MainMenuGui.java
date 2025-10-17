package com.outlaw.bounties.gui;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MainMenuGui extends SimpleGui implements Listener {
    private final BountyPlugin plugin;

    public MainMenuGui(BountyPlugin plugin) {
        this.plugin = plugin;
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override public String title() { return org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_main", "&8Bounty Hunter")); }
    @Override public int size() { return 27; }

    @Override
    public void init(Inventory inv) {
        var lang = plugin.locale();
        inv.setItem(11, withMeta(new ItemStack(Material.WRITABLE_BOOK), lang.tr("gui.main_bounties"), lore("Choisir un contrat")));
        inv.setItem(15, withMeta(new ItemStack(Material.CHEST), lang.tr("gui.main_loot"), lore("Voir / récupérer")));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getRawSlot() == 11) plugin.guiManager().openBounties(p);
        else if (e.getRawSlot() == 15) plugin.guiManager().openLoot(p);
    }
}
