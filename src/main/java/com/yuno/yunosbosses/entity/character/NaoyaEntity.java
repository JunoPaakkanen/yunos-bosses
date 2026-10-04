package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.YunosBossEntity;

import com.yuno.yunosbosses.entity.goal.NaoyaAttackGoal;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NaoyaEntity extends PathAwareEntity implements GeoEntity, YunosBossEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Identifier SPEED_STACK_MODIFIER_ID = Identifier.of("yunosbosses", "naoya_speed_stacks");

    private static final TrackedData<Integer> SPEED_STACKS =
            DataTracker.registerData(NaoyaEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_DASHING =
            DataTracker.registerData(NaoyaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private final ServerBossBar bossBar = new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.YELLOW,
            BossBar.Style.PROGRESS
    );

    private NaoyaAttackGoal attackGoal;
    private int speedDecayTimer = 100;

    public NaoyaEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 250.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.ARMOR, 5.0D)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SPEED_STACKS, 0);
        builder.add(IS_DASHING, false);
    }

    public int getSpeedStacks() {
        return this.dataTracker.get(SPEED_STACKS);
    }

    public void setSpeedStacks(int stacks) {
        int clamped = Math.max(0, Math.min(15, stacks));
        this.dataTracker.set(SPEED_STACKS, clamped);
        this.speedDecayTimer = 80;
        updateSpeedAttribute();
    }

    public void addSpeedStacks(int count) {
        setSpeedStacks(getSpeedStacks() + count);
    }

    public boolean isDashing() {
        return this.dataTracker.get(IS_DASHING);
    }

    public void setDashing(boolean dashing) {
        this.dataTracker.set(IS_DASHING, dashing);
    }

    private void updateSpeedAttribute() {
        EntityAttributeInstance speedAttr = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_STACK_MODIFIER_ID);
            int stacks = getSpeedStacks();
            if (stacks > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        SPEED_STACK_MODIFIER_ID,
                        stacks * 0.04D,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                speedAttr.addTemporaryModifier(modifier);
            }
        }
    }

    @Override
    protected void initGoals() {
        this.attackGoal = new NaoyaAttackGoal(this, 1.6D);

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, this.attackGoal);
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public void executeProjectionSequence() {
        if (this.attackGoal != null) {
            this.attackGoal.triggerFlankDash();
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, state -> {
            if (state.isMoving() || isDashing()) {
                if (this.isSprinting() || this.getTarget() != null || isDashing() || getSpeedStacks() >= 4) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.naoya.run"));
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.naoya.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.naoya.idle"));
        })
        .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.naoya.attack"))
        .triggerableAnim("barrage", RawAnimation.begin().thenLoop("animation.naoya.punch_barrage"))
        .triggerableAnim("run", RawAnimation.begin().thenLoop("animation.naoya.run")));
    }

    public void triggerAttackAnim() {
        this.triggerAnim("controller", "attack");
    }

    public void triggerBarrageAnim() {
        this.triggerAnim("controller", "barrage");
    }

    public void triggerRunAnim() {
        this.triggerAnim("controller", "run");
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

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        // Passive speed decay when out of action
        if (this.speedDecayTimer > 0) {
            this.speedDecayTimer--;
        } else if (getSpeedStacks() > 0) {
            setSpeedStacks(getSpeedStacks() - 1);
            this.speedDecayTimer = 50;
        }

        int stacks = getSpeedStacks();
        // Speed VFX
        if (isDashing() || stacks >= 4) {
            if (this.age % 2 == 0) {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + 0.2, this.getZ(),
                        2, 0.2, 0.1, 0.2, 0.05);
            }
        }

        // Supersonic Ram pass-by damage at top speed stacks
        if (stacks >= 10 && (isSprinting() || isDashing())) {
            ProjectionSorcery.handleHighSpeedRam(this, stacks);
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Chance to evasively side-step incoming attacks using 24-FPS reaction
        if (this.attackGoal != null && this.getRandom().nextFloat() < 0.40F) {
            this.attackGoal.tryEvasiveStep(source);
        }

        boolean result = super.damage(world, source, amount);
        if (result && amount > 15.0F) {
            // Heavy hit breaks his speed accumulation rhythm
            this.setSpeedStacks(Math.max(0, this.getSpeedStacks() - 3));
        }
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
    }

    @Override
    public String getBossIdentifier() {
        return "naoya";
    }
}
