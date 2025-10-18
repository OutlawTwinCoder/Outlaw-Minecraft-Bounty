package com.outlaw.bounties.commands;

import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.manager.SafeZoneManager;
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
            sender.sendMessage(ChatColor.YELLOW + "/bounty create safezone");
            sender.sendMessage(ChatColor.YELLOW + "/bounty create safezone circle");
            sender.sendMessage(ChatColor.YELLOW + "/bounty show safezone");
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
            case "create" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Only in-game."); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("safezone")) {
                    if (args.length >= 3 && args[2].equalsIgnoreCase("circle")) {
                        if (args.length >= 4 && args[3].equalsIgnoreCase("done")) {
                            plugin.safeZoneSelectionManager().completePolygon(p);
                        } else {
                            plugin.safeZoneSelectionManager().beginPolygon(p);
                        }
                    } else {
                        plugin.safeZoneSelectionManager().beginCuboid(p);
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "/bounty create safezone");
                    sender.sendMessage(ChatColor.YELLOW + "/bounty create safezone circle");
                }
            }
            case "show" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Only in-game."); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("safezone")) {
                    plugin.safeZoneVisualizer().showZones(p, plugin.safeZoneManager().all());
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "/bounty show safezone");
                }
            }
            case "safezone" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Only in-game."); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    p.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.safezone_list_header"));
                    for (var z : plugin.safeZoneManager().all()) {
                        p.sendMessage(ChatColor.GRAY + plugin.locale().tr(
                                "messages.safezone_list_item",
                                "id", z.id,
                                "type", zoneType(z),
                                "info", describeZone(z)
                        ));
                    }
                } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                    try {
                        int id = Integer.parseInt(args[2]);
                        boolean ok = plugin.safeZoneManager().remove(id);
                        if (ok) p.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_removed", "id", id));
                        else p.sendMessage(ChatColor.RED + "ID invalide.");
                    } catch (Exception ex) {
                        p.sendMessage(ChatColor.RED + "Usage: /bounty safezone remove <id>");
                    }
                } else {
                    p.sendMessage(ChatColor.YELLOW + "/bounty safezone list");
                    p.sendMessage(ChatColor.YELLOW + "/bounty safezone remove <id>");
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.locale().reload();
                plugin.bountyManager().reload();
                plugin.safeZoneManager().reload();
                plugin.shopManager().reload();
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

    private String describeZone(SafeZoneManager.Zone zone) {
        return switch (zone.type) {
            case SPHERE -> String.format("(%.1f, %.1f, %.1f) r=%.1f", zone.x, zone.y, zone.z, zone.radius);
            case CUBOID -> String.format("(%d,%d) -> (%d,%d)", zone.minX, zone.minZ, zone.maxX, zone.maxZ);
            case POLYGON -> "points=" + zone.anchors.size();
        };
    }

    private String zoneType(SafeZoneManager.Zone zone) {
        String key = switch (zone.type) {
            case SPHERE, POLYGON -> "messages.safezone_type_circle";
            case CUBOID -> "messages.safezone_type_square";
        };
        return plugin.locale().tr(key);
    }
}
