package org.polyfrost.polydium.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class AutoDisable {
    static final int PASS = 0;
    static final int BLIT = 1;
    static final int HUD = 2;

    private static final Logger LOGGER = LogManager.getLogger("Polydium");
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final long MAX_FRAME_GAP_NANOS = 250_000_000L;
    private static final int MIN_CYCLES = 5;
    private static final int AGREEING_WINDOWS = 3;
    private static final long MARGIN_NANOS = 20_000L;
    private static final int MAX_INVALID_GPU_WINDOWS = 10;

    private static final int WRITING = -1;
    private static final int ABANDONED = -2;
    private static final int SLOTS = GpuTimer.PAIRS;
    private static final int[] slotKind = new int[SLOTS];
    private static final int[] slotEpoch = new int[SLOTS];
    private static final long[] slotCpu = new long[SLOTS];
    private static final int[] slotWritten = new int[SLOTS];
    private static int head;
    private static int tail;

    private static final long[] spanStart = new long[3];
    private static final int[] spanSlot = new int[3];
    private static final boolean[] spanActive = new boolean[3];

    private static final long[] cost = new long[3];
    private static int epoch;
    private static long windowStart = -1;
    private static long windowEnd = -1;
    private static long lastTick;
    private static int frames;
    private static int cycles;
    private static int votes;
    private static boolean windowDirty;
    private static boolean invalidGpu;
    private static int invalidGpuWindows;
    private static boolean warmingUp;

    private static boolean suppressed;
    private static boolean calibrating;
    private static double cycleCost;
    private static double blitCost;
    private static double hudOffset;

    private AutoDisable() {
    }

    static boolean suppressed() {
        return suppressed;
    }

    static void reset() {
        for (int kind = 0; kind < spanActive.length; kind++) {
            abandon(kind);
            spanActive[kind] = false;
        }
        windowStart = -1;
        epoch++;
        votes = 0;
        suppressed = false;
    }

    static boolean begin(int kind) {
        if (windowStart < 0 || windowEnd >= 0 || suppressed != (kind == HUD)) {
            return false;
        }
        abandon(kind);
        int slot = allocateSlot();
        if (slot >= 0 && !GpuTimer.write(slot * 2)) {
            slotCpu[slot] = ABANDONED;
            slot = -1;
        }
        if (slot >= 0) {
            slotWritten[slot] = 1;
        }
        spanActive[kind] = true;
        spanSlot[kind] = slot;
        spanStart[kind] = System.nanoTime();
        return true;
    }

    static void end(int kind) {
        if (!spanActive[kind]) return;
        int slot = spanSlot[kind];
        boolean gpuWritten = slot >= 0 && GpuTimer.write(slot * 2 + 1);
        long cpu = System.nanoTime() - spanStart[kind];
        spanActive[kind] = false;
        if (gpuWritten) {
            slotWritten[slot] = 2;
            slotKind[slot] = kind;
            slotEpoch[slot] = epoch;
            slotCpu[slot] = cpu;
            return;
        }
        if (slot >= 0) {
            slotCpu[slot] = ABANDONED;
        }
        if (GpuTimer.supported()) {
            windowDirty = true;
            invalidGpu = true;
        }
        cost[kind] += cpu;
    }

    static void onCycle() {
        if (windowEnd < 0) {
            cycles++;
        }
    }

    static void tick(boolean enabled, boolean measurable, int hudFps) {
        if (!enabled) {
            if (suppressed) {
                reset();
            }
            windowStart = -1;
            return;
        }
        if (!measurable) {
            windowStart = -1;
            votes = 0;
            return;
        }
        long now = System.nanoTime();
        long gap = now - lastTick;
        lastTick = now;
        if (windowStart < 0) {
            startWindow(now);
            epoch++;
            warmingUp = true;
            return;
        }
        if (windowEnd < 0) {
            frames++;
            if (gap > MAX_FRAME_GAP_NANOS) {
                windowDirty = true;
            }
            if (now - windowStart < WINDOW_NANOS) {
                return;
            }
            windowEnd = now;
        }
        poll();
        if (tail != head) {
            if (now - windowEnd < WINDOW_NANOS) {
                return;
            }
            epoch++;
            windowDirty = true;
            invalidGpu = true;
        }
        if (!invalidGpu) {
            invalidGpuWindows = 0;
        }
        if (invalidGpu && ++invalidGpuWindows == MAX_INVALID_GPU_WINDOWS) {
            LOGGER.warn("GPU timer keeps returning invalid or late samples, measuring HUD cost on the CPU only");
            GpuTimer.disable();
            tail = head;
            switchMode(false);
        } else {
            evaluate(windowEnd - windowStart, hudFps);
        }
        startWindow(now);
    }

    private static void abandon(int kind) {
        if (spanActive[kind] && spanSlot[kind] >= 0) {
            slotCpu[spanSlot[kind]] = ABANDONED;
        }
    }

    private static int allocateSlot() {
        if (!GpuTimer.supported()) {
            return -1;
        }
        if ((head + 1) % SLOTS == tail) {
            poll();
            if ((head + 1) % SLOTS == tail) {
                return -1;
            }
        }
        int slot = head;
        head = (head + 1) % SLOTS;
        slotCpu[slot] = WRITING;
        slotWritten[slot] = 0;
        return slot;
    }

    private static void poll() {
        while (tail != head) {
            long cpu = slotCpu[tail];
            if (cpu == WRITING) {
                return;
            }
            if (cpu == ABANDONED) {
                int written = slotWritten[tail];
                if (written > 0 && !GpuTimer.available(tail * 2 + written - 1)) {
                    return;
                }
                tail = (tail + 1) % SLOTS;
                continue;
            }
            int limit = head > tail ? head : SLOTS;
            int run = 1;
            while (tail + run < limit && slotCpu[tail + run] >= 0) {
                run++;
            }
            int ready = GpuTimer.read(tail, run);
            for (int slot = tail; slot < tail + ready; slot++) {
                if (slotEpoch[slot] == epoch) {
                    long gpu = GpuTimer.elapsed(slot);
                    if (gpu < 0) {
                        windowDirty = true;
                        invalidGpu = true;
                    }
                    cost[slotKind[slot]] += Math.max(slotCpu[slot], gpu);
                }
            }
            tail = (tail + ready) % SLOTS;
            if (ready < run) {
                return;
            }
        }
    }

    private static void startWindow(long now) {
        windowStart = now;
        windowEnd = -1;
        frames = 0;
        cycles = 0;
        windowDirty = false;
        invalidGpu = false;
        cost[PASS] = cost[BLIT] = cost[HUD] = 0;
    }

    private static void evaluate(long elapsedNanos, int hudFps) {
        if (warmingUp) {
            warmingUp = false;
            return;
        }
        if (frames == 0 || windowDirty) return;
        if (!suppressed) {
            if (cycles < MIN_CYCLES) {
                votes = 0;
                return;
            }
            cycleCost = cost[PASS] / (double) cycles;
            double cycleRatio = cycles / (double) frames;
            blitCost = cost[BLIT] / (double) frames;
            double saving = cycleCost * (1 - cycleRatio) - blitCost;
            if (vote(saving < -MARGIN_NANOS)) {
                LOGGER.info("Disabling HUD caching: HUD costs {} us per frame without it, {} us with it",
                        micros(cycleCost), micros(cycleCost * cycleRatio + blitCost));
                calibrating = true;
                switchMode(true);
            }
        } else {
            double hud = cost[HUD] / (double) frames;
            if (calibrating) {
                calibrating = false;
                hudOffset = cycleCost - hud;
                return;
            }
            double expected = Math.max(0, hud + hudOffset);
            double cycleRatio = Math.min(1.0, hudFps * (elapsedNanos / 1e9) / frames);
            double saving = expected * (1 - cycleRatio) - blitCost;
            if (vote(saving > MARGIN_NANOS)) {
                LOGGER.info("Enabling HUD caching: HUD costs {} us per frame without it, about {} us with it",
                        micros(expected), micros(expected * cycleRatio + blitCost));
                switchMode(false);
            }
        }
    }

    private static boolean vote(boolean wantsSwitch) {
        votes = wantsSwitch ? votes + 1 : 0;
        return votes >= AGREEING_WINDOWS;
    }

    private static void switchMode(boolean suppress) {
        votes = 0;
        epoch++;
        warmingUp = true;
        suppressed = suppress;
    }

    private static long micros(double nanos) {
        return Math.round(nanos / 1000);
    }
}
