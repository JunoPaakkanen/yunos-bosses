package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class NaoyaFrameShatterAbility implements BossAbility {

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        return target != null && target.isAlive() && target.hasStatusEffect(ModEffects.FRAME_FREEZE);
    }

    @Override
    public void onStart(MobEntity boss, LivingEntity target) {
        if (target == null) return;
        double distance = boss.distanceTo(target);
        if (distance > 4.0) {
            World world = boss.getWorld();
            Vec3d dir = target.getPos().subtract(boss.getPos());
            double dist = dir.length();
            if (dist > 1e-4) {
                dir = dir.normalize();
            } else {
                dir = boss.getRotationVec(1.0F);
            }

            // Project 3 frames to close distance right up to 2 blocks in front of target
            double dashDist = Math.max(0.0, dist - 2.0);
            List<Vec3d> dashWaypoints = ProjectionSorcery.createStraightTrajectory(world, boss, boss.getPos(), dir, 3, dashDist / 3.0);

            for (Vec3d pt : dashWaypoints) {
                ProjectionSorcery.broadcastFrameImage(world, boss, pt, 30);
            }

            if (!dashWaypoints.isEmpty()) {
                Vec3d finalPos = dashWaypoints.getLast();
                boss.refreshPositionAndAngles(finalPos.x, finalPos.y, finalPos.z, boss.getYaw(), boss.getPitch());
                boss.fallDistance = 0.0F;
            }

            AbstractBossAttackGoal.snapLook(boss, target);
        }
    }

    @Override
    public int getWindupTicks() {
        return 8;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    boss.getX(), boss.getY() + 1.2, boss.getZ(),
                    2, 0.1, 0.2, 0.1, 0.05);
        }
    }

    @Override
    public int getRecoveryTicks() {
        return 16;
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        if (boss instanceof NaoyaEntity naoya) {
            naoya.triggerAttackAnim();
        }

        World world = boss.getWorld();
        if (world instanceof ServerWorld sw && target != null && target.isAlive()) {
            DamageSource source = boss.getDamageSources().mobAttack(boss);
            // LivingEntityMixin intercepts this because target has FRAME_FREEZE:
            // Boosts damage by 1.5x, calls ProjectionSorcery.shatterFrame, plays sound and particles, and cleanses the freeze!
            target.damage(sw, source, 22.0F);

            // Enable Wall Slam detonation if knocked into a wall
            if (target instanceof WallSlamData wsd) {
                wsd.yunos$setWallSlamTimer(40);
            }

            // Explosive launch knockback
            Vec3d launch = boss.getRotationVec(1.0F).normalize().multiply(1.8).add(0, 0.35, 0);
            target.setVelocity(launch);
            target.velocityModified = true;
        }

        if (boss instanceof NaoyaEntity naoya) {
            naoya.addSpeedStacks(3);
        }
    }
}
