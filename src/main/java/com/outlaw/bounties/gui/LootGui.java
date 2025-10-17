package com.outlaw.bounties.gui;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ActiveBounty;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LootGui extends SimpleGui implements Listener {
    private final BountyPlugin plugin;
    private final Player player;

    public LootGui(BountyPlugin plugin, Player p) {
        this.plugin = plugin;
        this.player = p;
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override public String title() { return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_loot", "&8Récompenses")); }
    @Override public int size() { return 27; }

    @Override
    public void init(Inventory inv) {
        var ab = plugin.activeBountyManager().get(player.getUniqueId());
        if (ab != null && ab.state == ActiveBounty.State.KILLED) {
            inv.setItem(13, withMeta(new ItemStack(Material.CHEST_MINECART), "§a" + plugin.locale().tr("gui.claim"), lore("Cliquer pour récupérer")));
        } else {
            inv.setItem(13, withMeta(new ItemStack(Material.BARRIER), "§c" + plugin.locale().tr("messages.not_killed_yet"), null));
        }
        inv.setItem(22, withMeta(new ItemStack(Material.ARROW), "§7" + plugin.locale().tr("gui.back"), null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getRawSlot() == 22) {
            plugin.guiManager().openMain(p);
            return;
        }
        if (e.getRawSlot() == 13) {
            var ab = plugin.activeBountyManager().get(p.getUniqueId());
            if (ab == null || ab.state != ActiveBounty.State.KILLED) {
                p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.not_killed_yet"));
                return;
            }
            plugin.activeBountyManager().giveRewards(p);
            plugin.activeBountyManager().markClaimed(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.claimed"));
            plugin.activeBountyManager().clear(p.getUniqueId());
            p.closeInventory();
        }
    }
}
