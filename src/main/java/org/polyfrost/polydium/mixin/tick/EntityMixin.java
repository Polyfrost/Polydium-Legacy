package org.polyfrost.polydium.mixin.tick;

import net.minecraft.entity.Entity;
import org.polyfrost.polydium.client.TickCulling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements TickCulling.Rendered {
    @Unique
    private int polydium$renderedFrame = -1;

    @Override
    public boolean polydium$wasRendered() {
        return this.polydium$renderedFrame == TickCulling.frame;
    }

    @Override
    public void polydium$markRendered() {
        this.polydium$renderedFrame = TickCulling.frame;
    }
}
