package org.polyfrost.polydium.client;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.living.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;
import org.polyfrost.polydium.PolydiumConfig;

public final class HudCache {
    public static boolean capturing;

    private static RenderTarget target;
    private static boolean failed;
    private static boolean dirty = true;
    private static long lastCapture;
    private static int lastScale;

    private HudCache() {
    }

    public interface Gui {
        void polydium$renderVignette(float brightness, Window window);

        void polydium$renderPumpkinOverlay(Window window);

        void polydium$renderNauseaOverlay(float portalTime, Window window);

        boolean polydium$hasCrosshair();
    }

    public static boolean render(GameGui gui, float tickDelta, Runnable draw) {
        Minecraft minecraft = Minecraft.getInstance();
        int fps = PolydiumConfig.INSTANCE.hudFps;
        if (failed || fps <= 0 || !GLX.useFbo() || minecraft.screen != null) {
            dirty = true;
            return false;
        }

        Window window = new Window(minecraft);
        try {
            if (target == null) {
                target = new RenderTarget(minecraft.width, minecraft.height, true);
                target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                dirty = true;
            } else if (target.width != minecraft.width || target.height != minecraft.height) {
                target.resize(minecraft.width, minecraft.height);
                dirty = true;
            }
        } catch (RuntimeException e) {
            LogManager.getLogger("Polydium").error("Could not create HUD framebuffer, HUD caching disabled", e);
            failed = true;
            minecraft.getRenderTarget().bindWrite(true);
            return false;
        }
        if (window.getScale() != lastScale) {
            lastScale = window.getScale();
            dirty = true;
        }

        long now = System.nanoTime();
        long interval = 1_000_000_000L / fps;
        long elapsed = now - lastCapture;
        if (dirty || elapsed >= interval) {
            lastCapture = dirty || elapsed >= 2 * interval ? now : lastCapture + interval;
            dirty = false;
            GlStateManager.colorMask(true, true, true, true);
            target.clear();
            target.bindWrite(true);
            capturing = true;
            try {
                draw.run();
            } finally {
                capturing = false;
                minecraft.getRenderTarget().bindWrite(true);
                GlStateManager.colorMask(true, true, true, !minecraft.options.anaglyph);
            }
        }

        minecraft.gameRenderer.setupGuiState();
        GlStateManager.enableBlend();
        if (Minecraft.isFancyGraphicsEnabled()) {
            ((Gui) gui).polydium$renderVignette(minecraft.player.getBrightness(tickDelta), window);
        }
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        ItemStack helmet = minecraft.player.inventory.getArmor(3);
        if (minecraft.options.perspective == 0 && helmet != null && helmet.getItem() == Item.byBlock(Blocks.PUMPKIN)) {
            ((Gui) gui).polydium$renderPumpkinOverlay(window);
        }
        if (!minecraft.player.hasStatusEffect(StatusEffect.NAUSEA)) {
            float portalTime = minecraft.player.lastPortalTime + (minecraft.player.portalTime - minecraft.player.lastPortalTime) * tickDelta;
            if (portalTime > 0.0F) {
                ((Gui) gui).polydium$renderNauseaOverlay(portalTime, window);
            }
        }
        if (((Gui) gui).polydium$hasCrosshair()) {
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            minecraft.getTextureManager().bind(GuiElement.ICONS_LOCATION);
            GlStateManager.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.enableAlphaTest();
            gui.drawTexture(window.getWidth() / 2 - 7, window.getHeight() / 2 - 7, 0, 0, 16, 16);
        }
        composite(window);
        return true;
    }

    public static int captureFramebuffer() {
        return target.frameBufferId;
    }

    private static void composite(Window window) {
        GlStateManager.disableAlphaTest();
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        target.bindRead();

        double width = window.getScaledWidth();
        double height = window.getScaledHeight();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(0.0, height, 0.0).texture(0.0, 0.0).nextVertex();
        buffer.vertex(width, height, 0.0).texture(1.0, 0.0).nextVertex();
        buffer.vertex(width, 0.0, 0.0).texture(1.0, 1.0).nextVertex();
        buffer.vertex(0.0, 0.0, 0.0).texture(0.0, 1.0).nextVertex();
        tesselator.end();

        target.unbindRead();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.enableAlphaTest();
    }
}
