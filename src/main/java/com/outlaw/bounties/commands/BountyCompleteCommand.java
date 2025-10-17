package com.outlaw.bounties.commands;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.model.ActiveBounty;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyCompleteCommand implements CommandExecutor {
    private final BountyPlugin plugin;
    public BountyCompleteCommand(BountyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        var ab = plugin.activeBountyManager().get(p.getUniqueId());
        if (ab == null) {
            p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.no_active"));
            return true;
        }
        if (ab.state != ActiveBounty.State.KILLED) {
            p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.not_killed_yet"));
            return true;
        }
        Location npc = plugin.npcManager().getNPCLocation();
        if (npc == null) {
            p.sendMessage(ChatColor.RED + plugin.locale().tr("messages.no_npc"));
            return true;
        }
        p.teleport(npc);
        p.sendMessage(ChatColor.GRAY + plugin.locale().tr("messages.teleported_back"));
        plugin.guiManager().openLoot(p);
        return true;
    }
}
