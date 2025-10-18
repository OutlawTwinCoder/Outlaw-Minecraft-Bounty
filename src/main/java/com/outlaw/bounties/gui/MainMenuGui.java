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
    private final Player player;

    public MainMenuGui(BountyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override public String title() { return org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_main", "&8Bounty Hunter")); }
    @Override public int size() { return 27; }

    @Override
    public void init(Inventory inv) {
        var lang = plugin.locale();
        java.util.List<String> bountyLore = new java.util.ArrayList<>();
        bountyLore.add(org.bukkit.ChatColor.GRAY + lang.tr("gui.main_bounties_desc"));
        inv.setItem(11, withMeta(new ItemStack(Material.WRITABLE_BOOK), lang.tr("gui.main_bounties"), bountyLore));

        java.util.List<String> shopLore = new java.util.ArrayList<>();
        shopLore.add(org.bukkit.ChatColor.GRAY + lang.tr("gui.main_shop_desc"));
        shopLore.add("");
        int currentPoints = plugin.pointsManager().getPoints(player.getUniqueId());
        java.util.Map<String, String> shopVars = new java.util.HashMap<>();
        shopVars.put("points", String.valueOf(currentPoints));
        shopVars.put("total", String.valueOf(currentPoints));
        String hint = lang.tr("gui.shop_points_hint", shopVars);
        if (!hint.isBlank()) {
            shopLore.add(org.bukkit.ChatColor.GOLD + hint);
        }
        inv.setItem(13, withMeta(new ItemStack(Material.EMERALD), lang.tr("gui.main_shop"), shopLore));

        java.util.List<String> lootLore = new java.util.ArrayList<>();
        lootLore.add(org.bukkit.ChatColor.GRAY + lang.tr("gui.main_loot_desc"));
        inv.setItem(15, withMeta(new ItemStack(Material.CHEST), lang.tr("gui.main_loot"), lootLore));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getRawSlot() == 11) plugin.guiManager().openBounties(p);
        else if (e.getRawSlot() == 13) plugin.guiManager().openShop(p);
        else if (e.getRawSlot() == 15) plugin.guiManager().openLoot(p);
    }
}
