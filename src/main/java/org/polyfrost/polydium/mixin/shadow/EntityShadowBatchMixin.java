package org.polyfrost.polydium.mixin.shadow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.argentum.impl.render.entity.EntityShadowBatch;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityShadowBatch.class, remap = false)
public abstract class EntityShadowBatchMixin {
    @Shadow
    @Final
    private Long2ReferenceOpenHashMap<?> blocks;

    @Shadow
    @Final
    private Long2LongOpenHashMap light;

    @Unique
    private World polydium$cachedWorld;

    @Unique
    private long polydium$cachedTick;

    @Unique
    private boolean polydium$reuse;

    @Inject(method = "beginFrame", at = @At("HEAD"))
    private void polydium$checkTick(CallbackInfo ci) {
        World world = Minecraft.getInstance().world;
        long tick = world == null ? 0L : world.getTime();
        this.polydium$reuse = world == this.polydium$cachedWorld && tick == this.polydium$cachedTick;
        this.polydium$cachedWorld = world;
        this.polydium$cachedTick = tick;
    }

    @WrapOperation(method = "beginFrame", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ReferenceOpenHashMap;clear()V"))
    private void polydium$keepBlocks(Long2ReferenceOpenHashMap<?> blocks, Operation<Void> original) {
        if (!this.polydium$reuse) {
            original.call(blocks);
        }
    }

    @WrapOperation(method = "beginFrame", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2LongOpenHashMap;clear()V"))
    private void polydium$keepLight(Long2LongOpenHashMap light, Operation<Void> original) {
        if (!this.polydium$reuse) {
            original.call(light);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void polydium$dropWorld(CallbackInfo ci) {
        this.blocks.clear();
        this.light.clear();
        this.polydium$cachedWorld = null;
    }
}
