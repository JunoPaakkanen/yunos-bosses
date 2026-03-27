package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.spell.ModSpells;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class UbelAttackGoal extends Goal {
    private final UbelEntity ubel;
    private LivingEntity target;

    private int attackTimer; // Ticks for the current attack animation
    private int cooldownTimer; // Ticks to wait between attacks
    private final double speed; // Movement speed

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
        this.attackTimer = 0;
        this.ubel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        if (this.attackTimer > 0) {
            // Superfast tracking during the attack
            this.ubel.getLookControl().lookAt(
                    this.target.getX(), this.target.getEyeY() - 0.5, this.target.getZ(), 180.0F, 180.0F);
        } else {
            // Normal smooth tracking while walking
            this.ubel.getLookControl().lookAt(this.target, 30.0F, 30.0F);
        }

        double distanceSq = this.ubel.squaredDistanceTo(this.target);

        // --- 1. COOLDOWN LOGIC ---
        if (this.cooldownTimer > 0) {
            this.cooldownTimer--;
        }

        // --- 2. MOVEMENT LOGIC ---
        // Mover closer to target
        if (this.attackTimer <= 0) {
            this.ubel.getNavigation().startMovingTo(this.target, this.speed);
        }

        // --- 3. ATTACK TRIGGER ---
        // If close enough and ready to hit
        if (distanceSq <= 4.0 && this.cooldownTimer <= 0 && this.attackTimer <= 0) {
            this.attackTimer = 20; // Start a 1-second "Attack Phase"

            // START GECKOLIB ANIMATION HERE
            // this.ubel.triggerAnim("controller", "attack");
        }

        // --- 4. THE ATTACK SEQUENCE ---
        if (this.attackTimer > 0) {
            this.attackTimer--;

            // Stop moving during the attack
            this.ubel.getNavigation().stop();

            // Attack halfway through the animation (at tick 10)
            if (this.attackTimer == 10) {
                // Snap to face the target
                this.ubel.setBodyYaw(this.ubel.getHeadYaw());
                this.ubel.setYaw(this.ubel.getHeadYaw());

                // Cast Cutting Magic Reelseiden
                var Spell = ModSpells.CUTTING_MAGIC_REELSEIDEN;
                Spell.cast(
                        this.ubel.getWorld(),
                        this.ubel,
                        this.ubel.getMainHandStack()
                );
            }

            // Once the animation ends, set the next cooldown
            if (this.attackTimer == 0) {
                this.cooldownTimer = 10; // Reset 1 second attack cooldown
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() || !this.ubel.getNavigation().isIdle();
    }
}
