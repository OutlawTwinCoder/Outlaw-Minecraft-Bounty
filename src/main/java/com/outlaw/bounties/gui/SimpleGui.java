package com.outlaw.bounties.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleGui {
    public abstract String title();
    public abstract int size();
    public abstract void init(Inventory inv);

    public Inventory create() {
        Inventory inv = Bukkit.createInventory(null, size(), title());
        init(inv);
        return inv;
    }

    protected ItemStack withMeta(ItemStack it, String name, List<String> lore) {
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            if (name != null) m.setDisplayName(org.bukkit.ChatColor.RESET + name);
            if (lore != null) m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    protected List<String> lore(String... lines) {
        List<String> out = new ArrayList<>();
        for (String l : lines) out.add(org.bukkit.ChatColor.GRAY + l);
        return out;
    }
}
