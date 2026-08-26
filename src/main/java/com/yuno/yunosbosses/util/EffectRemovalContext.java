package com.yuno.yunosbosses.util;

public class EffectRemovalContext {
    // ThreadLocal ensures thread-safety during server ticks
    private static final ThreadLocal<Boolean> MANUAL_REMOVAL = ThreadLocal.withInitial(() -> false);

    public static void setManualRemoval(boolean value) {
        MANUAL_REMOVAL.set(value);
    }

    public static boolean isManualRemoval() {
        return MANUAL_REMOVAL.get();
    }
}
