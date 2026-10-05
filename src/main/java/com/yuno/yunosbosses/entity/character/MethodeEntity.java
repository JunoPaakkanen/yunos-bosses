package com.yuno.yunosbosses.entity.character;

import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.goal.MethodeAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class MethodeEntity extends PathAwareEntity implements GeoEntity, YunosBossEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final int DEFENSIVE_MAGIC_COOLDOWN = 100; // 5 seconds (100 ticks)
    private int defensiveMagicCooldown = 0;

    public MethodeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        // Equip Methode's Staff in the main hand
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.METHODE_STAFF));
    }

    private final ServerBossBar bossBar = new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.BLUE, // The color of the boss bar
            BossBar.Style.PROGRESS // The style (e.g., PROGRESS, NOTCHED_6, NOTCHED_10, etc.)
    );

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 150.0D) // 150 health
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25D) // Movement speed
                .add(EntityAttributes.ATTACK_DAMAGE, 9.0D) // Attack damage
                .add(EntityAttributes.ARMOR, 10.0D)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0D); // Aggro follow range
    }

    @Override
    protected void initGoals() {
        // Prevent her from drowning when in water.
        this.goalSelector.add(0, new SwimGoal(this));

        // Randomly walk around when not fighting.
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));

        // Look at the player when nearby.
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));

        // Look around randomly when idle.
        this.goalSelector.add(5, new LookAroundGoal(this));

        // Get revenge on any entity (mobs, iron golems, wolves, players) if she gets hit
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());

        // Target hostile entities / golems attacking her or players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (entity, serverWorld) -> entity instanceof PlayerEntity player ? (!player.isCreative() && !player.isSpectator()) : entity.getAttacking() == this));

        // Move towards her targets to attack them.
        this.goalSelector.add(2, new MethodeAttackGoal(this, 2D));
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
        controllers.add(new AnimationController<>("controller", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.methode.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.methode.idle"));
        })
        .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.methode.attack")));
    }

    public void triggerAttackAnim() {
        this.triggerAnim("controller", "attack");
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

    /**
     * Attempts to raise Methode's Defensive Magic shield towards an incoming threat (projectile or beam).
     * Returns true if the shield was successfully raised.
     */
    public boolean tryDefendAgainst(Vec3d threatOrigin) {
        if (this.defensiveMagicCooldown > 0) {
            return false;
        }
        if (this.isDead() || !this.isAlive()) {
            return false;
        }

        Vec3d toThreat = threatOrigin.subtract(this.getEyePos()).normalize();
        if (BarrierManager.hasActiveBarrierFor(this.getUuid(), toThreat)) {
            return false;
        }

        // Snap Methode's look directly towards the incoming threat
        double dx = toThreat.x;
        double dy = toThreat.y;
        double dz = toThreat.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
        this.setHeadYaw(targetYaw);
        this.setBodyYaw(targetYaw);
        this.setYaw(targetYaw);
        this.setPitch(targetPitch);
        this.getLookControl().lookAt(threatOrigin.x, threatOrigin.y, threatOrigin.z, 360.0F, 360.0F);

        // Cast Defensive Magic
        ModSpells.DEFENSIVE_MAGIC.cast(this.getWorld(), this, this.getMainHandStack());
        this.defensiveMagicCooldown = DEFENSIVE_MAGIC_COOLDOWN; // 5-second cooldown (100 ticks)
        return true;
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

        if (closest != null) {
            tryDefendAgainst(closest.getPos());
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
    }

    @Override
    public String getBossIdentifier() {
        return "methode";
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false;
    }
}
