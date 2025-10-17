package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Location;

import java.util.*;

public class SafeZoneManager {
    private final BountyPlugin plugin;

    public static class Zone {
        public int id;
        public String world;
        public double x,y,z,radius;
        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equals(world)) return false;
            double dx = loc.getX()-x, dy = loc.getY()-y, dz = loc.getZ()-z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz) <= radius;
        }
    }

    private final Map<Integer, Zone> zones = new LinkedHashMap<>();
    private int nextId = 1;

    public SafeZoneManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        zones.clear();
        var cfg = plugin.zonesCfg();
        var list = cfg.getList("zones");
        if (list != null) {
            int max = 0;
            for (Object o : list) {
                if (o instanceof Map<?,?> raw) {
                    Map<String, Object> map = new HashMap<>();
                    for (var e : raw.entrySet()) {
                        if (e.getKey() != null) map.put(e.getKey().toString(), e.getValue());
                    }
                    Zone z = new Zone();
                    z.id = toInt(map.get("id"), 0);
                    z.world = String.valueOf(map.getOrDefault("world","world"));
                    z.x = toDouble(map.get("x"), 0);
                    z.y = toDouble(map.get("y"), 64);
                    z.z = toDouble(map.get("z"), 0);
                    z.radius = toDouble(map.get("radius"), 20);
                    zones.put(z.id, z);
                    max = Math.max(max, z.id);
                }
            }
            nextId = max + 1;
        }
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }
    private double toDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }

    public Collection<Zone> all() { return zones.values(); }

    public Zone add(Location center, double radius) {
        Zone z = new Zone();
        z.id = nextId++;
        z.world = center.getWorld().getName();
        z.x = center.getX();
        z.y = center.getY();
        z.z = center.getZ();
        z.radius = radius;
        zones.put(z.id, z);
        persist();
        return z;
    }

    public boolean remove(int id) {
        if (zones.remove(id) != null) {
            persist();
            return true;
        }
        return false;
    }

    public boolean isSafe(Location loc) {
        for (Zone z : zones.values()) {
            if (z.contains(loc)) return true;
        }
        return false;
    }

    private void persist() {
        var cfg = plugin.zonesCfg();
        java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
        for (Zone z : zones.values()) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", z.id);
            map.put("world", z.world);
            map.put("x", z.x);
            map.put("y", z.y);
            map.put("z", z.z);
            map.put("radius", z.radius);
            list.add(map);
        }
        cfg.set("zones", list);
        plugin.saveZones();
    }
}
