package org.polyfrost.polydium.mixin.world;

import net.minecraft.client.world.chunk.ClientChunkCache;
import org.polyfrost.polydium.client.BlockStamps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Inject(method = "loadChunk", at = @At("RETURN"))
    private void polydium$bumpOnLoad(int chunkX, int chunkZ, CallbackInfoReturnable<?> cir) {
        BlockStamps.epoch++;
    }

    @Inject(method = "unloadChunk", at = @At("HEAD"))
    private void polydium$bumpOnUnload(int chunkX, int chunkZ, CallbackInfo ci) {
        BlockStamps.epoch++;
    }
}
