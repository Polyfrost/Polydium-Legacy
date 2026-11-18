package org.polyfrost.polydium.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.polyfrost.polydium.client.BlockStamps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private static final int LAVA = 1, WALL = 2, FIRE = 4, SUBMERGED = 8, NO_WATER = 16, NO_CONTACT = 32;

    @Shadow
    public World world;

    @Shadow
    public boolean noClip;

    @Shadow
    protected boolean inWater;

    @Shadow
    public abstract Box getShape();

    @Unique
    private double polydium$minX = Double.NaN, polydium$minY, polydium$minZ, polydium$maxX, polydium$maxY, polydium$maxZ;

    @Unique
    private long polydium$stamp, polydium$epoch;

    @Unique
    private int polydium$known, polydium$values;

    @Unique
    private boolean polydium$validate() {
        if (this.world == null || !this.world.isClient || (Object) this instanceof LocalClientPlayerEntity) {
            return false;
        }
        Box box = this.getShape();
        long stamp = BlockStamps.stamp(this.world, box);
        if (box.minX != this.polydium$minX || box.minY != this.polydium$minY || box.minZ != this.polydium$minZ
                || box.maxX != this.polydium$maxX || box.maxY != this.polydium$maxY || box.maxZ != this.polydium$maxZ
                || stamp != this.polydium$stamp || BlockStamps.epoch != this.polydium$epoch) {
            this.polydium$minX = box.minX;
            this.polydium$minY = box.minY;
            this.polydium$minZ = box.minZ;
            this.polydium$maxX = box.maxX;
            this.polydium$maxY = box.maxY;
            this.polydium$maxZ = box.maxZ;
            this.polydium$stamp = stamp;
            this.polydium$epoch = BlockStamps.epoch;
            this.polydium$known = 0;
        }
        return true;
    }

    @Unique
    private boolean polydium$cached(int bit, Operation<Boolean> original) {
        if (!this.polydium$validate()) {
            return original.call();
        }
        if ((this.polydium$known & bit) == 0) {
            boolean value = original.call();
            this.polydium$known |= bit;
            this.polydium$values = value ? this.polydium$values | bit : this.polydium$values & ~bit;
        }
        return (this.polydium$values & bit) != 0;
    }

    @WrapMethod(method = "isInLava")
    private boolean polydium$cacheLava(Operation<Boolean> original) {
        return this.polydium$cached(LAVA, original);
    }

    @WrapMethod(method = "isInWall")
    private boolean polydium$cacheInWall(Operation<Boolean> original) {
        if (this.noClip || (Object) this instanceof PlayerEntity) {
            return original.call();
        }
        return this.polydium$cached(WALL, original);
    }

    @WrapMethod(method = "isSubmergedIn")
    private boolean polydium$cacheSubmerged(Material liquid, Operation<Boolean> original) {
        if (liquid != Material.WATER || (Object) this instanceof PlayerEntity) {
            return original.call(liquid);
        }
        return this.polydium$cached(SUBMERGED, args -> original.call(liquid));
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;containsFireSource(Lnet/minecraft/util/math/Box;)Z"))
    private boolean polydium$cacheFire(World world, Box bounds, Operation<Boolean> original) {
        return this.polydium$cached(FIRE, args -> original.call(world, bounds));
    }

    @WrapMethod(method = "checkWaterCollisions")
    private boolean polydium$cacheDry(Operation<Boolean> original) {
        if (!this.polydium$validate()) {
            return original.call();
        }
        if ((this.polydium$known & NO_WATER) != 0) {
            this.inWater = false;
            return false;
        }
        boolean wet = original.call();
        if (!wet) {
            this.polydium$known |= NO_WATER;
        }
        return wet;
    }

    @WrapMethod(method = "checkBlockCollisions")
    private void polydium$cacheNoContact(Operation<Void> original) {
        if (!this.polydium$validate()) {
            original.call();
            return;
        }
        if ((this.polydium$known & NO_CONTACT) != 0) {
            return;
        }
        original.call();
        if (this.polydium$touchesOnlyAir()) {
            this.polydium$known |= NO_CONTACT;
        }
    }

    @Unique
    private boolean polydium$touchesOnlyAir() {
        Box box = this.getShape();
        BlockPos min = new BlockPos(box.minX + 0.001, box.minY + 0.001, box.minZ + 0.001);
        BlockPos max = new BlockPos(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001);
        if (!this.world.isAreaLoaded(min, max)) {
            return true;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (this.world.getBlockState(pos.set(x, y, z)).getBlock() != Blocks.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
