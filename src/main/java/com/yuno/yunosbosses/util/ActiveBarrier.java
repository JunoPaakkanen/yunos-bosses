package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ActiveBarrier {
    private final UUID ownerUuid;
    private final Vec3d position;
    private final Vec3d direction; // Which way the shield faces
    private int maxTicks;
    private int currentTicks;
    private final float radius;

    private final Identifier texture;
    private final BiConsumer<Entity, ActiveBarrier> domainEffect;
    private final DomainExpansion domainExpansion;
    private final boolean openBarrier;
    private boolean furnaceCueSent = false;
    private int domainBlockCursor = 0;

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration, float radius, Identifier texture, BiConsumer<Entity, ActiveBarrier> domainEffect, DomainExpansion domainExpansion, boolean openBarrier) {
        this.ownerUuid = ownerUuid;
        this.position = position;
        this.direction = direction;
        this.maxTicks = duration;
        this.currentTicks = 0;
        this.texture = texture;
        this.domainEffect = domainEffect;
        this.domainExpansion = domainExpansion;
        this.radius = radius;
        this.openBarrier = openBarrier;
    }

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration, float radius, Identifier texture, BiConsumer<Entity, ActiveBarrier> domainEffect, DomainExpansion domainExpansion) {
        this(ownerUuid, position, direction, duration, radius, texture, domainEffect, domainExpansion,
                domainExpansion != null && domainExpansion.isOpenBarrier());
    }

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration, Identifier texture, BiConsumer<Entity, ActiveBarrier> domainEffect, float radius) {
        this(ownerUuid, position, direction, duration, radius, texture, domainEffect, null, false);
    }

    public void tick() {
        this.currentTicks++;
    }

    public boolean isExpired() { return currentTicks >= maxTicks; }

    /**
     * Instantly marks this barrier as expired so it is cleaned up on the next tick.
     */
    public void expire() {
        this.currentTicks = this.maxTicks;
    }

    // Getters
    public Vec3d getPosition() { return this.position; }
    public Vec3d getDirection() { return this.direction; }
    public int getMaxTicks() { return this.maxTicks; }
    public int getCurrentTicks() { return this.currentTicks; }
    public UUID getOwnerUuid() { return this.ownerUuid; }
    public Identifier getTexture() { return this.texture; }
    public BiConsumer<Entity, ActiveBarrier> getDomainEffect() { return this.domainEffect; }
    public DomainExpansion getDomainExpansion() { return this.domainExpansion; }
    public float getRadius() { return this.radius; }
    public boolean isOpenBarrier() { return this.openBarrier; }

    public boolean isFurnaceCueSent() { return this.furnaceCueSent; }
    public void setFurnaceCueSent(boolean sent) { this.furnaceCueSent = sent; }

    public int getDomainBlockCursor() { return this.domainBlockCursor; }
    public void setDomainBlockCursor(int cursor) { this.domainBlockCursor = cursor; }
}
