package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.goal.UbelAttackGoal;
import com.yuno.yunosbosses.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class UbelEntity extends PathAwareEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UbelEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Equip Ubel with her staff
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BASIC_MAGICAL_STAFF));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25f)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 500.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D);
    }

    // initGoals defines the entity's goals and priorities.
    @Override
    protected void initGoals() {
        // If she's in water, she MUST swim to stay alive.
        this.goalSelector.add(0, new SwimGoal(this));

        // Wander around the world so she doesn't just stand still.
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));

        // Look at the player when they are nearby.
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));

        // Just look around randomly while standing still.
        this.goalSelector.add(5, new LookAroundGoal(this));

        // Get revenge on the player if she gets hit.
        this.targetSelector.add(1, new RevengeGoal(this));

        // Move towards her targets to melee attack them.
        this.goalSelector.add(2, new UbelAttackGoal(this, 2D));
    }

    @Override
    public boolean cannotDespawn() {
        return true; // She will stay in the world forever until killed.
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ubel.walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ubel.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean shouldRender(double distance) {
        double d = 128.0 * 5;
        return distance < d * d;
    }

}
