package org.polyfrost.polydium.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.polyfrost.polydium.client.TickCulling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArgentumWorldRenderer.class, remap = false)
public abstract class ArgentumWorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void polydium$nextFrame(CallbackInfoReturnable<Integer> cir) {
        if (!((ArgentumWorldRenderer) (Object) this).getEntityInstancing().isShadowPass()) {
            TickCulling.frame++;
        }
    }

    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;F)Z", remap = true))
    private boolean polydium$markRendered(EntityRenderDispatcher dispatcher, Entity entity, float tickDelta, Operation<Boolean> original) {
        if (!((ArgentumWorldRenderer) (Object) this).getEntityInstancing().isShadowPass()) {
            ((TickCulling.Rendered) entity).polydium$markRendered();
        }
        return original.call(dispatcher, entity, tickDelta);
    }
}
