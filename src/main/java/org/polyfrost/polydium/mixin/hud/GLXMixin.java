package org.polyfrost.polydium.mixin.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GLX;
import org.lwjgl.opengl.GL30;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GLX.class)
public abstract class GLXMixin {
    @ModifyVariable(method = "bindFramebuffer", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int polydium$keepHudTarget(int framebuffer, int target) {
        if (!HudCache.capturing || target == GL30.GL_READ_FRAMEBUFFER) return framebuffer;
        if (framebuffer == 0 || framebuffer == Minecraft.getInstance().getRenderTarget().frameBufferId) return HudCache.captureFramebuffer();
        return framebuffer;
    }
}
