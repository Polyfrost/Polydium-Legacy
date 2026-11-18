package org.polyfrost.polydium.mixin.srv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.net.IDN;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.menu.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import org.polyfrost.polydium.client.SrvLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Shadow
    private boolean aborted;

    @Shadow
    private void connect(String address, int port) {
    }

    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/options/ServerListEntry;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ServerAddress;parse(Ljava/lang/String;)Lnet/minecraft/client/network/ServerAddress;")
    )
    private ServerAddress polydium$deferSrv(String ip, Operation<ServerAddress> original, @Share("srvHost") LocalRef<String> srvHost) {
        if (!SrvLookup.available()) {
            return original.call(ip);
        }
        String[] deferred = new String[1];
        SrvLookup.DEFERRED.set(deferred);
        try {
            return original.call(ip);
        } finally {
            SrvLookup.DEFERRED.remove();
            srvHost.set(deferred[0]);
        }
    }

    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/options/ServerListEntry;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/menu/multiplayer/ConnectScreen;connect(Ljava/lang/String;I)V")
    )
    private void polydium$connectAfterSrv(ConnectScreen screen, String address, int port, Operation<Void> original, @Share("srvHost") LocalRef<String> srvHost) {
        String host = srvHost.get();
        if (host == null) {
            original.call(screen, address, port);
            return;
        }
        String asciiHost;
        try {
            asciiHost = IDN.toASCII(host);
        } catch (IllegalArgumentException exception) {
            original.call(screen, address, port);
            return;
        }
        SrvLookup.resolve(asciiHost, (target, targetPort) -> Minecraft.getInstance().executeTask(() -> {
            if (!this.aborted) {
                this.connect(target, targetPort);
            }
        }));
    }
}
