package org.polyfrost.polydium.mixin.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.pipeline.RenderTarget;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @ModifyArg(method = "bindWrite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GLX;bindFramebuffer(II)V"), index = 1)
    private int polydium$keepHudTarget(int framebuffer) {
        return HudCache.capturing && (Object) this == Minecraft.getInstance().getRenderTarget() ? HudCache.captureFramebuffer() : framebuffer;
    }
}
