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
        if (config.directRenderingDefaultApplied || Argentum.CONFIG == null) return;
        if (DirectRenderingDefault.shouldEnable(GL11.glGetString(GL11.GL_RENDERER))) {
            Argentum.CONFIG.directRendering = true;
            Argentum.CONFIG_STORAGE.save();
        }
        config.directRenderingDefaultApplied = true;
        config.save();
    }
}
