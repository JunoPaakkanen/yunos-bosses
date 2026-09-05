package com.yuno.yunosbosses.entity.other;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
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
