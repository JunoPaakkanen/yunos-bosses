package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.goal.MethodeAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class MethodeEntity extends PathAwareEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final int DEFENSIVE_MAGIC_COOLDOWN = 100; // 5 seconds (100 ticks)
    private int defensiveMagicCooldown = 0;

    public MethodeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Equip Methode with her staff
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.UBEL_STAFF));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 350.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25f)
                .add(EntityAttributes.FOLLOW_RANGE, 500.0D)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0D);
    }

    // BOSS HEALTH BAR
    private final ServerBossBar bossBar = new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.RED,
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

        // Get revenge on the player if she gets hit.
        this.targetSelector.add(1, new RevengeGoal(this));

        // Actively target players (checkVisibility = false to prevent dropping combat lock behind partial cover)
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));

        // Move towards her targets to attack them.
        this.goalSelector.add(2, new MethodeAttackGoal(this, 2D));
    }

    @Override
    public boolean cannotDespawn() {
        return true; // She will stay in the world forever until killed.
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.methode.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.methode.idle"));
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

    public int getDefensiveMagicCooldown() {
        return this.defensiveMagicCooldown;
    }

    public void setDefensiveMagicCooldown(int cooldown) {
        this.defensiveMagicCooldown = cooldown;
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        // Sets the progress to health percentage (0.0 to 1.0)
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        // Always intercept incoming projectiles with Defensive Magic (works in Creative, Survival, Idle, etc.)
        handleDefensiveMagic(world);
    }

    private void handleDefensiveMagic(ServerWorld world) {
        // Enforce 5-second cooldown: decrement and return early if on cooldown
        if (this.defensiveMagicCooldown > 0) {
            this.defensiveMagicCooldown--;
            return;
        }

        // Search for incoming projectiles within 16 blocks
        double detectionRadius = 16.0;
        Box searchBox = this.getBoundingBox().expand(detectionRadius);
        List<ProjectileEntity> incomingProjectiles = world.getEntitiesByClass(
                ProjectileEntity.class,
                searchBox,
                projectile -> DefensiveProjectileShieldAbility.isProjectileHeadingTowardsBoss(this, projectile)
        );

        if (incomingProjectiles.isEmpty()) {
            return;
        }

        // Find the closest incoming projectile
        ProjectileEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (ProjectileEntity p : incomingProjectiles) {
            double d = p.squaredDistanceTo(this);
            if (d < closestDistSq) {
                closestDistSq = d;
                closest = p;
            }
        }

        if (closest == null) return;

        Vec3d toProj = closest.getPos().subtract(this.getEyePos()).normalize();

        // If Methode already has an active barrier in front of her protecting that direction, return
        if (BarrierManager.hasActiveBarrierFor(this.getUuid(), toProj)) {
            return;
        }

        // Snap Methode's look to the incoming projectile
        double dx = toProj.x;
        double dy = toProj.y;
        double dz = toProj.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
        this.setHeadYaw(targetYaw);
        this.setBodyYaw(targetYaw);
        this.setYaw(targetYaw);
        this.setPitch(targetPitch);
        this.getLookControl().lookAt(closest.getX(), closest.getY(), closest.getZ(), 360.0F, 360.0F);

        // Cast Defensive Magic!
        ModSpells.DEFENSIVE_MAGIC.cast(world, this, this.getMainHandStack());
        this.defensiveMagicCooldown = DEFENSIVE_MAGIC_COOLDOWN; // 5-second cooldown (100 ticks)
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
    }
}
