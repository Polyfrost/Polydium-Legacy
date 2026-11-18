package org.polyfrost.polydium.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.GameRenderer;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;render(F)V"))
    private void polydium$cacheHud(GameGui gui, float tickDelta, Operation<Void> original) {
        if (!HudCache.render(gui, tickDelta, () -> original.call(gui, tickDelta))) {
            original.call(gui, tickDelta);
        }
    }
}
