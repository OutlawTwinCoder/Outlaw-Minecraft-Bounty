package com.outlaw.bounties.gui;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ShopOffer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BountyShopGui extends SimpleGui implements Listener {
    private final BountyPlugin plugin;
    private final Player player;
    private final List<String> order = new ArrayList<>();

    public BountyShopGui(BountyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String title() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_shop", "&8Bounty Shop"));
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void init(Inventory inv) {
        int points = plugin.pointsManager().getPoints(player.getUniqueId());
        inv.setItem(4, withMeta(new ItemStack(Material.SUNFLOWER),
                "§e" + plugin.locale().tr("gui.shop_balance_title"),
                java.util.List.of(ChatColor.GRAY + plugin.locale().tr("gui.shop_balance_value", Map.of("points", String.valueOf(points))))));

        int slot = 10;
        order.clear();
        for (ShopOffer offer : plugin.shopManager().offers()) {
            order.add(offer.id);
            List<String> lore = new ArrayList<>();
            for (String line : offer.description) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");
            lore.add(ChatColor.GOLD + plugin.locale().tr("gui.shop_cost", Map.of("cost", String.valueOf(offer.cost))));
            ItemStack icon = new ItemStack(offer.icon);
            inv.setItem(slot, withMeta(icon, "§a" + offer.display, lore));
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot >= 26) break;
        }

        inv.setItem(22, withMeta(new ItemStack(Material.ARROW), "§7" + plugin.locale().tr("gui.back"), null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;

        if (e.getRawSlot() == 22) {
            plugin.guiManager().openMain(p);
            return;
        }

        int slot = e.getRawSlot();
        int idx = -1;
        int s = 10;
        int i = 0;
        while (s < 26 && i < order.size()) {
            if (slot == s) { idx = i; break; }
            i++; s++;
            if ((s + 1) % 9 == 0) s += 2;
        }

        if (idx < 0) return;

        ShopOffer offer = plugin.shopManager().get(order.get(idx));
        if (offer == null) return;

        int cost = offer.cost;
        if (!plugin.pointsManager().spendPoints(player.getUniqueId(), cost)) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr("messages.shop_not_enough"));
            return;
        }

        if (!offer.commands.isEmpty()) {
            for (String command : offer.commands) {
                String parsed = command.replace("%player%", player.getName()).replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        } else {
            ItemStack reward = new ItemStack(offer.rewardItem, offer.rewardAmount);
            var leftover = player.getInventory().addItem(reward);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        }

        player.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.shop_purchased", Map.of(
                "item", offer.display,
                "cost", String.valueOf(cost)
        )));
        plugin.guiManager().openShop(player);
    }
}
