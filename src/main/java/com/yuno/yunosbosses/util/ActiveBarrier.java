package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration, Identifier texture, BiConsumer<Entity, ActiveBarrier> domainEffect, DomainExpansion domainExpansion) {
        this.ownerUuid = ownerUuid;
        this.position = position;
        this.direction = direction;
        this.maxTicks = duration;
        this.currentTicks = 0;
        this.texture = texture;
        this.domainEffect = domainEffect;
        this.domainExpansion = domainExpansion;
        this.radius = domainExpansion.getRadius();
    }

    public ActiveBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int duration, Identifier texture, BiConsumer<Entity, ActiveBarrier> domainEffect, float radius) {
        this.ownerUuid = ownerUuid;
        this.position = position;
        this.direction = direction;
        this.maxTicks = duration;
        this.currentTicks = 0;
        this.texture = texture;
        this.domainEffect = domainEffect;
        this.domainExpansion = null;
        this.radius = radius;
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
    public Identifier getTexture() { return this.texture; }
    public BiConsumer<Entity, ActiveBarrier> getDomainEffect() { return this.domainEffect; }
    public DomainExpansion getDomainExpansion() { return this.domainExpansion; }
    public float getRadius() { return this.radius; }
}