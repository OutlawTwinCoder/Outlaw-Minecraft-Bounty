package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

public class SafeZoneVisualizer {
    private static final long SHOW_DURATION_TICKS = 20L * 30L;

    private final BountyPlugin plugin;

    public SafeZoneVisualizer(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public void showZones(Player player, Collection<SafeZoneManager.Zone> zones) {
        if (zones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_show_none"));
            return;
        }

        Map<BlockKey, BlockData> original = new HashMap<>();
        int displayed = 0;
        for (SafeZoneManager.Zone zone : zones) {
            if (!zone.world.equals(player.getWorld().getName())) continue;
            int before = original.size();
            switch (zone.type) {
                case CUBOID -> visualizeCuboid(player, zone, original);
                case POLYGON -> visualizePolygon(player, zone, original);
                case SPHERE -> visualizeSphere(player, zone, original);
            }
            if (original.size() > before) {
                displayed++;
            }
        }

        if (displayed == 0) {
            player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_show_none"));
            return;
        }

        player.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.safezone_show_started", Map.of(
                "count", String.valueOf(displayed)
        )));

        Bukkit.getScheduler().runTaskLater(plugin, () -> revert(player, original), SHOW_DURATION_TICKS);
    }

    private void visualizeCuboid(Player player, SafeZoneManager.Zone zone, Map<BlockKey, BlockData> original) {
        if (zone.anchors.size() < 2) return;
        int y = zone.anchors.get(0).y;
        for (int x = zone.minX; x <= zone.maxX; x++) {
            sendBlock(player, original, zone.world, x, y, zone.minZ, Material.GLOWSTONE.createBlockData());
            sendBlock(player, original, zone.world, x, y, zone.maxZ, Material.GLOWSTONE.createBlockData());
        }
        for (int z = zone.minZ; z <= zone.maxZ; z++) {
            sendBlock(player, original, zone.world, zone.minX, y, z, Material.GLOWSTONE.createBlockData());
            sendBlock(player, original, zone.world, zone.maxX, y, z, Material.GLOWSTONE.createBlockData());
        }
    }

    private void visualizePolygon(Player player, SafeZoneManager.Zone zone, Map<BlockKey, BlockData> original) {
        if (zone.anchors.isEmpty()) return;
        int y = zone.anchors.get(0).y;
        List<SafeZoneManager.BlockPos> anchors = zone.anchors;
        for (int i = 0; i < anchors.size(); i++) {
            SafeZoneManager.BlockPos a = anchors.get(i);
            SafeZoneManager.BlockPos b = anchors.get((i + 1) % anchors.size());
            drawLine(player, original, zone.world, a.x, a.z, b.x, b.z, y, Material.GLOWSTONE.createBlockData());
        }
    }

    private void visualizeSphere(Player player, SafeZoneManager.Zone zone, Map<BlockKey, BlockData> original) {
        Location center = new Location(player.getWorld(), zone.x, zone.y, zone.z);
        int radius = (int) Math.round(zone.radius);
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            int x = (int) Math.floor(center.getBlockX() + Math.cos(rad) * radius);
            int z = (int) Math.floor(center.getBlockZ() + Math.sin(rad) * radius);
            sendBlock(player, original, zone.world, x, center.getBlockY(), z, Material.GLOWSTONE.createBlockData());
        }
        // Keep the outline clean without highlighting the center block to avoid confusing markers.
    }

    private void drawLine(Player player, Map<BlockKey, BlockData> original, String worldName, int x0, int z0, int x1, int z1, int y, BlockData data) {
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;

        while (true) {
            sendBlock(player, original, worldName, x0, y, z0, data);
            if (x0 == x1 && z0 == z1) break;
            int e2 = err * 2;
            if (e2 > -dz) {
                err -= dz;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z0 += sz;
            }
        }
    }

    private void sendBlock(Player player, Map<BlockKey, BlockData> original, String worldName, int x, int y, int z, BlockData data) {
        if (!player.getWorld().getName().equals(worldName)) return;
        Location loc = new Location(player.getWorld(), x, y, z);
        Block block = loc.getBlock();
        BlockKey key = new BlockKey(worldName, x, y, z);
        original.putIfAbsent(key, block.getBlockData());
        player.sendBlockChange(loc, data);
    }

    private void revert(Player player, Map<BlockKey, BlockData> original) {
        if (!player.isOnline()) return;
        for (var entry : original.entrySet()) {
            BlockKey key = entry.getKey();
            if (!player.getWorld().getName().equals(key.world())) continue;
            Location loc = new Location(player.getWorld(), key.x(), key.y(), key.z());
            player.sendBlockChange(loc, entry.getValue());
        }
    }

    private record BlockKey(String world, int x, int y, int z) { }
}
