package org.polyfrost.polydium.mixin.glmirror;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.GameRenderer;
import org.polyfrost.polydium.client.GlStateMirror;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(
            method = {"setupCamera", "renderItemInHand", "render(IFJ)V", "renderClouds"},
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false),
            require = 6
    )
    private void polydium$mirrorPerspective(float fov, float aspect, float near, float far, Operation<Void> original) {
        original.call(fov, aspect, near, far);
        GlStateMirror.perspective(fov, aspect, near, far);
    }
}
