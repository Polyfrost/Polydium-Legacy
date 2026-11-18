package org.polyfrost.polydium.mixin.particle;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.WorldGeneratorType;
import org.polyfrost.polydium.client.ParticleCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleMixin extends Entity {
    private ParticleMixin(World world) {
        super(world);
    }

    @Unique
    private final ParticleCollisions.Memo polydium$memo = new ParticleCollisions.Memo();

    @Override
    public void move(double dx, double dy, double dz) {
        if (this.noClip || this.world.getGeneratorType() == WorldGeneratorType.DEBUG_ALL_BLOCK_STATES) {
            super.move(dx, dy, dz);
            return;
        }

        if (this.inCobweb) {
            this.inCobweb = false;
            dx *= 0.25;
            dy *= 0.05;
            dz *= 0.25;
            this.velocityX = 0.0;
            this.velocityY = 0.0;
            this.velocityZ = 0.0;
        }

        double wantX = dx, wantY = dy, wantZ = dz;
        Box shape = this.getShape();
        List<Box> boxes = ParticleCollisions.collect(this.world, this, shape.expanded(dx, dy, dz), this.polydium$memo);
        if (boxes == null) {
            shape = shape.moved(dx, dy, dz);
        } else {
            for (Box box : boxes) dy = box.intersectY(shape, dy);
            shape = shape.moved(0.0, dy, 0.0);
            for (Box box : boxes) dx = box.intersectX(shape, dx);
            shape = shape.moved(dx, 0.0, 0.0);
            for (Box box : boxes) dz = box.intersectZ(shape, dz);
            shape = shape.moved(0.0, 0.0, dz);
        }
        this.setShape(shape);
        this.x = (shape.minX + shape.maxX) / 2.0;
        this.y = shape.minY;
        this.z = (shape.minZ + shape.maxZ) / 2.0;

        this.collidingHorizontally = wantX != dx || wantZ != dz;
        this.collidingVertically = wantY != dy;
        this.onGround = this.collidingVertically && wantY < 0.0;
        this.colliding = this.collidingHorizontally || this.collidingVertically;
        if (wantX != dx) this.velocityX = 0.0;
        if (wantZ != dz) this.velocityZ = 0.0;
        if (wantY != dy) {
            this.polydium$landingBlock().beforeCollision(this.world, this);
        }
        this.checkBlockCollisions();
    }

    @Unique
    private Block polydium$landingBlock() {
        BlockPos pos = new BlockPos(MathHelper.floor(this.x), MathHelper.floor(this.y - 0.2F), MathHelper.floor(this.z));
        Block block = this.world.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.AIR) {
            Block below = this.world.getBlockState(pos.down()).getBlock();
            if (below instanceof FenceBlock || below instanceof WallBlock || below instanceof FenceGateBlock) {
                return below;
            }
        }
        return block;
    }
}
