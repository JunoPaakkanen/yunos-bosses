package com.yuno.yunosbosses.util;

import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public class ActiveBeam {
    private final Vec3d start;
    private final int range;
    private final int maxTicks;
    private int currentTicks;
    private final UUID ownerUuid;
    private final int chargeTicks = 20;
    private Vec3d lockedStart = null;
    private Vec3d lockedDir = null;

    public ActiveBeam(UUID ownerUuid, Vec3d start, int range, int maxTicks, int currentTicks) {
        this.ownerUuid = ownerUuid;
        this.start = start;
        this.range = range;
        this.maxTicks = maxTicks;
        this.currentTicks = currentTicks;
    }

    public void incrementAge() {
        this.currentTicks++;
    }

    public boolean isExpired() {
        return this.currentTicks >= maxTicks;
    }

    public boolean isCharging() {
        return this.currentTicks < chargeTicks;
    }

    public float getFiringProgress() {
        if (currentTicks < 20) return 0.0f;
        return (float) (currentTicks - 20) / (maxTicks -20);
    }

    public void lock(Vec3d start, Vec3d dir) {
        if (this.lockedStart == null) {
            this.lockedStart = start;
            this.lockedDir = dir;
        }
    }

    // Standard Getters
    public Vec3d getStart() { return start; }
    public int getRange() { return range; }
    public int getCurrentTicks() { return currentTicks; }
    public int getMaxTicks() { return maxTicks; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Vec3d getLockedStart() { return lockedStart; }
    public Vec3d getLockedDir() { return lockedDir; }
}