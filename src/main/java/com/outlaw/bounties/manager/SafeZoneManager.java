package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class SafeZoneManager {
    private final BountyPlugin plugin;

    public enum ZoneType {
        SPHERE,
        CUBOID,
        POLYGON
    }

    public static class BlockPos {
        public final int x;
        public final int y;
        public final int z;

        public BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Point2D {
        public final double x;
        public final double z;

        public Point2D(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    public static class Zone {
        public int id;
        public String world;
        public ZoneType type;
        public double x,y,z,radius;
        public int minX,maxX,minZ,maxZ;
        public int minY,maxY;
        public List<Point2D> polygon = new ArrayList<>();
        public List<BlockPos> anchors = new ArrayList<>();

        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equals(world)) return false;
            int bx = loc.getBlockX();
            int by = loc.getBlockY();
            int bz = loc.getBlockZ();

            if (type == ZoneType.SPHERE) {
                double dx = loc.getX() - x;
                double dy = loc.getY() - y;
                double dz = loc.getZ() - z;
                return Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius;
            }

            if (by < minY || by > maxY) return false;

            if (type == ZoneType.CUBOID) {
                return bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ;
            }

            if (type == ZoneType.POLYGON) {
                if (polygon.isEmpty()) return false;
                if (bx < minX || bx > maxX || bz < minZ || bz > maxZ) return false;
                boolean inside = false;
                for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
                    Point2D pi = polygon.get(i);
                    Point2D pj = polygon.get(j);
                    double diff = pj.z - pi.z;
                    if (Math.abs(diff) < 1e-6) continue;
                    boolean intersect = ((pi.z > bz) != (pj.z > bz)) &&
                            (bx < (pj.x - pi.x) * (bz - pi.z) / diff + pi.x);
                    if (intersect) inside = !inside;
                }
                return inside;
            }
            return false;
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
                    String typeStr = String.valueOf(map.getOrDefault("type", "sphere"));
                    try {
                        z.type = ZoneType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                    } catch (Exception ex) {
                        z.type = ZoneType.SPHERE;
                    }
                    if (z.type == ZoneType.SPHERE) {
                        z.x = toDouble(map.get("x"), 0);
                        z.y = toDouble(map.get("y"), 64);
                        z.z = toDouble(map.get("z"), 0);
                        z.radius = toDouble(map.get("radius"), 20);
                    } else {
                        z.minX = toInt(map.get("minX"), 0);
                        z.maxX = toInt(map.get("maxX"), 0);
                        z.minZ = toInt(map.get("minZ"), 0);
                        z.maxZ = toInt(map.get("maxZ"), 0);
                        z.minY = toInt(map.get("minY"), -64);
                        z.maxY = toInt(map.get("maxY"), 320);
                        Object anchorsObj = map.get("anchors");
                        if (anchorsObj instanceof List<?> listAnchors) {
                            for (Object ao : listAnchors) {
                                if (ao instanceof Map<?,?> am) {
                                    int ax = toInt(am.get("x"), 0);
                                    int ay = toInt(am.get("y"), 0);
                                    int az = toInt(am.get("z"), 0);
                                    z.anchors.add(new BlockPos(ax, ay, az));
                                }
                            }
                        }
                        if (z.type == ZoneType.POLYGON) {
                            Object polyObj = map.get("polygon");
                            if (polyObj instanceof List<?> listPoly) {
                                for (Object po : listPoly) {
                                    if (po instanceof Map<?,?> pm) {
                                        double px = toDouble(pm.get("x"), 0);
                                        double pz = toDouble(pm.get("z"), 0);
                                        z.polygon.add(new Point2D(px, pz));
                                    }
                                }
                            }
                            if (z.polygon.isEmpty() && !z.anchors.isEmpty()) {
                                for (BlockPos bp : z.anchors) {
                                    z.polygon.add(new Point2D(bp.x + 0.5, bp.z + 0.5));
                                }
                            }
                        }
                    }
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

    public Zone addSphere(Location center, double radius) {
        Zone z = new Zone();
        z.id = nextId++;
        z.world = center.getWorld().getName();
        z.type = ZoneType.SPHERE;
        z.x = center.getX();
        z.y = center.getY();
        z.z = center.getZ();
        z.radius = radius;
        zones.put(z.id, z);
        persist();
        return z;
    }

    public Zone addCuboid(World world, BlockPos first, BlockPos second) {
        Zone z = new Zone();
        z.id = nextId++;
        z.world = world.getName();
        z.type = ZoneType.CUBOID;
        z.minX = Math.min(first.x, second.x);
        z.maxX = Math.max(first.x, second.x);
        z.minZ = Math.min(first.z, second.z);
        z.maxZ = Math.max(first.z, second.z);
        z.minY = world.getMinHeight();
        z.maxY = world.getMaxHeight();
        z.anchors.add(first);
        z.anchors.add(second);
        zones.put(z.id, z);
        persist();
        return z;
    }

    public Zone addPolygon(World world, List<BlockPos> anchorBlocks) {
        Zone z = new Zone();
        z.id = nextId++;
        z.world = world.getName();
        z.type = ZoneType.POLYGON;
        z.minY = world.getMinHeight();
        z.maxY = world.getMaxHeight();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos bp : anchorBlocks) {
            z.anchors.add(bp);
            minX = Math.min(minX, bp.x);
            maxX = Math.max(maxX, bp.x);
            minZ = Math.min(minZ, bp.z);
            maxZ = Math.max(maxZ, bp.z);
            z.polygon.add(new Point2D(bp.x + 0.5, bp.z + 0.5));
        }
        z.minX = minX;
        z.maxX = maxX;
        z.minZ = minZ;
        z.maxZ = maxZ;
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

    public Optional<Zone> findById(int id) {
        return Optional.ofNullable(zones.get(id));
    }

    private void persist() {
        var cfg = plugin.zonesCfg();
        java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
        for (Zone z : zones.values()) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", z.id);
            map.put("world", z.world);
            map.put("type", z.type.name().toLowerCase(Locale.ROOT));
            switch (z.type) {
                case SPHERE -> {
                    map.put("x", z.x);
                    map.put("y", z.y);
                    map.put("z", z.z);
                    map.put("radius", z.radius);
                }
                case CUBOID -> {
                    map.put("minX", z.minX);
                    map.put("maxX", z.maxX);
                    map.put("minZ", z.minZ);
                    map.put("maxZ", z.maxZ);
                    map.put("minY", z.minY);
                    map.put("maxY", z.maxY);
                    map.put("anchors", serializeAnchors(z.anchors));
                }
                case POLYGON -> {
                    map.put("minX", z.minX);
                    map.put("maxX", z.maxX);
                    map.put("minZ", z.minZ);
                    map.put("maxZ", z.maxZ);
                    map.put("minY", z.minY);
                    map.put("maxY", z.maxY);
                    map.put("anchors", serializeAnchors(z.anchors));
                    java.util.List<java.util.Map<String,Object>> poly = new ArrayList<>();
                    for (Point2D point : z.polygon) {
                        java.util.Map<String,Object> pp = new HashMap<>();
                        pp.put("x", point.x);
                        pp.put("z", point.z);
                        poly.add(pp);
                    }
                    map.put("polygon", poly);
                }
            }
            list.add(map);
        }
        cfg.set("zones", list);
        plugin.saveZones();
    }

    private List<Map<String,Object>> serializeAnchors(List<BlockPos> anchors) {
        List<Map<String,Object>> list = new ArrayList<>();
        for (BlockPos bp : anchors) {
            Map<String,Object> map = new HashMap<>();
            map.put("x", bp.x);
            map.put("y", bp.y);
            map.put("z", bp.z);
            list.add(map);
        }
        return list;
    }
}
