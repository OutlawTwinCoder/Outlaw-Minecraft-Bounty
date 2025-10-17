package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class SafeZoneSelectionManager implements Listener {
    private final BountyPlugin plugin;
    private final Map<UUID, SelectionSession> sessions = new HashMap<>();

    private enum Mode {
        CUBOID,
        POLYGON
    }

    private static class SelectionSession {
        final Mode mode;
        final String world;
        final List<SafeZoneManager.BlockPos> points = new ArrayList<>();

        SelectionSession(Mode mode, String world) {
            this.mode = mode;
            this.world = world;
        }
    }

    public SafeZoneSelectionManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public void beginCuboid(Player player) {
        SelectionSession session = new SelectionSession(Mode.CUBOID, player.getWorld().getName());
        sessions.put(player.getUniqueId(), session);
        player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_select_cuboid_first"));
    }

    public void beginPolygon(Player player) {
        SelectionSession session = new SelectionSession(Mode.POLYGON, player.getWorld().getName());
        sessions.put(player.getUniqueId(), session);
        player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_select_circle_start"));
    }

    public void cancel(Player player, String reasonKey) {
        sessions.remove(player.getUniqueId());
        if (reasonKey != null) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr(reasonKey));
        }
    }

    public boolean completePolygon(Player player) {
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null || session.mode != Mode.POLYGON) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr("messages.safezone_circle_no_session"));
            return false;
        }
        if (session.points.size() < 5) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr("messages.safezone_circle_not_enough"));
            return false;
        }
        if (!player.getWorld().getName().equals(session.world)) {
            cancel(player, "messages.safezone_select_other_world");
            return false;
        }
        SafeZoneManager.Zone zone = plugin.safeZoneManager().addPolygon(player.getWorld(), session.points);
        sessions.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.safezone_created", Map.of(
                "id", String.valueOf(zone.id),
                "type", plugin.locale().tr("messages.safezone_type_circle")
        )));
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getWorld().getName().equals(session.world)) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr("messages.safezone_select_other_world"));
            return;
        }

        event.setCancelled(true);

        SafeZoneManager.BlockPos pos = new SafeZoneManager.BlockPos(block.getX(), block.getY(), block.getZ());

        switch (session.mode) {
            case CUBOID -> handleCuboid(player, session, pos);
            case POLYGON -> handlePolygon(player, session, pos);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void handleCuboid(Player player, SelectionSession session, SafeZoneManager.BlockPos pos) {
        if (session.points.isEmpty()) {
            session.points.add(pos);
            player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_select_cuboid_second"));
            return;
        }

        SafeZoneManager.BlockPos first = session.points.get(0);
        if (!player.getWorld().getName().equals(session.world)) {
            cancel(player, "messages.safezone_select_other_world");
            return;
        }
        SafeZoneManager.Zone zone = plugin.safeZoneManager().addCuboid(player.getWorld(), first, pos);
        sessions.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + plugin.locale().tr("messages.safezone_created", Map.of(
                "id", String.valueOf(zone.id),
                "type", plugin.locale().tr("messages.safezone_type_square")
        )));
    }

    private void handlePolygon(Player player, SelectionSession session, SafeZoneManager.BlockPos pos) {
        if (session.points.size() >= 8) {
            player.sendMessage(ChatColor.RED + plugin.locale().tr("messages.safezone_circle_max"));
            return;
        }
        session.points.add(pos);
        int count = session.points.size();
        player.sendMessage(ChatColor.YELLOW + plugin.locale().tr("messages.safezone_circle_progress", Map.of(
                "count", String.valueOf(count)
        )));
        if (count >= 5) {
            player.sendMessage(ChatColor.GOLD + plugin.locale().tr("messages.safezone_circle_ready"));
        }
    }
}
