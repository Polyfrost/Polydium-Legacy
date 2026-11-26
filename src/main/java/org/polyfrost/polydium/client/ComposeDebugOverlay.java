package org.polyfrost.polydium.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.overlay.DebugOverlay;
import org.apache.logging.log4j.LogManager;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class ComposeDebugOverlay {
    private static final MethodHandle DEFER = find();

    private ComposeDebugOverlay() {
    }

    static void defer(DebugOverlay overlay) {
        if (DEFER == null) return;
        try {
            DEFER.invoke(overlay);
        } catch (Throwable t) {
            LogManager.getLogger("Polydium").error("Could not hand F3 to PolyCompose", t);
        }
    }

    private static MethodHandle find() {
        if (!FabricLoader.getInstance().isModLoaded("polycompose")) return null;
        try {
            Class<?> host = Class.forName("org.polyfrost.compose.minecraft.render.gl.GlHost");
            Object instance = host.getField("INSTANCE").get(null);
            return MethodHandles.publicLookup()
                    .findVirtual(host, "deferDebugScreen", MethodType.methodType(boolean.class, DebugOverlay.class))
                    .bindTo(instance);
        } catch (ReflectiveOperationException | LinkageError e) {
            LogManager.getLogger("Polydium").warn("PolyCompose F3 hand-off unavailable", e);
            return null;
        }
    }
}
