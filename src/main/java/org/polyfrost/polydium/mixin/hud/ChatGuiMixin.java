package org.polyfrost.polydium.mixin.hud;

import net.minecraft.client.gui.chat.ChatGui;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatGui.class)
public abstract class ChatGuiMixin {
    @Inject(method = {"scroll", "resetScroll"}, at = @At("TAIL"))
    private void polydium$recaptureOnScroll(CallbackInfo ci) {
        HudCache.invalidate();
    }
}
