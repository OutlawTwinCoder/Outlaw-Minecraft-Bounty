package com.outlaw.bounties.manager;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

public class SpawnManager {
    private final BountyPlugin plugin;
    private final Random rng = new Random();

    public SpawnManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public Location pickRandomSpawn(World world) {
        if (world == null) return null;
        int attempts = plugin.getConfig().getInt("spawn.max_attempts", 80);

        var border = world.getWorldBorder();
        double size = border.getSize() / 2.0 - 64; // margin
        var center = border.getCenter();

        for (int i=0;i<attempts;i++) {
            double x = center.getX() + (rng.nextDouble()*2-1) * size;
            double z = center.getZ() + (rng.nextDouble()*2-1) * size;
            Location loc = new Location(world, x, world.getHighestBlockYAt((int) x, (int) z) + 1.0, z);

            if (plugin.getConfig().getBoolean("spawn.avoid_liquids", true)) {
                Block bBelow = loc.clone().subtract(0,1,0).getBlock();
                if (bBelow.isLiquid() || bBelow.getType() == Material.WATER || bBelow.getType() == Material.LAVA) continue;
            }

            if (plugin.safeZoneManager().isSafe(loc)) continue;
            return loc;
        }
        return null;
    }
}
