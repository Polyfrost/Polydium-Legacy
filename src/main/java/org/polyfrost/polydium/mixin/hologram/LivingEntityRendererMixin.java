package org.polyfrost.polydium.mixin.hologram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ArmorStandRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected boolean renderMarker;

    @Shadow
    public abstract void renderNameTag(LivingEntity entity, double dx, double dy, double dz);

    @Inject(method = "render(Lnet/minecraft/entity/living/LivingEntity;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void polydium$renderHologram(LivingEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        if (this.renderMarker || !(entity instanceof ArmorStandEntity) || ((Object) this).getClass() != ArmorStandRenderer.class
                || !entity.isInvisibleTo(Minecraft.getInstance().player) || !polydium$isEmpty(entity.getEquipment())) {
            return;
        }
        ci.cancel();
        GlStateManager.enableAlphaTest();
        GlStateManager.depthMask(true);
        GlStateManager.activeTexture(GLX.GL_TEXTURE1);
        GlStateManager.enableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        GlStateManager.enableCull();
        this.renderNameTag(entity, x, y, z);
    }

    @Unique
    private static boolean polydium$isEmpty(ItemStack[] equipment) {
        for (ItemStack stack : equipment) {
            if (stack != null) {
                return false;
            }
        }
        return true;
    }
}
