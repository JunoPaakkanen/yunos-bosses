package com.yuno.yunosbosses.util;

import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public class ActiveBeam {
    private final Vec3d start;
    private final int range;
    private final int chargeTicks;
    private final int durationTicks;
    private final int maxTicks;
    private int currentTicks;
    private final UUID ownerUuid;
    private Vec3d lockedStart = null;
    private Vec3d lockedDir = null;
    private final boolean useCustomStart;
    private final Vec3d direction;
    private final float radius;

    public ActiveBeam(UUID ownerUuid, Vec3d start, int range, int chargeTicks, int durationTicks, float radius, boolean useCustomStart, Vec3d direction) {
        this.ownerUuid = ownerUuid;
        this.start = start;
        this.range = range;
        this.chargeTicks = Math.max(0, chargeTicks);
        this.durationTicks = Math.max(1, durationTicks);
        this.maxTicks = this.chargeTicks + this.durationTicks;
        this.currentTicks = 0;
        this.radius = radius > 0 ? radius : 0.4F;
        this.useCustomStart = useCustomStart;
        this.direction = direction;
    }

    // Backward-compatible constructor
    public ActiveBeam(UUID ownerUuid, Vec3d start, int range, int maxTicks, int currentTicks, boolean useCustomStart, Vec3d direction) {
        this(ownerUuid, start, range, 20, Math.max(1, maxTicks - 20), 0.4F, useCustomStart, direction);
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

    public float getChargeProgress() {
        if (chargeTicks <= 0) return 1.0f;
        return Math.min(1.0f, (float) currentTicks / (float) chargeTicks);
    }

    public float getFiringProgress() {
        if (isCharging()) return 0.0f;
        return Math.min(1.0f, (float) (currentTicks - chargeTicks) / (float) Math.max(1, durationTicks));
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
    public int getChargeTicks() { return chargeTicks; }
    public int getDurationTicks() { return durationTicks; }
    public int getCurrentTicks() { return currentTicks; }
    public int getMaxTicks() { return maxTicks; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Vec3d getLockedStart() { return lockedStart; }
    public Vec3d getLockedDir() { return lockedDir; }
    public boolean isUsingCustomStart() { return useCustomStart; }
    public Vec3d getDirection() { return direction; }
    public float getRadius() { return radius; }
}
