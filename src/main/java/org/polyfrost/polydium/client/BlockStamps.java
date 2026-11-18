package org.polyfrost.polydium.client;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class BlockStamps {
    private static long counter;
    public static long epoch;

    private BlockStamps() {
    }

    public static long next() {
        return ++counter;
    }

    public static long stamp(World world, Box box) {
        return stamp(world, MathHelper.floor(box.minX - 1.0) >> 4, MathHelper.floor(box.minZ - 1.0) >> 4,
                MathHelper.floor(box.maxX + 1.0) >> 4, MathHelper.floor(box.maxZ + 1.0) >> 4);
    }

    public static long stamp(World world, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        long max = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                max = Math.max(max, ((Stamped) world.getChunkAt(cx, cz)).polydium$stamp());
            }
        }
        return max;
    }

    public interface Stamped {
        long polydium$stamp();
    }
}
