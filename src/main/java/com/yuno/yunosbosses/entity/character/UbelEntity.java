package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.goal.UbelAttackGoal;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.util.DuoModeCheckHelper;
import com.yuno.yunosbosses.world.ModGameRules;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

public class UbelEntity extends PathAwareEntity implements GeoEntity, YunosBossEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean difficultyInitialized = false;
    private UbelAttackGoal attackGoal;

    public UbelEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Equip Ubel with her staff
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.UBEL_STAFF));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 250.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25f)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0D)
                .add(EntityAttributes.ARMOR, 10.0D)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0D);
    }

    // BOSS HEALTH BAR
    private final ServerBossBar bossBar = new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.GREEN,
            BossBar.Style.NOTCHED_6
    );

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

        // Get revenge on any entity (mobs, iron golems, wolves, players) if she gets hit
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());

        // Target hostile entities / golems attacking her or players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (entity, serverWorld) -> entity instanceof PlayerEntity player ? (!player.isCreative() && !player.isSpectator()) : entity.getAttacking() == this));

        // Move towards her targets to attack them.
        this.attackGoal = new UbelAttackGoal(this, 2D);
        this.goalSelector.add(2, this.attackGoal);
    }

    public void applyDifficultyStats(boolean isDuo) {
        var maxHealthAttr = this.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHp = isDuo ? 550.0D : 250.0D;
            maxHealthAttr.setBaseValue(maxHp);
            this.setHealth((float) maxHp);
        }
        var armorAttr = this.getAttributeInstance(EntityAttributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(isDuo ? 15.0D : 10.0D);
        }
        var movementSpeedAttr = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeedAttr != null) {
            movementSpeedAttr.setBaseValue(isDuo ? 0.27D : 0.25D);
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
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target instanceof PlayerEntity) {
            this.setPersistent();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ubel.walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ubel.idle"));
        })
        // Melee animation
        .triggerableAnim("melee_attack", RawAnimation.begin().thenPlay("animation.ubel.melee"))
        // Domain cast animation
        .triggerableAnim("domain", RawAnimation.begin().thenPlay("animation.ubel.domain"))
        // Dismantle animation
        .triggerableAnim("dismantle", RawAnimation.begin().thenPlay("animation.ubel.dismantle")));
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
        // Sets the progress to health percentage (0.0 to 1.0)
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.attackGoal != null) {
            this.attackGoal.tryShadowSplitFeint(source);
        }
        return super.damage(world, source, amount);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false;
    }

    public void triggerMeleeAnim() {
        this.triggerAnim("controller", "melee_attack");
    }

    public void triggerDomainAnim() {
        this.triggerAnim("controller", "domain");
    }

    public void triggerDismantleAnim() {
        this.triggerAnim("controller", "dismantle");
    }

    @Override
    public String getBossIdentifier() {
        return "ubel";
    }
}
