package com.yuno.yunosbosses.util;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class ActiveBarrier {
    private final UUID ownerUuid;
    private final Vec3d position;
    private final Vec3d direction; // Which way the shield faces
    private int maxTicks;
    private int currentTicks;

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration) {
        this.ownerUuid = ownerUuid;
        this.position = position;
        this.direction = direction;
        this.maxTicks = duration;
        this.currentTicks = 0;
    }

    public void tick() {
        this.currentTicks++;
    }

    public boolean isExpired() { return currentTicks >= maxTicks; }

    // Getters
    public Vec3d getPosition() { return this.position; }
    public Vec3d getDirection() { return this.direction; }
    public int getMaxTicks() { return this.maxTicks; }
    public int getCurrentTicks() { return this.currentTicks; }
    public UUID getOwnerUuid() { return this.ownerUuid; }
}