package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.gui.MainMenuGui;
import com.outlaw.bounties.gui.BountiesGui;
import com.outlaw.bounties.gui.LootGui;
import org.bukkit.entity.Player;

public class GuiManager {
    private final BountyPlugin plugin;

    public GuiManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player p) { p.openInventory(new MainMenuGui(plugin, p).create()); }
    public void openBounties(Player p) { p.openInventory(new BountiesGui(plugin, p).create()); }
    public void openLoot(Player p) { p.openInventory(new LootGui(plugin, p).create()); }
    public void openShop(Player p) { p.openInventory(new com.outlaw.bounties.gui.BountyShopGui(plugin, p).create()); }
}
