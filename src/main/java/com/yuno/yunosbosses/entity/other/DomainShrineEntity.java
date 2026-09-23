package com.yuno.yunosbosses.entity.other;

import com.yuno.yunosbosses.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DomainShrineEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DomainShrineEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();

        // If the shrine exists for more than 60 seconds, destroy it
        if (!this.getWorld().isClient && this.age > 1200) {
            this.discard();
        }

        // --- SHRINE CURSED AURA & RISING GROUND EFFECT ---
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // While rising (first 45 ticks): ground crumbling and earth bursts
            if (this.age <= 45) {
                double r = 4.0;
                for (int i = 0; i < 2; i++) {
                    double angle = this.random.nextDouble() * 2.0 * Math.PI;
                    double dist = this.random.nextDouble() * r;
                    double px = this.getX() + Math.cos(angle) * dist;
                    double pz = this.getZ() + Math.sin(angle) * dist;
                    serverWorld.spawnParticles(ParticleTypes.POOF, px, this.getY() + 0.1, pz, 1, 0.1, 0.05, 0.1, 0.02);
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, this.getY() + 0.1, pz, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            // Continuous dark cursed aura billowing around the shrine
            if (this.age % 2 == 0) {
                double sx = this.getX() + (this.random.nextDouble() - 0.5) * 6.0;
                double sz = this.getZ() + (this.random.nextDouble() - 0.5) * 6.0;
                double sy = this.getY() + 0.5 + this.random.nextDouble() * 5.0;
                serverWorld.spawnParticles(ParticleTypes.SMOKE, sx, sy, sz, 1, 0.05, 0.08, 0.05, 0.01);
                serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, sx, sy, sz, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isCollidable(Entity entity) {
        return true;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return true;
    }

    @Override
    public void pushAwayFrom(Entity entity) {}

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    protected void readCustomData(ReadView nbt) {

    }

    @Override
    protected void writeCustomData(WriteView nbt) {

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
