package org.polyfrost.polydium.mixin.glmirror;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.argentum.impl.render.environment.EndPortalRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.polyfrost.polydium.client.GlStateMirror;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EndPortalRenderer.class, remap = false)
public abstract class EndPortalRendererMixin {
    @Shadow @Final private Matrix4f nextModelView;
    @Shadow @Final private Matrix4f nextProjection;

    @Inject(method = "capture", at = @At(value = "INVOKE", target = "Ldev/rdh/argentum/impl/render/environment/EndPortalRenderer;readProjection()Z"))
    private void polydium$mirroredMatrices(CallbackInfoReturnable<Boolean> cir) {
        this.nextModelView.set(GlStateMirror.matrix(GlStateMirror.MODELVIEW_MATRIX));
        this.nextProjection.set(GlStateMirror.matrix(GlStateMirror.PROJECTION_MATRIX));
    }

    @WrapOperation(method = "fixedFunction", at = @At(value = "INVOKE", target = "Ldev/rdh/argentum/impl/render/environment/EndPortalRenderer;readMatrix(ILorg/joml/Matrix4f;)V"), require = 2)
    private void polydium$skipMatrixRead(EndPortalRenderer renderer, int parameter, Matrix4f out, Operation<Void> original) {
    }

    @WrapOperation(method = "flush", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private int polydium$mirroredState(int parameter, Operation<Integer> original) {
        GlStateManager.Blend blend = GlStateManager.BLEND;
        return switch (parameter) {
            case GL11.GL_MATRIX_MODE -> GlStateMirror.matrixMode();
            case GL14.GL_BLEND_SRC_RGB -> blend.sfactorRGB;
            case GL14.GL_BLEND_DST_RGB -> blend.dfactorRGB;
            case GL14.GL_BLEND_SRC_ALPHA -> blend.sfactorAlpha;
            case GL14.GL_BLEND_DST_ALPHA -> blend.dfactorAlpha;
            default -> original.call(parameter);
        };
    }
}
