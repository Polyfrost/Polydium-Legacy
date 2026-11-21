package org.polyfrost.polydium.mixin.border;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Unique
    private double polydium$minX, polydium$maxX, polydium$minZ, polydium$maxZ;

    @Inject(method = "renderWorldBorder", at = @At("HEAD"))
    private void polydium$resetBorder(Entity camera, float tickDelta, CallbackInfo ci) {
        this.polydium$minX = this.polydium$maxX = this.polydium$minZ = this.polydium$maxZ = Double.NaN;
    }

    @WrapOperation(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;getMinX()D"))
    private double polydium$minX(WorldBorder border, Operation<Double> original) {
        if (Double.isNaN(this.polydium$minX)) this.polydium$minX = original.call(border);
        return this.polydium$minX;
    }

    @WrapOperation(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;getMaxX()D"))
    private double polydium$maxX(WorldBorder border, Operation<Double> original) {
        if (Double.isNaN(this.polydium$maxX)) this.polydium$maxX = original.call(border);
        return this.polydium$maxX;
    }

    @WrapOperation(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;getMinZ()D"))
    private double polydium$minZ(WorldBorder border, Operation<Double> original) {
        if (Double.isNaN(this.polydium$minZ)) this.polydium$minZ = original.call(border);
        return this.polydium$minZ;
    }

    @WrapOperation(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;getMaxZ()D"))
    private double polydium$maxZ(WorldBorder border, Operation<Double> original) {
        if (Double.isNaN(this.polydium$maxZ)) this.polydium$maxZ = original.call(border);
        return this.polydium$maxZ;
    }
}
