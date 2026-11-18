package org.polyfrost.polydium.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.polyfrost.polydium.client.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameGui.class)
public abstract class GameGuiMixin implements HudCache.Gui {
    @Shadow
    private void renderVignette(float brightness, Window window) {
    }

    @Shadow
    private void renderPumpkinOverlay(Window window) {
    }

    @Shadow
    private void renderNauseaOverlay(float portalTime, Window window) {
    }

    @Shadow
    protected abstract boolean hasCrosshair();

    @Override
    public void polydium$renderVignette(float brightness, Window window) {
        this.renderVignette(brightness, window);
    }

    @Override
    public void polydium$renderPumpkinOverlay(Window window) {
        this.renderPumpkinOverlay(window);
    }

    @Override
    public void polydium$renderNauseaOverlay(float portalTime, Window window) {
        this.renderNauseaOverlay(portalTime, window);
    }

    @Override
    public boolean polydium$hasCrosshair() {
        return this.hasCrosshair();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;renderVignette(FLnet/minecraft/client/render/Window;)V"))
    private void polydium$skipCachedVignette(GameGui gui, float brightness, Window window, Operation<Void> original) {
        if (HudCache.capturing) {
            GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            return;
        }
        original.call(gui, brightness, window);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;renderPumpkinOverlay(Lnet/minecraft/client/render/Window;)V"))
    private void polydium$skipCachedPumpkin(GameGui gui, Window window, Operation<Void> original) {
        if (!HudCache.capturing) original.call(gui, window);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;renderNauseaOverlay(FLnet/minecraft/client/render/Window;)V"))
    private void polydium$skipCachedPortal(GameGui gui, float portalTime, Window window, Operation<Void> original) {
        if (!HudCache.capturing) original.call(gui, portalTime, window);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;hasCrosshair()Z"))
    private boolean polydium$skipCachedCrosshair(GameGui gui, Operation<Boolean> original) {
        return !HudCache.capturing && original.call(gui);
    }
}
