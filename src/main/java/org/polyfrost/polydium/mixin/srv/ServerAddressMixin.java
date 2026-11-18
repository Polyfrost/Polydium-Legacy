package org.polyfrost.polydium.mixin.srv;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.network.ServerAddress;
import org.polyfrost.polydium.client.SrvLookup;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerAddress.class)
public abstract class ServerAddressMixin {
    @WrapMethod(method = "parseAddress")
    private static String[] polydium$deferSrvLookup(String address, Operation<String[]> original) {
        String[] deferred = SrvLookup.DEFERRED.get();
        if (deferred == null) {
            return original.call(address);
        }
        deferred[0] = address;
        return new String[]{address, "25565"};
    }
}
