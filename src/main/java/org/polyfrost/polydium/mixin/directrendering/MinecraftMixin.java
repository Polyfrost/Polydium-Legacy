package org.polyfrost.polydium.mixin.directrendering;

import dev.rdh.argentum.impl.Argentum;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.polyfrost.polydium.PolydiumConfig;
import org.polyfrost.polydium.client.DirectRenderingDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void polydium$defaultDirectRendering(CallbackInfo ci) {
        PolydiumConfig config = PolydiumConfig.INSTANCE;
        if (config.directRenderingUserSet || Argentum.CONFIG == null) return;
        Boolean last = config.directRenderingAutoValue;
        if (last != null && Argentum.CONFIG.directRendering != last) {
            config.directRenderingUserSet = true;
            config.save();
            return;
        }
        boolean enable = DirectRenderingDefault.shouldEnable(GL11.glGetString(GL11.GL_RENDERER));
        if (Argentum.CONFIG.directRendering != enable) {
            Argentum.CONFIG.directRendering = enable;
            Argentum.CONFIG_STORAGE.save();
        }
        if (last == null || last != enable) {
            config.directRenderingAutoValue = enable;
            config.save();
        }
    }
}
