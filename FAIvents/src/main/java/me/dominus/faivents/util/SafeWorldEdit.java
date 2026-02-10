package me.dominus.faivents.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeWorldEdit {

    private SafeWorldEdit() {
    }

    public static void setBlock(Location loc, Material mat) {
        if (loc == null || mat == null) {
            return;
        }
        Block b = loc.getBlock();
        b.setType(mat, false);
    }

    public static Location findNearestAir(Location center, int radius, int height) {
        if (center == null) {
            return null;
        }
        World w = center.getWorld();
        if (w == null) {
            return null;
        }
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = 0; dy <= height; dy++) {
                        int x = cx + dx;
                        int y = cy + dy;
                        int z = cz + dz;
                        Block base = w.getBlockAt(x, y, z);
                        Block above = w.getBlockAt(x, y + 1, z);
                        Block below = w.getBlockAt(x, y - 1, z);
                        if (base.getType() == Material.AIR && above.getType() == Material.AIR && below.getType().isSolid()) {
                            return new Location(w, x + 0.5, y, z + 0.5);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Location getHighestSafe(World world, int x, int z) {
        if (world == null) {
            return null;
        }
        int y = world.getHighestBlockYAt(x, z);
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        if (loc.getBlock().getType() == Material.AIR) {
            return loc;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
}

