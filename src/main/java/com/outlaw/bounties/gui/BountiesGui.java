package com.outlaw.bounties.gui;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.Bounty;
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

public class BountiesGui extends SimpleGui implements Listener {
    private final BountyPlugin plugin;
    private final Player player;
    private final List<String> order = new ArrayList<>();

    public BountiesGui(BountyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override public String title() { return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_bounties", "&8Choisir un Bounty")); }
    @Override public int size() { return 54; }

    @Override
    public void init(Inventory inv) {
        order.clear();
        int slot = 10;
        for (Bounty b : plugin.bountyManager().all()) {
            order.add(b.id);
            ItemStack it = new ItemStack(Material.CROSSBOW);
            java.util.List<String> l = new java.util.ArrayList<>();
            if (b.description != null && !b.description.isEmpty()) {
                for (String line : b.description.split("\\n")) {
                    l.add(ChatColor.GRAY + line);
                }
                l.add("");
            }
            if (b.tierDisplay != null && !b.tierDisplay.isEmpty()) {
                l.add(ChatColor.AQUA + plugin.locale().tr("gui.bounty_tier", java.util.Map.of("tier", b.tierDisplay)));
                l.add("");
            }
            l.add(ChatColor.GOLD + plugin.locale().tr("gui.bounty_points", java.util.Map.of(
                    "points", String.valueOf(Math.max(0, b.pointsReward))
            )));
            l.add("");
            l.add(ChatColor.YELLOW + "» " + plugin.locale().tr("gui.start"));
            inv.setItem(slot, withMeta(it, "§e" + b.display, l));
            slot++;
            if ((slot+1) % 9 == 0) slot += 2;
        }
        inv.setItem(49, withMeta(new ItemStack(Material.ARROW), "§7" + plugin.locale().tr("gui.back"), null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getRawSlot() == 49) {
            plugin.guiManager().openMain(p);
            return;
        }

        int slot = e.getRawSlot();
        int idx = -1;
        int s = 10; int i=0;
        while (s < 44 && i < order.size()) {
            if (slot == s) { idx = i; break; }
            i++; s++; if ((s+1)%9 == 0) s += 2;
        }
        if (idx >= 0) {
            p.closeInventory();
            if (plugin.activeBountyManager().hasActive(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.already_active"));
                return;
            }
            String id = order.get(idx);
            var b = plugin.bountyManager().get(id);
            if (b == null) return;
            boolean ok = plugin.activeBountyManager().startBounty(p, b);
            if (ok) {
                p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.bounty_started", java.util.Map.of("name", b.display)));
            } else {
                p.sendMessage(ChatColor.RED + "Impossible de démarrer ce bounty pour le moment.");
            }
        }
    }
}
