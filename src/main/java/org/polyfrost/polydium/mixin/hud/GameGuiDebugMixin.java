package org.polyfrost.polydium.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.overlay.DebugOverlay;
import net.minecraft.client.render.Window;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class GameGuiDebugMixin {
    @Shadow
    @Final
    private DebugOverlay debugOverlay;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/overlay/DebugOverlay;render(Lnet/minecraft/client/render/Window;)V"))
    private void polydium$markDebugOverlay(float tickDelta, CallbackInfo ci) {
        if (HudCache.capturing) HudCache.debugOverlayReached(this.debugOverlay);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/overlay/DebugOverlay;render(Lnet/minecraft/client/render/Window;)V"))
    private void polydium$deferDebugOverlay(DebugOverlay overlay, Window window, Operation<Void> original) {
        if (!HudCache.capturing || !HudCache.deferDebugOverlay(original)) original.call(overlay, window);
    }
}
