package com.yuno.yunosbosses.entity.projectile;

import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SlashProjectileEntity extends ProjectileEntity {
    private Vec3d startPos;
    private static final double MAX_RANGE = 5.0; // 5 block range
    public final float randomRoll = (float) (Math.random() * 360.0);
    private float baseDamage;

    public SlashProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SlashProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world, float baseDamage) {
        super(entityType, world);
        this.baseDamage = baseDamage;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();

        // Store starting position on first tick
        if (this.startPos == null) {
            this.startPos = this.getPos();
        }

        // Check if the projectile has reached its maximum range
        double distanceTraveled = this.getPos().distanceTo(this.startPos);
        if (distanceTraveled >= MAX_RANGE) {
            this.discard();
            return;
        }

        // Perform collision check
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);

        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onCollision(hitResult);
        }

        // Move the projectile
        Vec3d velocity = this.getVelocity();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);

        // Discard the projectile after 20 ticks
        if (this.age > 20) {
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);

        if (this.getWorld().isClient) return;

        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        Vec3d hitPos = target.getBoundingBox().getCenter();

        if (target != owner && target instanceof LivingEntity) {
            if (owner != null) {
                // Apply damage to the target
                DamageSource source = ModDamageTypes.of(this.getWorld(), ModDamageTypes.CUTTING_MAGIC, owner);
                target.damage(source, baseDamage);
                // Spawn particles on hit
                spawnImpactParticles(hitPos);

            }
        }
        this.discard();
    }

    @Override
    protected boolean canHit(Entity entity) {
        // Don't hit the owner
        if (entity == this.getOwner()) {
            return false;
        }
        // Only hit living entities
        return entity instanceof LivingEntity && super.canHit(entity);
    }

    private void spawnImpactParticles(Vec3d pos) {
        if (this.getWorld().isClient) return;

        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        // Spawn particles
        serverWorld.spawnParticles(
                ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                pos.x, pos.y, pos.z,
                1,
                0.0, 0.0, 0.0,
                0.0
        );
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("BaseDamage", this.baseDamage);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.baseDamage = nbt.getFloat("BaseDamage");
    }
}
