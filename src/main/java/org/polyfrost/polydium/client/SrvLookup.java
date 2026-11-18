package org.polyfrost.polydium.client;

import java.util.function.BiConsumer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

public final class SrvLookup {
    public static final ThreadLocal<String[]> DEFERRED = new ThreadLocal<>();

    private static final boolean AVAILABLE = probe();

    private SrvLookup() {
    }

    public static boolean available() {
        return AVAILABLE;
    }

    public static void resolve(String host, BiConsumer<String, Integer> done) {
        NettySrv.resolve(host, done);
    }

    private static boolean probe() {
        if (!FabricLoader.getInstance().isModLoaded("sarcio")) {
            return false;
        }
        try {
            NettySrv.init();
            return true;
        } catch (Throwable t) {
            LogManager.getLogger("Polydium").warn("Sarcio DNS resolver unavailable, using vanilla SRV lookup", t);
            return false;
        }
    }
}
