package org.polyfrost.polydium.mixin.limiter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import org.polyfrost.polydium.client.PreciseFrameLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @WrapOperation(method = "runGame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;sync(I)V", remap = false))
    private void polydium$preciseSync(int fps, Operation<Void> original) {
        PreciseFrameLimiter.limit(fps);
    }
}
