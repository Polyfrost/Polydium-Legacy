package org.polyfrost.polydium.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.MovingBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public final class ParticleCollisions {
    private ParticleCollisions() {
    }

    public static List<Box> collect(World world, Entity entity, Box swept, Memo memo) {
        int minX = MathHelper.floor(swept.minX);
        int maxX = MathHelper.floor(swept.maxX + 1.0);
        int minY = MathHelper.floor(swept.minY) - 1;
        int maxY = MathHelper.floor(swept.maxY + 1.0);
        int minZ = MathHelper.floor(swept.minZ);
        int maxZ = MathHelper.floor(swept.maxZ + 1.0);

        WorldBorder border = world.getWorldBorder();
        boolean inside = world.isWithinBorder(border, entity);
        boolean borderStone = inside && !(minX + 1 > border.getMinX() && maxX - 1 < border.getMaxX()
                && minZ + 1 > border.getMinZ() && maxZ - 1 < border.getMaxZ());
        if (borderStone) {
            return slow(world, entity, swept, minX, minY, minZ, maxX, maxY, maxZ, border);
        }

        int minChunkX = minX >> 4, maxChunkX = (maxX - 1) >> 4;
        int minChunkZ = minZ >> 4, maxChunkZ = (maxZ - 1) >> 4;
        long stamp = BlockStamps.stamp(world, minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        if (memo.matches(minX, minY, minZ, maxX, maxY, maxZ, stamp)) {
            return null;
        }

        boolean candidates = false;
        List<Box> boxes = null;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int fromY = Math.max(minY, 0);
        int toY = Math.min(maxY - 1, 255);
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(pos.set(cx << 4, 64, cz << 4))) continue;
                WorldChunk chunk = world.getChunkAt(cx, cz);
                if (chunk.isEmpty()) continue;
                WorldChunkSection[] sections = chunk.getSections();
                int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX - 1, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ - 1, (cz << 4) + 15);
                for (int sy = fromY >> 4; sy <= toY >> 4; sy++) {
                    WorldChunkSection section = sections[sy];
                    if (section == null || section.isEmpty()) continue;
                    int y0 = Math.max(fromY, sy << 4), y1 = Math.min(toY, (sy << 4) + 15);
                    for (int y = y0; y <= y1; y++) {
                        for (int z = z0; z <= z1; z++) {
                            for (int x = x0; x <= x1; x++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                Block block = state.getBlock();
                                if (block == Blocks.AIR || y == minY && !reachesUp(block)) continue;
                                candidates = true;
                                if (boxes == null) boxes = new ArrayList<>(4);
                                block.addCollisions(world, pos.set(x, y, z), state, swept, boxes, entity);
                            }
                        }
                    }
                }
            }
        }
        if (!candidates) {
            memo.remember(minX, minY, minZ, maxX, maxY, maxZ, stamp);
        }
        return boxes == null || boxes.isEmpty() ? null : boxes;
    }

    private static boolean reachesUp(Block block) {
        return block instanceof FenceBlock || block instanceof WallBlock || block instanceof FenceGateBlock
                || block instanceof MovingBlock;
    }

    public static final class Memo {
        private long stamp = -1, epoch;
        private int minX, minY, minZ, maxX, maxY, maxZ;

        boolean matches(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long stamp) {
            return this.stamp == stamp && this.epoch == BlockStamps.epoch
                    && this.minX == minX && this.minY == minY && this.minZ == minZ
                    && this.maxX == maxX && this.maxY == maxY && this.maxZ == maxZ;
        }

        void remember(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long stamp) {
            this.stamp = stamp;
            this.epoch = BlockStamps.epoch;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    private static List<Box> slow(World world, Entity entity, Box swept, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, WorldBorder border) {
        List<Box> boxes = new ArrayList<>();
        BlockState stone = Blocks.STONE.defaultState();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (!world.isChunkLoaded(pos.set(x, 64, z))) continue;
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = border.contains(pos) ? world.getBlockState(pos) : stone;
                    state.getBlock().addCollisions(world, pos, state, swept, boxes, entity);
                }
            }
        }
        return boxes.isEmpty() ? null : boxes;
    }
}
