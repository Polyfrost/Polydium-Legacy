package org.polyfrost.polydium.mixin.world;

import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.polyfrost.polydium.client.BlockStamps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements BlockStamps.Stamped {
    @Shadow
    @Final
    private World world;

    @Unique
    private long polydium$stamp;

    @Override
    public long polydium$stamp() {
        return this.polydium$stamp;
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void polydium$stampBlockChange(BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null && this.world.isClient) {
            this.polydium$stamp = BlockStamps.next();
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void polydium$stampChunkData(byte[] data, int sections, boolean full, CallbackInfo ci) {
        if (this.world.isClient) {
            this.polydium$stamp = BlockStamps.next();
        }
    }
}
