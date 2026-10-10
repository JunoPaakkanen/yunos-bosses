package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.implementation.offensive.Shrine;
import com.yuno.yunosbosses.util.DuoModeCheckHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.EnumSet;

public class UbelAttackGoal extends Goal {
    private final UbelEntity ubel;
    private LivingEntity target;

    private int attackDurationTimer; // Ticks for the current attack animation
    private int cooldownTimer; // Ticks to wait between attacks
    private int teleportCooldown;
    private int enhancedDismantleCooldown;
    private final double speed; // Movement speed
    private int currentAttackType = 0; // 0: Cutting Magic Reelseiden, 1: Melee, 2: Long range Dismantle, 3: Enhanced Dismantle
    private boolean usedDomainExpansion = false;

    public UbelAttackGoal(UbelEntity ubel, double speed) {
        this.ubel = ubel;
        this.speed = speed;
        // Controls specify what the entity can't do while this goal is active
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity currentTarget = this.ubel.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            if (currentTarget instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
                this.ubel.setTarget(null);
                return false;
            }
            if (this.ubel.squaredDistanceTo(currentTarget) > 40.0 * 40.0) {
                this.ubel.setTarget(null);
                return false;
            }
            this.target = currentTarget;
            return true;
        }

        findTarget();
        return this.target != null && this.target.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        if (this.target == null || !this.target.isAlive()) {
            this.ubel.setTarget(null);
            this.ubel.setAttacker(null);
            this.target = null;
            return false;
        }
        if (this.target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            this.ubel.setTarget(null);
            this.ubel.setAttacker(null);
            this.target = null;
            return false;
        }
        if (this.ubel.squaredDistanceTo(this.target) > 40.0 * 40.0) {
            this.ubel.setTarget(null);
            this.ubel.setAttacker(null);
            this.target = null;
            return false;
        }
        return true;
    }

    private void findTarget() {
        LivingEntity attacker = this.ubel.getAttacker();
        if (attacker != null && attacker.isAlive() && !attacker.isSpectator()) {
            if (!(attacker instanceof PlayerEntity player && player.isCreative())) {
                if (this.ubel.squaredDistanceTo(attacker) <= 40.0 * 40.0) {
                    this.ubel.setTarget(attacker);
                    this.target = attacker;
                    return;
                }
            }
        }

        PlayerEntity nearest = this.ubel.getWorld().getClosestPlayer(this.ubel, 32.0);
        if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && nearest.isAlive()) {
            this.ubel.setTarget(nearest);
            this.target = nearest;
        } else {
            this.ubel.setTarget(null);
            this.target = null;
        }
    }

    @Override
    public void start() {
        this.cooldownTimer = 10;
        this.enhancedDismantleCooldown = 200;
    }

    @Override
    public void stop() {
        this.target = null;
        this.attackDurationTimer = 0;
        this.ubel.getNavigation().stop();
    }

    private void snapLookAtTarget() {
        if (this.target == null) return;
        this.ubel.getLookControl().lookAt(this.target, 180.0F, 180.0F);
        double dx = this.target.getX() - this.ubel.getX();
        double dy = (this.target.getEyeY() - 0.2) - this.ubel.getEyeY();
        double dz = this.target.getZ() - this.ubel.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
        this.ubel.setHeadYaw(targetYaw);
        this.ubel.setBodyYaw(targetYaw);
        this.ubel.setYaw(targetYaw);
        this.ubel.setPitch(targetPitch);
    }

    private void onAttackFinished() {
        if (DuoModeCheckHelper.isDuoMode(this.ubel.getWorld())) {
            // In Duo mode, look for the other nearby attacker after finishing an attack
            LivingEntity attacker = this.ubel.getAttacker();
            if (attacker != null && attacker.isAlive() && attacker != this.target && this.ubel.squaredDistanceTo(attacker) <= 32.0 * 32.0) {
                // 50% chance to pivot to the off-target player
                if (this.ubel.getRandom().nextFloat() < 0.50F) {
                    this.ubel.setTarget(attacker);
                    this.target = attacker;
                }
            }
        }
    }

    @Override
    public void tick() {
        // Sync target if boss acquired a new valid target (e.g. from RevengeGoal or targetSelector)
        LivingEntity currentBossTarget = this.ubel.getTarget();
        if (currentBossTarget != null && currentBossTarget.isAlive() && currentBossTarget != this.target) {
            if (!(currentBossTarget instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()))) {
                this.target = currentBossTarget;
            }
        }

        if (this.target == null || !this.target.isAlive()) {
            this.ubel.setTarget(null);
            this.ubel.setAttacker(null);
            this.target = null;
            this.attackDurationTimer = 0;
            this.stop();
            return;
        }

        double distanceSq = this.ubel.squaredDistanceTo(this.target);
        if (distanceSq > 40.0 * 40.0) {
            this.ubel.setTarget(null);
            this.ubel.setAttacker(null);
            this.target = null;
            this.attackDurationTimer = 0;
            this.stop();
            return;
        }

        if (this.attackDurationTimer > 0) {
            snapLookAtTarget();
        } else {
            this.ubel.getLookControl().lookAt(this.target, 30.0F, 30.0F);
        }

        // --- TELEPORT COOLDOWN ---
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        double directDistance = this.ubel.distanceTo(this.target);
        double dy = this.target.getY() - this.ubel.getY();
        boolean verticallyUnreachable = dy > 1.8;
        boolean obstructed = !this.ubel.canSee(this.target);

        // --- SAFE TELEPORT LOGIC (periodically evaluated to avoid pathfinding spam) ---
        if (this.teleportCooldown <= 0 && this.ubel.age % 10 == 0 && directDistance <= 32.0) {
            boolean canBypassDistance = verticallyUnreachable || (obstructed && directDistance <= 6.0);
            if (directDistance > 6.0 || canBypassDistance) {
                var path = this.ubel.getNavigation().findPathTo(this.target, 0);
                boolean shouldTeleport = (path == null || !path.reachesTarget());
                if (canBypassDistance) {
                    shouldTeleport = true;
                }
                if (!shouldTeleport && path != null && path.getLength() > directDistance * 2.0 && directDistance > 10.0) {
                    shouldTeleport = true;
                }
                if (directDistance > 18.0 && directDistance <= 32.0) {
                    shouldTeleport = true;
                }

                if (shouldTeleport) {
                    this.teleportToTarget();
                    this.teleportCooldown = 100; // 5-second cooldown
                }
            }
        }

        // --- COUNTER DOMAIN EXPANSION (INSTANT CLASH RESPONSE) ---
        if (!this.usedDomainExpansion && !DomainClashManager.hasBurnout(this.ubel.getUuid())) {
            if (DomainClashManager.isEntityCastingDomainNear(this.ubel, 35.0)) {
                this.teleportToTarget();
                this.domainExpansion();
                this.ubel.triggerDomainAnim();
                this.usedDomainExpansion = true;
                this.attackDurationTimer = 80;
            }
        }

        // --- DOMAIN EXPANSION (HEALTH THRESHOLD: <= 60% HP) ---
        if (this.ubel.getHealth() <= (this.ubel.getMaxHealth() * 0.60F) && !this.usedDomainExpansion && !DomainClashManager.hasBurnout(this.ubel.getUuid())) {
            this.teleportToTarget();
            this.domainExpansion();
            this.ubel.triggerDomainAnim();
            this.usedDomainExpansion = true;
            this.attackDurationTimer = 80;
        }

        // --- COOLDOWN LOGIC ---
        if (this.cooldownTimer > 0) {
            this.cooldownTimer--;
        }
        if (this.enhancedDismantleCooldown > 0) {
            this.enhancedDismantleCooldown--;
        }

        // --- MOVEMENT LOGIC ---
        // Move closer to target unless charging Enhanced Dismantle
        if (this.currentAttackType != 3) {
            this.ubel.getNavigation().startMovingTo(this.target, this.speed);
        }

        // --- ATTACK TRIGGER ---
        // Ready to hit
        if (this.cooldownTimer <= 0 && this.attackDurationTimer <= 0) {
            // Check if the target is within 25-block range to start the attack sequence
            if (distanceSq <= 625.0) {
                // Enhanced Dismantle chance when at range (> 6 blocks)
                if (this.enhancedDismantleCooldown <= 0 && distanceSq > 36.0 && this.ubel.getRandom().nextFloat() <= 0.30F) {
                    this.currentAttackType = 3; // Enhanced Dismantle
                    this.attackDurationTimer = 100;
                    this.enhancedDismantleCooldown = 600; // 30-second cooldown

                    // Levitate Ubel
                    this.ubel.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1));
                    this.ubel.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0));
                    this.ubel.getNavigation().stop();

                    // Play Dismantle animation
                    this.ubel.triggerDismantleAnim();
                } else {
                    // Standard attacks
                    this.currentAttackType = 0;
                    this.attackDurationTimer = 20;
                }
            }
        }

        // --- THE ATTACK SEQUENCE ---
        if (this.attackDurationTimer > 0) {
            this.attackDurationTimer--;

            if (this.currentAttackType == 3) {
                // Enhanced Dismantle
                snapLookAtTarget();

                // Fire the spell after 30 ticks
                if (this.attackDurationTimer == 70) {
                    snapLookAtTarget();

                    var spell = (Shrine) ModSpells.SHRINE;
                    spell.fireDismantle(this.ubel.getWorld(), this.ubel, this.ubel.getMainHandStack(), 3.0F);
                }

                if (this.attackDurationTimer == 0) {
                    this.cooldownTimer = DuoModeCheckHelper.isDuoMode(this.ubel.getWorld()) ? 14 : 20; // Shorter attack cooldown in Duo mode.
                    onAttackFinished();
                }
            } else {
                // Trigger attack animation
                if (this.attackDurationTimer == 15) {
                    this.ubel.triggerMeleeAnim();
                }

                // Attack halfway through the attack phase (at tick 10)
                if (this.attackDurationTimer == 10) {
                    snapLookAtTarget();

                    // Check ranges
                    if (distanceSq <= 9.0) {
                        this.currentAttackType = 1; // Melee (0 to 3 blocks)
                        meleeAttack();
                    } else if (distanceSq <= 36.0) {
                        this.currentAttackType = 0; // Cutting Magic Reelseiden (3 to 6 blocks)
                        var spell = ModSpells.CUTTING_MAGIC_REELSEIDEN;
                        spell.cast(this.ubel.getWorld(), this.ubel, this.ubel.getMainHandStack());
                    } else {
                        this.currentAttackType = 2; // Long range Dismantle (6 to 25 blocks)
                        var spell = (Shrine) ModSpells.SHRINE;
                        spell.fireDismantle(this.ubel.getWorld(), this.ubel, this.ubel.getMainHandStack(), 1.0F);
                    }
                }

                // Once the animation ends, set the next cooldown
                if (this.attackDurationTimer == 0) {
                    this.cooldownTimer = DuoModeCheckHelper.isDuoMode(this.ubel.getWorld()) ? 7 : 10;
                    onAttackFinished();
                }
            }
        }
    }

    public void meleeAttack() {
        float damage = 15.0F;
        float knockbackStrength = 1.5F;

        if (this.target.isBlocking()) {
            return;
        }

        // Apply damage
        this.target.damage((ServerWorld) this.ubel.getWorld(), this.ubel.getWorld().getDamageSources().mobAttack(this.ubel), damage);
        // Apply knockback
        double deltaX = this.target.getX() - this.ubel.getX();
        double deltaZ = this.target.getZ() - this.ubel.getZ();
        this.target.takeKnockback(knockbackStrength, -deltaX, -deltaZ);
        // Play hit sound
        this.ubel.getWorld().playSound(null, this.ubel.getX(), this.ubel.getY(), this.ubel.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                this.ubel.getSoundCategory(), 1.0F, 1.0F);
    }

    public void domainExpansion() {
        ModSpells.DOMAIN_EXPANSION_SHRINE.cast(
                this.ubel.getWorld(),
                this.ubel,
                this.ubel.getMainHandStack()
        );
    }

    private void teleportToTarget() {
        if (this.target == null || !this.target.isAlive() || this.ubel.distanceTo(this.target) > 32.0) {
            return;
        }
        Vec3d safePos = findSafePositionNear(this.target.getPos(), 2.0, 5.0);
        if (safePos != null) {
            this.ubel.refreshPositionAndAngles(safePos.x, safePos.y, safePos.z, this.ubel.getYaw(), this.ubel.getPitch());
            this.ubel.getNavigation().stop();
            snapLookAtTarget();
        }
    }

    private Vec3d findSafePositionNear(Vec3d center, double minR, double maxR) {
        World world = this.ubel.getWorld();
        for (int i = 0; i < 16; i++) {
            double angle = this.ubel.getRandom().nextDouble() * Math.PI * 2.0;
            double r = minR + this.ubel.getRandom().nextDouble() * (maxR - minR);
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockPos targetCol = BlockPos.ofFloored(x, center.y, z);

            // 1. Search vertically around target Y (from dy = +4 down to dy = -12)
            // Catches elevated pillars, hills, floors, and platforms
            for (int dy = 4; dy >= -12; dy--) {
                BlockPos feetPos = targetCol.up(dy);
                if (isValidStandPosition(world, feetPos)) {
                    return new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
                }
            }

            // 2. Fallback: world surface top position (for extreme heights / pillars)
            BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetCol);
            if (isValidStandPosition(world, topPos)) {
                return new Vec3d(topPos.getX() + 0.5, topPos.getY(), topPos.getZ() + 0.5);
            }
        }
        return null;
    }

    private boolean isValidStandPosition(World world, BlockPos feetPos) {
        BlockPos floorPos = feetPos.down();
        BlockPos headPos = feetPos.up();
        BlockState floorState = world.getBlockState(floorPos);
        BlockState feetState = world.getBlockState(feetPos);
        BlockState headState = world.getBlockState(headPos);

        boolean solidFloor = floorState.isSolidBlock(world, floorPos)
                || floorState.isOpaqueFullCube()
                || floorState.hasSolidTopSurface(world, floorPos, this.ubel);

        boolean clearFeet = feetState.getCollisionShape(world, feetPos).isEmpty();
        boolean clearHead = headState.getCollisionShape(world, headPos).isEmpty();

        return solidFloor && clearFeet && clearHead;
    }
}
