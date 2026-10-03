package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractBossAttackGoal extends Goal {
    protected final MobEntity boss;
    protected final double speed;
    protected LivingEntity target;

    protected int globalCooldown = 0;
    protected int teleportCooldown = 0;
    protected int defensiveCooldown = 0;
    protected int attackTimer = 0;
    protected BossAbility activeAbility = null;

    protected final List<BossAbility> abilities = new ArrayList<>();
    protected final List<DefensiveProjectileShieldAbility> defensiveAbilities = new ArrayList<>();

    // Ideal distance: -1 means "just chase the target normally"
    private double idealDistance = -1;
    private static final double DISTANCE_TOLERANCE = 2.0;

    public AbstractBossAttackGoal(MobEntity boss, double speed) {
        this.boss = boss;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    protected void registerAbility(BossAbility entry) {
        if (entry instanceof DefensiveProjectileShieldAbility defensive) {
            defensiveAbilities.add(defensive);
        } else {
            abilities.add(entry);
        }
    }

    protected void setIdealDistance(double distance) {
        this.idealDistance = distance;
    }

    @Override
    public boolean canStart() {
        this.target = this.boss.getTarget();
        if (this.target == null || !this.target.isAlive()) {
            findTarget();
        }
        return this.target != null && this.target.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    protected void findTarget() {
        PlayerEntity nearest = this.boss.getWorld().getClosestPlayer(this.boss, 32.0);
        if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && nearest.isAlive()) {
            this.boss.setTarget(nearest);
            this.target = nearest;
        }
    }

    @Override
    public void stop() {
        if (this.activeAbility != null) {
            this.activeAbility.stop(this.boss);
        }
        this.target = null;
        this.attackTimer = 0;
        this.activeAbility = null;
        this.defensiveCooldown = 0;
        this.boss.setAttacking(false);
        this.boss.getNavigation().stop();
    }

    public static void snapLook(MobEntity boss, LivingEntity target) {
        if (boss == null || target == null) return;
        boss.getLookControl().lookAt(target, 180.0F, 180.0F);
        double dx = target.getX() - boss.getX();
        double dy = (target.getEyeY() - 0.2) - boss.getEyeY();
        double dz = target.getZ() - boss.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
        boss.setHeadYaw(targetYaw);
        boss.setBodyYaw(targetYaw);
        boss.setYaw(targetYaw);
        boss.setPitch(targetPitch);
    }

    protected void snapLookAtTarget() {
        snapLook(this.boss, this.target);
    }

    protected void onAbilityStarted(BossAbility ability) {
    }

    protected void onAbilityExecuted(BossAbility ability) {
    }

    public BossAbility getActiveAbility() {
        return this.activeAbility;
    }

    public void startAbility(BossAbility ability) {
        startAbility(ability, ability.getWindupTicks());
    }

    public void startAbility(BossAbility ability, int windupTicks) {
        this.activeAbility = ability;
        this.attackTimer = windupTicks;

        snapLookAtTarget();
        this.boss.getNavigation().stop();
        onAbilityStarted(ability);
        ability.onStart(this.boss, this.target);

        // Instant ability execution if windup <= 0
        if (this.attackTimer <= 0) {
            this.activeAbility.execute(this.boss, this.target);
            if (!this.activeAbility.isMultiTickExecution()) {
                this.globalCooldown = this.activeAbility.getRecoveryTicks();
                onAbilityExecuted(this.activeAbility);
                this.activeAbility = null;
            }
        }
    }

    @Override
    public void tick() {
        if (this.target == null || !this.target.isAlive()) {
            findTarget();
            if (this.target == null) {
                if (this.activeAbility != null) {
                    this.activeAbility.stop(this.boss);
                    this.activeAbility = null;
                }
                this.attackTimer = 0;
                return;
            }
        }

        double distanceSq = this.boss.squaredDistanceTo(this.target);

        // 1. Universal Tracking & Movement (only when not in windup or executing multi-tick ability)
        if (this.attackTimer > 0) {
            snapLookAtTarget();
        } else if (this.activeAbility == null) {
            this.boss.getLookControl().lookAt(this.target, 30.0F, 30.0F);
            handleMovement(distanceSq);
        }

        // 2. Universal Teleport Logic (only when idle / chasing)
        if (this.attackTimer <= 0 && this.activeAbility == null) {
            handleTeleportation();
        }

        // 3. Timers
        if (this.globalCooldown > 0) globalCooldown--;
        if (this.teleportCooldown > 0) teleportCooldown--;
        if (this.defensiveCooldown > 0) defensiveCooldown--;

        for (BossAbility ability : abilities) {
            ability.tick(this.boss, this.target);
        }

        // 4. REACTIVE DEFENSE: Intercept incoming projectiles immediately every tick (only when not in active ability)
        if (this.activeAbility == null) {
            handleDefensiveReactions();
        }

        // 5. Delayed Attack Execution Phase (Windup > 0)
        if (this.attackTimer > 0) {
            this.attackTimer--;
            if (this.activeAbility != null) {
                this.activeAbility.tickWindup(this.boss, this.target, this.attackTimer);
            }
            if (this.attackTimer == 0 && activeAbility != null) {
                snapLookAtTarget();
                activeAbility.execute(this.boss, this.target);
                if (!this.activeAbility.isMultiTickExecution()) {
                    this.globalCooldown = activeAbility.getRecoveryTicks();
                    onAbilityExecuted(activeAbility);
                    this.activeAbility = null;
                }
            }
            return;
        }

        // 6. Multi-tick Ability Execution (e.g., Dashes)
        if (this.activeAbility != null && this.activeAbility.isMultiTickExecution()) {
            boolean finished = this.activeAbility.tickExecution(this.boss, this.target);
            if (finished) {
                this.globalCooldown = this.activeAbility.getRecoveryTicks();
                onAbilityExecuted(this.activeAbility);
                this.activeAbility = null;
            }
            return;
        }

        // 7. Select & Trigger New Offensive Ability
        if (this.globalCooldown <= 0) {
            for (BossAbility ability : abilities) {
                if (ability.canUse(this.boss, this.target, distanceSq)) {
                    startAbility(ability);
                    break;
                }
            }
        }
    }

    protected void handleDefensiveReactions() {
        if (this.defensiveCooldown > 0 || this.defensiveAbilities.isEmpty()) return;
        if (this.boss instanceof MethodeEntity methode && methode.getDefensiveMagicCooldown() > 0) return;

        for (DefensiveProjectileShieldAbility defensive : defensiveAbilities) {
            ProjectileEntity incoming = defensive.findIncomingProjectile(this.boss);
            if (incoming != null) {
                Vec3d toProj = incoming.getPos().subtract(this.boss.getEyePos()).normalize();

                // If she already has an active barrier in front of her facing this incoming projectile, skip
                if (BarrierManager.hasActiveBarrierFor(this.boss.getUuid(), toProj)) {
                    return;
                }

                // Snap boss look directly towards the incoming projectile so the barrier faces it
                double dx = toProj.x;
                double dy = toProj.y;
                double dz = toProj.z;
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
                this.boss.setHeadYaw(targetYaw);
                this.boss.setBodyYaw(targetYaw);
                this.boss.setYaw(targetYaw);
                this.boss.setPitch(targetPitch);

                // Instantly execute defensive barrier
                defensive.execute(this.boss, this.target);
                int recovery = defensive.getRecoveryTicks();
                this.defensiveCooldown = recovery;
                if (this.boss instanceof MethodeEntity methode) {
                    methode.setDefensiveMagicCooldown(recovery);
                }
                break;
            }
        }
    }

    protected double getMovementSpeed() {
        return this.speed;
    }

    private void handleMovement(double distanceSq) {
        if (idealDistance < 0) {
            // No ideal distance set — chase normally
            this.boss.getNavigation().startMovingTo(this.target, getMovementSpeed());
            return;
        }

        double distance = Math.sqrt(distanceSq);
        double minDistance = idealDistance - DISTANCE_TOLERANCE;
        double maxDistance = idealDistance + DISTANCE_TOLERANCE;

        if (distance >= minDistance && distance <= maxDistance) {
            // Within comfortable zone — stop moving and face target
            this.boss.getNavigation().stop();
            return;
        }

        if (distance > maxDistance) {
            // Too far — chase the target directly
            this.boss.getNavigation().startMovingTo(this.target, getMovementSpeed());
        } else {
            // Too close — back away to the ideal point
            Vec3d toTarget = this.target.getPos().subtract(this.boss.getPos()).normalize();
            Vec3d destinationPos = this.target.getPos().subtract(toTarget.multiply(idealDistance));
            this.boss.getNavigation().startMovingTo(destinationPos.x, destinationPos.y, destinationPos.z, getMovementSpeed());
        }
    }

    protected void handleTeleportation() {
        if (this.teleportCooldown > 0) return;
        double directDistance = this.boss.distanceTo(this.target);
        if (directDistance <= 6.0) return;

        // Periodically evaluate path to prevent per-tick navigation spam
        if (this.boss.age % 10 == 0) {
            var path = this.boss.getNavigation().findPathTo(this.target, 0);

            boolean shouldTeleport = (path == null || !path.reachesTarget());
            if (!shouldTeleport && path != null && path.getLength() > directDistance * 2.0 && directDistance > 10.0) {
                shouldTeleport = true;
            }
            if (directDistance > 18.0) {
                shouldTeleport = true;
            }

            if (shouldTeleport) {
                Vec3d safePos = findSafePositionNear(this.target.getPos(), 3.0, 5.0);
                if (safePos != null) {
                    performTeleport(safePos);
                }
            }
        }
    }

    protected void performTeleport(Vec3d safePos) {
        this.boss.refreshPositionAndAngles(safePos.x, safePos.y, safePos.z, this.boss.getYaw(), this.boss.getPitch());
        this.boss.getNavigation().stop();
        this.teleportCooldown = 100;
    }

    protected Vec3d findSafePositionNear(Vec3d center, double minR, double maxR) {
        World world = this.boss.getWorld();
        for (int i = 0; i < 10; i++) {
            double angle = this.boss.getRandom().nextDouble() * Math.PI * 2.0;
            double r = minR + this.boss.getRandom().nextDouble() * (maxR - minR);
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockPos targetBlock = BlockPos.ofFloored(x, center.y, z);

            for (int dy = 3; dy >= -3; dy--) {
                BlockPos feetPos = targetBlock.up(dy);
                BlockPos floorPos = feetPos.down();
                BlockPos headPos = feetPos.up();

                if (world.getBlockState(floorPos).isSolidBlock(world, floorPos)
                        && world.getBlockState(feetPos).isAir()
                        && world.getBlockState(headPos).isAir()) {
                    return new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
                }
            }
        }
        return null;
    }
}
