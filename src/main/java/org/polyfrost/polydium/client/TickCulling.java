package org.polyfrost.polydium.client;

public final class TickCulling {
    public static int frame;

    private TickCulling() {
    }

    public interface Rendered {
        boolean polydium$wasRendered();

        void polydium$markRendered();
    }
}
