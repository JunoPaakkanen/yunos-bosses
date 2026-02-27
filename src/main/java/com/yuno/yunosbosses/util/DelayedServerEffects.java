package com.yuno.yunosbosses.util;

import java.util.ArrayList;
import java.util.List;

public class DelayedServerEffects {
    private static final List<DelayedTask> TASKS= new ArrayList<>();

    private record DelayedTask(int remainingTicks, Runnable action) {}

    public static void delay(int ticks, Runnable action) {
        TASKS.add(new DelayedTask(ticks, action));
    }

    public static void tick() {
        for (int i = TASKS.size() -1; i >= 0; i--) {
            DelayedTask task = TASKS.get(i);
            int nextTicks = task.remainingTicks -1;

            if (nextTicks <= 0) {
                task.action.run();
                TASKS.remove(i);
            } else {
                TASKS.set(i, new DelayedTask(nextTicks, task.action));
            }
        }
    }
}
