package org.polyfrost.polydium.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GameGui;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GameGui.class, priority = 10000)
public abstract class GameGuiCrosshairMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;hasCrosshair()Z"))
    private boolean polydium$recordCrosshair(GameGui gui, Operation<Boolean> original) {
        if (!HudCache.capturing) return original.call(gui);
        HudCache.recordCrosshair(gui, () -> original.call(gui));
        return false;
    }
}
