package com.yuno.yunosbosses.entity.other;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class SeveredTorsoEntity extends PathAwareEntity {
    private int livingTicks = 0;
    private final int MAX_AGE = 600; // 30 seconds
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(SeveredTorsoEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    public SeveredTorsoEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(false);
    }

    @Override
    public void tick() {
        super.tick();
        livingTicks++;
        if (!this.getWorld().isClient && livingTicks > MAX_AGE) {
            this.discard(); // Goodbye, torso!
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, Optional.empty());
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void pushAway(Entity entity) {}

    @Override
    public boolean isCollidable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean collidesWith(Entity other) { return false; }

    @Override
    public void pushAwayFrom(Entity entity) {}

    @Override
    public boolean canHit() { return false; }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) { return true; }

    @Override
    protected void mobTick() {}

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.getOwnerUuid() != null) {
            nbt.putUuid("OwnerUUID", this.getOwnerUuid());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("OwnerUUID")) {
            this.setOwnerUuid(nbt.getUuid("OwnerUUID"));
        }
    }

    public void setOwnerUuid(UUID uuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUuid() {
        return this.dataTracker.get(OWNER_UUID).orElse(null);
    }
}