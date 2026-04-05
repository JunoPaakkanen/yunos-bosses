package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.spell.ModSpells;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class UbelAttackGoal extends Goal {
    private final UbelEntity ubel;
    private LivingEntity target;

    private int attackDurationTimer; // Ticks for the current attack animation
    private int cooldownTimer; // Ticks to wait between attacks
    private int teleportCooldown;
    private final double speed; // Movement speed
    private int currentAttackType = 0; // 0: Cutting Magic Reelseiden, 1: Melee

    public UbelAttackGoal(UbelEntity ubel, double speed) {
        this.ubel = ubel;
        this.speed = speed;
        // Controls specify what the entity can't do while this goal is active
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        this.target = this.ubel.getTarget();
        // Start if there is a target and the cooldown is over
        return this.target != null;
    }

    @Override
    public void start() {
        this.cooldownTimer = 10;
    }

    @Override
    public void stop() {
        this.target = null;
        this.attackDurationTimer = 0;
        this.ubel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        if (this.attackDurationTimer > 0) {
            // Superfast tracking during the attack
            this.ubel.getLookControl().lookAt(
                    this.target.getX(), this.target.getEyeY() - 0.5, this.target.getZ(), 180.0F, 180.0F);
        } else {
            // Normal smooth tracking while walking
            this.ubel.getLookControl().lookAt(this.target, 30.0F, 30.0F);
        }

        // --- TELEPORT LOGIC ---
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        double distanceSq = this.ubel.squaredDistanceTo(this.target); // Squared distance
        double directDistance = this.ubel.distanceTo(this.target); // Straight line distance

        var path = this.ubel.getNavigation().findPathTo(this.target, 0);
        boolean shouldTeleport = false;

        if (path == null || !path.reachesTarget()) {
            // Condition A: No path to target exists
            if (directDistance > 5.0) {
                shouldTeleport = true;
            }
        } else {
            // Condition B: Path exists, but it's too long
            double pathLength = path.getLength();
            if (pathLength > directDistance * 2.0 && directDistance > 8.0) {
                shouldTeleport = true;
            }
        }

        if (shouldTeleport && this.teleportCooldown <= 0) {
            this.teleportToTarget();
            this.teleportCooldown = 100; // 5 second cooldown
        }

        // --- COOLDOWN LOGIC ---
        if (this.cooldownTimer > 0) {
            this.cooldownTimer--;
        }

        // --- MOVEMENT LOGIC ---
        // Mover closer to target
        this.ubel.getNavigation().startMovingTo(this.target, this.speed);

        // --- ATTACK TRIGGER ---
        // Ready to hit
        if (this.cooldownTimer <= 0 && this.attackDurationTimer <= 0) {
            // Check if the target is within max range (5 block range for Cutting Magic Reelseiden)
            if (distanceSq <= 25.0) {
                this.attackDurationTimer = 20;
            }
            else {return;}
        }

        // --- THE ATTACK SEQUENCE ---
        if (this.attackDurationTimer > 0) {
            this.attackDurationTimer--;

            // Attack halfway through the attack phase (at tick 10)
            if (this.attackDurationTimer == 10) {
                // Check if the target is within melee range
                if (distanceSq <= 4.0) {
                    this.currentAttackType = 1; // Melee
                }
                else {
                    this.currentAttackType = 0; // Cutting Magic Reelseiden
                }

                // Snap to face the target
                this.ubel.setBodyYaw(this.ubel.getHeadYaw());
                this.ubel.setYaw(this.ubel.getHeadYaw());

                if (this.currentAttackType == 1) {
                    // Melee attack
                    meleeAttack();
                }
                else if (this.currentAttackType == 0) {
                    // Cast Cutting Magic Reelseiden
                    var spell = ModSpells.CUTTING_MAGIC_REELSEIDEN;
                    spell.cast(
                            this.ubel.getWorld(),
                            this.ubel,
                            this.ubel.getMainHandStack()
                    );
                }
            }

            // Once the animation ends, set the next cooldown
            if (this.attackDurationTimer == 0) {
                this.cooldownTimer = 10; // Reset 1 second attack cooldown
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() || !this.ubel.getNavigation().isIdle();
    }

    public void meleeAttack() {
        float damage = 15.0F;
        float knockbackStrength = 1.5F;

        if (this.target.isBlocking()) {
            return;
        }

        // Apply damage
        this.target.damage(this.ubel.getWorld().getDamageSources().mobAttack(this.ubel), damage);
        // Apply knockback
        double deltaX = this.target.getX() - this.ubel.getX();
        double deltaZ = this.target.getZ() - this.ubel.getZ();
        this.target.takeKnockback(knockbackStrength, -deltaX, -deltaZ);
        // Play hit sound and swing animation
        this.ubel.getWorld().playSound(null, this.ubel.getX(), this.ubel.getY(), this.ubel.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                this.ubel.getSoundCategory(), 1.0F, 1.0F);

    }

    private void teleportToTarget() {
        // Attempt to find a valid ground position near the target
        // Look in a 3-block radius around the target
        double tx = this.target.getX() + (this.ubel.getRandom().nextDouble() - 0.5) * 4.0;
        double ty = this.target.getY() + 0.1;
        double tz = this.target.getZ() + (this.ubel.getRandom().nextDouble() - 0.5) * 4.0;

        // Move her
        this.ubel.refreshPositionAndAngles(tx, ty, tz, this.ubel.getYaw(), this.ubel.getPitch());

        // Clear the current (bad) path so she recalculates from her new spot
        this.ubel.getNavigation().stop();

        // Immediate snap-look at player
        this.ubel.getLookControl().lookAt(this.target, 360.0F, 360.0F);
    }
}
