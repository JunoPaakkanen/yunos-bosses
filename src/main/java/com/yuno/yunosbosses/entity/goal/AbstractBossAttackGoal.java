package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractBossAttackGoal extends Goal {
    protected final MobEntity boss;
    protected final double speed;
    protected LivingEntity target;

    protected int globalCooldown = 0;
    protected int teleportCooldown = 0;
    protected int attackTimer = 0;
    protected BossAbility activeAbility = null;

    private final List<BossAbility> abilities = new ArrayList<>();

    // Ideal distance: -1 means "just chase the target normally"
    private double idealDistance = -1;
    private static final double DISTANCE_TOLERANCE = 2.0;

    public AbstractBossAttackGoal(MobEntity boss, double speed) {
        this.boss = boss;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    protected void registerAbility(BossAbility entry) {
        abilities.add(entry);
    }

    protected void setIdealDistance(double distance) {
        this.idealDistance = distance;
    }

    @Override
    public boolean canStart() {
        this.target = this.boss.getTarget();
        return this.target != null && this.target.isAlive();
    }

    @Override
    public void stop() {
        this.target = null;
        this.attackTimer = 0;
        this.activeAbility = null;
        this.boss.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        // 1. Universal Tracking & Movement
        double distanceSq = this.boss.squaredDistanceTo(this.target);
        if (this.attackTimer > 0) {
            this.boss.getLookControl().lookAt(this.target.getX(), this.target.getEyeY() - 0.5, this.target.getZ(), 180.0F, 180.0F);
        } else {
            this.boss.getLookControl().lookAt(this.target, 30.0F, 30.0F);
            handleMovement(distanceSq);
        }

        // 2. Universal Teleport Logic
        handleTeleportation();

        // 3. Timers
        if (this.globalCooldown > 0) globalCooldown--;
        if (this.teleportCooldown > 0) teleportCooldown--;

        // 4. Delayed Attack Execution Phase (Windup > 0)
        if (this.attackTimer > 0) {
            this.attackTimer--;
            if (this.attackTimer == 0 && activeAbility != null) {
                activeAbility.execute(this.boss, this.target);
                this.globalCooldown = activeAbility.getRecoveryTicks();
                this.activeAbility = null;
            }
            return;
        }

        // 5. Select & Trigger New Ability
        if (this.globalCooldown <= 0) {
            for (BossAbility ability : abilities) {
                boolean isDefensive = ability instanceof DefensiveProjectileShieldAbility;

                // Defensive abilities ignore globalCooldown
                if ((this.globalCooldown <= 0 || isDefensive) && ability.canUse(this.boss, this.target, distanceSq)) {
                    this.activeAbility = ability;
                    this.attackTimer = ability.getWindupTicks();

                    // Snap face target
                    this.boss.setBodyYaw(this.boss.getHeadYaw());
                    this.boss.setYaw(this.boss.getHeadYaw());
                    this.boss.getNavigation().stop();

                    // FIX: Handle 0-windup (instant) abilities immediately!
                    if (this.attackTimer <= 0) {
                        this.activeAbility.execute(this.boss, this.target);
                        this.globalCooldown = this.activeAbility.getRecoveryTicks();
                        this.activeAbility = null;
                    }
                    break;
                }
            }
        }
    }

    private void handleMovement(double distanceSq) {
        if (idealDistance < 0) {
            // No ideal distance set — just chase normally
            this.boss.getNavigation().startMovingTo(this.target, this.speed);
            return;
        }

        double distance = Math.sqrt(distanceSq);
        double minDistance = idealDistance - DISTANCE_TOLERANCE;
        double maxDistance = idealDistance + DISTANCE_TOLERANCE;

        if (distance >= minDistance && distance <= maxDistance) {
            // Within the comfortable zone — stop moving
            this.boss.getNavigation().stop();
            return;
        }

        // Calculate the point along the boss→target axis at idealDistance from the target
        Vec3d toTarget = this.target.getPos().subtract(this.boss.getPos()).normalize();
        Vec3d destinationPos = this.target.getPos().subtract(toTarget.multiply(idealDistance));

        if (distance > maxDistance) {
            // Too far — move toward the ideal point (closer to target)
            this.boss.getNavigation().startMovingTo(destinationPos.x, destinationPos.y, destinationPos.z, this.speed);
        } else {
            // Too close — back away to the ideal point (away from target)
            this.boss.getNavigation().startMovingTo(destinationPos.x, destinationPos.y, destinationPos.z, this.speed);
        }
    }

    private void handleTeleportation() {
        if (this.teleportCooldown > 0) return;
        double directDistance = this.boss.distanceTo(this.target);
        var path = this.boss.getNavigation().findPathTo(this.target, 0);

        boolean shouldTeleport = (path == null || !path.reachesTarget()) && directDistance > 5.0;
        if (!shouldTeleport && path != null && path.getLength() > directDistance * 2.0 && directDistance > 8.0) {
            shouldTeleport = true;
        }

        if (shouldTeleport) {
            double tx = this.target.getX() + (this.boss.getRandom().nextDouble() - 0.5) * 4.0;
            double ty = this.target.getY() + 0.1;
            double tz = this.target.getZ() + (this.boss.getRandom().nextDouble() - 0.5) * 4.0;
            this.boss.refreshPositionAndAngles(tx, ty, tz, this.boss.getYaw(), this.boss.getPitch());
            this.boss.getNavigation().stop();
            this.teleportCooldown = 100;
        }
    }
}