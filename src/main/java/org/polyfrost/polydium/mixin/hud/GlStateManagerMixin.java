package org.polyfrost.polydium.mixin.hud;

import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Inject(method = "blendFunc", at = @At("HEAD"), cancellable = true)
    private static void polydium$accumulateAlpha(int source, int destination, CallbackInfo ci) {
        if (HudCache.capturing && destination == GL11.GL_ONE_MINUS_SRC_ALPHA) {
            GlStateManager.blendFuncSeparate(source, destination, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }

    @Inject(method = "blendFuncSeparate", at = @At("HEAD"), cancellable = true)
    private static void polydium$accumulateAlphaSeparate(int sourceRgb, int destinationRgb, int sourceAlpha, int destinationAlpha, CallbackInfo ci) {
        if (HudCache.capturing && destinationRgb == GL11.GL_ONE_MINUS_SRC_ALPHA
                && (sourceAlpha != GL11.GL_ONE || destinationAlpha != GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            GlStateManager.blendFuncSeparate(sourceRgb, destinationRgb, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }

    @ModifyArg(method = "color4f", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", remap = false), index = 3)
    private static float polydium$opaqueWithoutBlend(float alpha) {
        return HudCache.capturedAlpha(alpha);
    }

    @Inject(method = "color4f", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", remap = false, shift = At.Shift.AFTER))
    private static void polydium$syncAlphaRef(float r, float g, float b, float a, CallbackInfo ci) {
        if (HudCache.capturing) HudCache.syncAlphaFunc();
    }

    @ModifyArg(method = "alphaFunc", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glAlphaFunc(IF)V", remap = false), index = 1)
    private static float polydium$scaleAlphaRef(float ref) {
        return HudCache.capturedAlphaRef(ref);
    }

    @Inject(method = {"enableBlend", "disableBlend"}, at = @At("TAIL"))
    private static void polydium$syncCapturedAlpha(CallbackInfo ci) {
        if (HudCache.capturing) HudCache.syncState();
    }
}
