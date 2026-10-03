package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class NaoyaPunchBarrageAbility implements BossAbility {
    private int cooldown = 0;
    private int totalStrikes = 0;
    private int strikeCounter = 0;
    private int executionTimer = 0;
    private boolean hitAny = false;
    private static final int STRIKE_INTERVAL = 2; // Rapid punch every 2 ticks (10 strikes/sec)

    @Override
    public void tick(MobEntity boss, LivingEntity target) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        if (this.cooldown > 0 || !(boss instanceof NaoyaEntity) || target == null || !target.isAlive()) {
            return false;
        }
        // If target is frozen by Frame Freeze, prioritize Frame Shatter
        if (target.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            return false;
        }

        double distance = Math.sqrt(distanceSq);
        return distance >= 1.2 && distance <= 7.5;
    }

    @Override
    public int getWindupTicks() {
        return 5;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        AbstractBossAttackGoal.snapLook(boss, target);
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    boss.getX(), boss.getY() + 0.3, boss.getZ(),
                    3, 0.2, 0.1, 0.2, 0.05);
            if (remainingWindup == 2) {
                sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1.6F);
            }
        }
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        this.hitAny = false;
        this.strikeCounter = 0;
        this.executionTimer = 0;

        int speedStacks = (boss instanceof NaoyaEntity naoya) ? naoya.getSpeedStacks() : 0;
        // Base 6 strikes, +1 strike per 2 speed stacks (up to 13 strikes at 14 stacks)
        this.totalStrikes = 6 + (speedStacks / 2);

        if (boss instanceof NaoyaEntity naoya) {
            naoya.setDashing(true);
            naoya.triggerBarrageAnim();
        }

        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 1.0F, 1.4F);
        }
    }

    @Override
    public boolean isMultiTickExecution() {
        return true;
    }

    @Override
    public boolean tickExecution(MobEntity boss, LivingEntity target) {
        if (target == null || !target.isAlive() || !(boss instanceof NaoyaEntity naoya)) {
            stop(boss);
            return true;
        }

        World world = boss.getWorld();
        if (!(world instanceof ServerWorld sw)) {
            stop(boss);
            return true;
        }

        AbstractBossAttackGoal.snapLook(boss, target);

        // Advancing sprint rush: drive boss relentlessly towards the target
        Vec3d diff = target.getPos().subtract(boss.getPos());
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        Vec3d forward = new Vec3d(diff.x, 0, diff.z);
        if (forward.lengthSquared() > 1e-4) {
            forward = forward.normalize();
        } else {
            forward = boss.getRotationVec(1.0F);
        }

        int speedStacks = naoya.getSpeedStacks();
        double rushSpeed = 0.38 + (speedStacks * 0.02);

        if (horizDist > 1.2) {
            boss.setVelocity(forward.x * rushSpeed, boss.getVelocity().y, forward.z * rushSpeed);
            boss.velocityModified = true;
        }

        // Projection Sorcery trailing frame afterimages & electric sparks
        if (this.executionTimer % 3 == 0) {
            ProjectionSorcery.broadcastFrameImage(world, boss, boss.getPos(), 25);
        }
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                boss.getX(), boss.getY() + 0.6, boss.getZ(),
                1, 0.15, 0.2, 0.15, 0.05);

        this.executionTimer++;

        // Rapid strikes cadence
        if (this.executionTimer % STRIKE_INTERVAL == 0) {
            this.strikeCounter++;

            if (horizDist <= 3.2) {
                // Strike hits target!
                float punchDamage = 2.4F + (speedStacks * 0.3F);
                DamageSource source = boss.getDamageSources().mobAttack(boss);

                if (target.damage(sw, source, punchDamage)) {
                    this.hitAny = true;

                    // Alternating pitch punch sound
                    float pitch = 1.2F + (boss.getRandom().nextFloat() * 0.6F);
                    sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 0.85F, pitch);

                    // Impact particles
                    Vec3d impactPos = target.getEyePos().subtract(0, 0.2, 0);
                    sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
                    sw.spawnParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 4, 0.15, 0.15, 0.15, 0.1);

                    // Relentlessly push target backwards along rush direction
                    Vec3d push = forward.multiply(0.24).add(0, 0.04, 0);
                    target.setVelocity(target.getVelocity().add(push));
                    target.velocityModified = true;

                    // Arm Wall Slam: If the target is driven into a wall during the rush, wall slam triggers!
                    if (target instanceof WallSlamData wsd) {
                        wsd.yunos$setWallSlamTimer(30);
                    }
                }
            } else {
                // Whiff / missed punch swing SFX
                sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.6F, 1.8F);
            }

            // Check if barrage is complete
            if (this.strikeCounter >= this.totalStrikes) {
                // Final Heavy Finisher Punch!
                if (horizDist <= 3.4) {
                    float finisherDamage = 7.0F + (speedStacks * 0.6F);
                    DamageSource source = boss.getDamageSources().mobAttack(boss);
                    if (target.damage(sw, source, finisherDamage)) {
                        this.hitAny = true;
                        target.takeKnockback(0.9F, boss.getX() - target.getX(), boss.getZ() - target.getZ());
                        if (target instanceof WallSlamData wsd) {
                            wsd.yunos$setWallSlamTimer(45);
                        }
                    }

                    sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                            ModSounds.FRAME_SHATTER, SoundCategory.HOSTILE, 1.1F, 1.3F);
                    sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.8F, 1.5F);

                    sw.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                    sw.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.4, 0.4, 0.4, 0.2);
                }

                // Landing the barrage awards +2 speed stacks to Naoya
                if (this.hitAny) {
                    naoya.addSpeedStacks(2);
                }

                naoya.setDashing(false);
                this.cooldown = Math.max(90, 160 - (speedStacks * 4));
                return true;
            }
        }

        // Safety timeout
        if (this.executionTimer > (this.totalStrikes * STRIKE_INTERVAL) + 20) {
            stop(boss);
            return true;
        }

        return false;
    }

    @Override
    public int getRecoveryTicks() {
        return 10;
    }

    @Override
    public void stop(MobEntity boss) {
        if (boss instanceof NaoyaEntity naoya) {
            naoya.setDashing(false);
            naoya.triggerRunAnim();
        }
        this.strikeCounter = 0;
        this.executionTimer = 0;
    }
}
