package org.polyfrost.polydium.mixin.glmirror;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.IntBuffer;
import net.minecraft.client.render.Camera;
import org.lwjgl.opengl.GL11;
import org.polyfrost.polydium.client.GlStateMirror;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(ILjava/nio/IntBuffer;)V", remap = false))
    private static void polydium$mirroredViewport(int parameter, IntBuffer out, Operation<Void> original) {
        if (parameter != GL11.GL_VIEWPORT || !GlStateMirror.readViewport(out)) original.call(parameter, out);
    }
}
