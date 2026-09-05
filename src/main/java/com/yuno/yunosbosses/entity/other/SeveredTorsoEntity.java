package com.yuno.yunosbosses.entity.other;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

import java.util.UUID;

public class SeveredTorsoEntity extends PathAwareEntity {
    private int livingTicks = 0;
    private static final int MAX_AGE = 600; // 30 seconds
    private static final TrackedData<String> OWNER_UUID_STRING = DataTracker.registerData(SeveredTorsoEntity.class, TrackedDataHandlerRegistry.STRING);

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
        builder.add(OWNER_UUID_STRING, "");
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createLivingAttributes()
                .add(EntityAttributes.MAX_HEALTH, 10.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void pushAway(Entity entity) {}

    @Override
    public boolean isCollidable(Entity entity) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean collidesWith(Entity other) { return false; }

    @Override
    public void pushAwayFrom(Entity entity) {}

    @Override
    public boolean canHit() { return false; }

    @Override
    public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
        return true;
    }

    @Override
    protected void mobTick(ServerWorld world) {}

    @Override
    protected void writeCustomData(WriteView nbt) {
        super.writeCustomData(nbt);
        nbt.putNullable("OwnerUUID", Uuids.INT_STREAM_CODEC, this.getOwnerUuid());
    }

    @Override
    protected void readCustomData(ReadView nbt) {
        super.readCustomData(nbt);
        nbt.read("OwnerUUID", Uuids.INT_STREAM_CODEC).ifPresent(this::setOwnerUuid);
    }

    public void setOwnerUuid(UUID uuid) {
        this.dataTracker.set(OWNER_UUID_STRING, uuid == null ? "" : uuid.toString());
    }

    public UUID getOwnerUuid() {
        String uuidStr = this.dataTracker.get(OWNER_UUID_STRING);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
