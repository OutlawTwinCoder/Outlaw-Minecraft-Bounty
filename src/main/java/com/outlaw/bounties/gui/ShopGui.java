package com.outlaw.bounties.gui;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ShopOffer;
import com.outlaw.bounties.model.TierDefinition;
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
import java.util.UUID;

public class ShopGui extends SimpleGui implements Listener {
    private final BountyPlugin plugin;
    private final Player player;
    private final List<String> order = new ArrayList<>();

    public ShopGui(BountyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String title() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("menu.title_shop", "&8Boutique Bounty"));
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void init(Inventory inv) {
        order.clear();
        int points = plugin.pointsManager().getPoints(player.getUniqueId());
        ItemStack indicator = withMeta(new ItemStack(Material.SUNFLOWER),
                ChatColor.GOLD + plugin.locale().tr("gui.shop_points", "points", points, "total", points),
                lore(plugin.locale().tr("gui.shop_points_lore")));
        inv.setItem(4, indicator);

        int slot = 10;
        for (ShopOffer offer : plugin.shopManager().all()) {
            order.add(offer.id);
            ItemStack icon = offer.icon != null ? offer.icon.createStack(1) : offer.reward.createStack(1);
            List<String> lore = new ArrayList<>();
            if (offer.description != null && !offer.description.isEmpty()) {
                for (String line : offer.description.split("\\n")) {
                    lore.add(ChatColor.GRAY + line);
                }
                lore.add("");
            }

            if (offer.tierId != null) {
                TierDefinition tier = plugin.bountyManager().getTier(offer.tierId);
                if (tier != null) {
                    String tierName = tier.displayName != null
                            ? ChatColor.translateAlternateColorCodes('&', tier.displayName)
                            : tier.id;
                    lore.add(ChatColor.AQUA + plugin.locale().tr("gui.shop_tier", "tier", tierName));
                    if (tier.description != null && !tier.description.isEmpty()) {
                        lore.add(ChatColor.DARK_AQUA + ChatColor.translateAlternateColorCodes('&', tier.description));
                    }
                    lore.add("");
                }
            }

            lore.add(ChatColor.GOLD + plugin.locale().tr("gui.shop_cost", "cost", offer.cost));

            lore.add(ChatColor.YELLOW + plugin.locale().tr(
                    "gui.shop_reward",
                    "amount", Math.max(1, offer.rewardAmount),
                    "item", ChatColor.stripColor(offer.reward.formattedName())
            ));

            ItemStack preview = offer.reward.createStack(Math.min(Math.max(1, offer.rewardAmount), offer.reward.prototype().getMaxStackSize()));
            if (preview.hasItemMeta() && preview.getItemMeta().hasLore()) {
                lore.add(" ");
                lore.add(ChatColor.GRAY + plugin.locale().tr("gui.shop_reward_stats"));
                for (String line : preview.getItemMeta().getLore()) {
                    lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.RESET + line);
                }
            }

            inv.setItem(slot, withMeta(icon, "§e" + offer.display, lore));
            slot++;
            if ((slot + 1) % 9 == 0) {
                slot += 2;
            }
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
        int s = 10;
        int i = 0;
        while (s < 44 && i < order.size()) {
            if (slot == s) {
                idx = i;
                break;
            }
            i++;
            s++;
            if ((s + 1) % 9 == 0) {
                s += 2;
            }
        }

        if (idx < 0) {
            return;
        }

        ShopOffer offer = plugin.shopManager().get(order.get(idx));
        if (offer == null) {
            return;
        }

        UUID playerId = p.getUniqueId();
        int cost = offer.cost;
        if (!plugin.pointsManager().spendPoints(playerId, cost)) {
            p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.not_enough_points"));
            return;
        }

        giveReward(p, offer);

        p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.shop_purchased", "name", offer.display, "cost", cost));
        int balance = plugin.pointsManager().getPoints(playerId);
        p.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.points_balance", "points", balance, "total", balance));

        plugin.guiManager().openShop(p);
    }

    private void giveReward(Player player, ShopOffer offer) {
        int amount = Math.max(1, offer.rewardAmount);
        int maxPerStack = offer.reward.prototype().getMaxStackSize();
        if (maxPerStack <= 0) maxPerStack = 64;
        int remaining = amount;
        while (remaining > 0) {
            int take = offer.reward.isStackable() ? Math.min(maxPerStack, remaining) : 1;
            ItemStack stack = offer.reward.createStack(take);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
            remaining -= take;
        }
    }
}

