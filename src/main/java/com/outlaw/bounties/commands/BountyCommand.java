package com.outlaw.bounties.commands;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyCommand implements CommandExecutor {
    private final BountyPlugin plugin;
    public BountyCommand(BountyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/bounty npc set|remove");
            sender.sendMessage(ChatColor.YELLOW + "/bounty safezone add <radius>");
            sender.sendMessage(ChatColor.YELLOW + "/bounty safezone list");
            sender.sendMessage(ChatColor.YELLOW + "/bounty safezone remove <id>");
            sender.sendMessage(ChatColor.YELLOW + "/bounty reload");
            sender.sendMessage(ChatColor.YELLOW + "/bounty open");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "npc" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Only in-game."); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    plugin.npcManager().spawnNPC(p.getLocation());
                    p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.npc_set"));
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                    plugin.npcManager().despawnNPC();
                    plugin.npcCfg().set("npc", null);
                    plugin.saveNpc();
                    p.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.npc_removed"));
                } else {
                    p.sendMessage(ChatColor.YELLOW + "/bounty npc set|remove");
                }
            }
            case "safezone" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Only in-game."); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("add")) {
                    double radius = 30.0;
                    if (args.length >= 3) {
                        try { radius = Double.parseDouble(args[2]); } catch (Exception ignored) {}
                    }
                    plugin.safeZoneManager().add(p.getLocation(), radius);
                    p.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.safezone_added", java.util.Map.of("radius", String.valueOf((int)radius))));
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    p.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.safezone_list_header"));
                    for (var z : plugin.safeZoneManager().all()) {
                        var vars = java.util.Map.of(
                                "id", String.valueOf(z.id),
                                "x", String.format("%.1f", z.x),
                                "y", String.format("%.1f", z.y),
                                "z", String.format("%.1f", z.z),
                                "radius", String.format("%.0f", z.radius)
                        );
                        p.sendMessage(ChatColor.GRAY + plugin.locale().tr("messages.safezone_list_item", vars));
                    }
                } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                    try {
                        int id = Integer.parseInt(args[2]);
                        boolean ok = plugin.safeZoneManager().remove(id);
                        if (ok) p.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_removed"));
                        else p.sendMessage(ChatColor.RED + "ID invalide.");
                    } catch (Exception ex) {
                        p.sendMessage(ChatColor.RED + "Usage: /bounty safezone remove <id>");
                    }
                } else {
                    p.sendMessage(ChatColor.YELLOW + "/bounty safezone add <radius>");
                    p.sendMessage(ChatColor.YELLOW + "/bounty safezone list");
                    p.sendMessage(ChatColor.YELLOW + "/bounty safezone remove <id>");
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.locale().reload();
                plugin.bountyManager().reload();
                plugin.safeZoneManager().reload();
                sender.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.reloaded"));
            }
            case "open" -> {
                if (sender instanceof Player p) {
                    plugin.guiManager().openMain(p);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
