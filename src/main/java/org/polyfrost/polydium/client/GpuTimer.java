package org.polyfrost.polydium.client;

import java.nio.IntBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

final class GpuTimer {
    static final int PAIRS = 128;
    private static final Logger LOGGER = LogManager.getLogger("Polydium");
    private static final long MAX_ELAPSED_NANOS = 250_000_000L;
    private static final int[] queries = new int[PAIRS * 2];
    private static final long[] elapsed = new long[PAIRS];
    private static boolean initialized;
    private static boolean supported;
    private static boolean warned;
    private static boolean warnedInvalid;
    private static long counterMask = -1L;

    private GpuTimer() {
    }

    static boolean supported() {
        if (!initialized) {
            initialized = true;
            try {
                ContextCapabilities caps = GLContext.getCapabilities();
                if (!caps.OpenGL33 && !caps.GL_ARB_timer_query) {
                    LOGGER.info("GPU timestamps are not supported, measuring HUD cost on the CPU only");
                    return false;
                }
                int bits = GL15.glGetQueryi(GL33.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS);
                if (bits <= 0) {
                    LOGGER.info("GPU timestamp counter has no bits, measuring HUD cost on the CPU only");
                    return false;
                }
                if (bits < 64) {
                    counterMask = (1L << bits) - 1;
                }
                IntBuffer ids = BufferUtils.createIntBuffer(queries.length);
                GL15.glGenQueries(ids);
                ids.get(queries);
                supported = true;
            } catch (RuntimeException e) {
                LOGGER.warn("GPU timestamps are unavailable, measuring HUD cost on the CPU only", e);
            }
        }
        return supported;
    }

    static boolean write(int index) {
        try {
            GL33.glQueryCounter(queries[index], GL33.GL_TIMESTAMP);
            return true;
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Failed to write GPU timestamp, measuring CPU time only for that span", e);
            }
            return false;
        }
    }

    static int read(int firstPair, int count) {
        for (int i = 0; i < count; i++) {
            int pair = firstPair + i;
            if (GL15.glGetQueryObjecti(queries[pair * 2 + 1], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
                return i;
            }
            store(pair, (GL33.glGetQueryObjecti64(queries[pair * 2 + 1], GL15.GL_QUERY_RESULT)
                    - GL33.glGetQueryObjecti64(queries[pair * 2], GL15.GL_QUERY_RESULT)) & counterMask);
        }
        return count;
    }

    static boolean available(int index) {
        return GL15.glGetQueryObjecti(queries[index], GL15.GL_QUERY_RESULT_AVAILABLE) != 0;
    }

    static long elapsed(int pair) {
        return elapsed[pair];
    }

    static void disable() {
        supported = false;
    }

    private static void store(int pair, long nanos) {
        if (nanos >= 0 && nanos <= MAX_ELAPSED_NANOS) {
            elapsed[pair] = nanos;
            return;
        }
        elapsed[pair] = -1;
        if (!warnedInvalid) {
            warnedInvalid = true;
            LOGGER.warn("GPU timer returned {} ns for one span, ignoring invalid GPU samples", nanos);
        }
    }
}
