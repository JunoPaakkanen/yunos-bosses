package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.goal.FernAttackGoal;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.util.BossCombatHelper;
import com.yuno.yunosbosses.util.DuoModeCheckHelper;
import com.yuno.yunosbosses.world.ModGameRules;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FernEntity extends PathAwareEntity implements GeoEntity, YunosBossEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean difficultyInitialized = false;
    private int reinforcementBrokenTicks = 0;
    private boolean alternateAttackAnim = false;

    private final ServerBossBar bossBar = new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.PURPLE,
            BossBar.Style.PROGRESS
    );

    private FernAttackGoal attackGoal;

    public FernEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        // Equip Fern's Staff in the main hand
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FERN_STAFF));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.ARMOR, 10.0D)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0D)
                .add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 0.5);
    }

    public void applyDifficultyStats(boolean isDuo) {
        var maxHealthAttr = this.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHp = isDuo ? 450.0D : 200.0D;
            maxHealthAttr.setBaseValue(maxHp);
            this.setHealth((float) maxHp);
        }
        var armorAttr = this.getAttributeInstance(EntityAttributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(isDuo ? 14.0D : 10.0D);
        }
        var movementSpeedAttr = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeedAttr != null) {
            movementSpeedAttr.setBaseValue(isDuo ? 0.28D : 0.25D);
        }
        this.difficultyInitialized = true;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        applyDifficultyStats(world.toServerWorld().getGameRules().getBoolean(ModGameRules.DUO_BOSS_DIFFICULTY));
        return data;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putBoolean("DifficultyInitialized", this.difficultyInitialized);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        this.difficultyInitialized = view.getBoolean("DifficultyInitialized", false);
    }

    @Override
    protected void initGoals() {
        this.attackGoal = new FernAttackGoal(this, 1.5D);

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, this.attackGoal);
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (entity, serverWorld) -> entity != this && (entity instanceof PlayerEntity player ? (!player.isCreative() && !player.isSpectator()) : entity.getAttacking() == this)));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == this || (target != null && target.getUuid().equals(this.getUuid()))) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
        if (target instanceof PlayerEntity) {
            this.setPersistent();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.fern.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.fern.idle"));
        })
        .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.fern.attack"))
        .triggerableAnim("attack2", RawAnimation.begin().thenPlay("animation.fern.attack2")));
    }

    public void triggerAttackAnim() {
        this.triggerAnim("controller", this.alternateAttackAnim ? "attack2" : "attack");
        this.alternateAttackAnim = !this.alternateAttackAnim;
    }

    @Override
    protected float modifyAppliedDamage(DamageSource source, float amount) {
        amount = super.modifyAppliedDamage(source, amount);

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            amount *= 0.10F;
        }

        if (source.isOf(DamageTypes.LAVA)) {
            amount *= 0.3F;
        }

        amount = BossCombatHelper.applyReinforcementAndCap(this, this, source, amount);
        return amount;
    }

    @Override
    public int getReinforcementBrokenTicks() {
        return this.reinforcementBrokenTicks;
    }

    @Override
    public void setReinforcementBrokenTicks(int ticks) {
        this.reinforcementBrokenTicks = ticks;
    }

    @Override
    public void breakReinforcement(int ticks, DamageSource source) {
        BossCombatHelper.executeReinforcementBreak(this, this, source, ticks);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (BossCombatHelper.isHeavySpell(source, amount)) {
            int duration = DuoModeCheckHelper.isDuoMode(world) ? 60 : 100;
            this.breakReinforcement(duration, source);
        }

        return super.damage(world, source, amount);
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
        if (!this.difficultyInitialized) {
            applyDifficultyStats(DuoModeCheckHelper.isDuoMode(world));
        }
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        if (this.reinforcementBrokenTicks > 0) {
            this.reinforcementBrokenTicks--;
            if (this.age % 4 == 0) {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        2, 0.2, 0.4, 0.2, 0.05);
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
    }

    @Override
    public String getBossIdentifier() {
        return "fern";
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false;
    }
}
