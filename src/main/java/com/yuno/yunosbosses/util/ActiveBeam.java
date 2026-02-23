package com.yuno.yunosbosses.util;

import net.minecraft.util.math.Vec3d;

public class ActiveBeam {
    private final Vec3d start;
    private final Vec3d end;
    private final int maxTicks;
    private int currentTicks;

    public ActiveBeam(Vec3d start, Vec3d end, int maxTicks, int currentTicks) {
        this.start = start;
        this.end = end;
        this.maxTicks = maxTicks;
        this.currentTicks = currentTicks;
    }

    public void incrementAge() {
        this.currentTicks++;
    }

    public boolean isExpired() {
        return this.currentTicks >= maxTicks;
    }

    // Standard Getters
    public Vec3d getStart() { return start; }
    public Vec3d getEnd() { return end; }
    public int getCurrentTicks() { return currentTicks; }
    public int getMaxTicks() { return maxTicks; }
}