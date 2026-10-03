package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NaoyaFlankDashAbility implements BossAbility {
    private int cooldown = 60;
    private List<Vec3d> dashWaypoints = new ArrayList<>();
    private int waypointIndex = 0;

    @Override
    public void tick(MobEntity boss, LivingEntity target) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        if (this.cooldown > 0 || target == null || !target.isAlive()) return false;
        double distance = Math.sqrt(distanceSq);
        return distance >= 4.5 && distance <= 22.0;
    }

    @Override
    public int getWindupTicks() {
        return 5;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    boss.getX(), boss.getY() + 0.2, boss.getZ(),
                    3, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        if (target == null) return;
        World world = boss.getWorld();
        boolean clockwise = boss.getRandom().nextBoolean();
        this.dashWaypoints = ProjectionSorcery.createFlankingTrajectory(world, boss, target, 2.5, clockwise, 5);

        if (this.dashWaypoints.isEmpty()) {
            return;
        }

        // Spawn blue afterimage ghosts at all waypoints
        for (Vec3d pt : this.dashWaypoints) {
            ProjectionSorcery.broadcastFrameImage(world, boss, pt, 40);
        }

        if (world instanceof ServerWorld sw) {
            sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    ModSounds.FRAME_SHATTER, SoundCategory.HOSTILE, 1.0F, 1.4F);
        }

        this.waypointIndex = 0;
        if (boss instanceof NaoyaEntity naoya) {
            naoya.setDashing(true);
        }
    }

    @Override
    public boolean isMultiTickExecution() {
        return true;
    }

    @Override
    public boolean tickExecution(MobEntity boss, LivingEntity target) {
        World world = boss.getWorld();

        if (this.dashWaypoints.isEmpty()) {
            if (boss instanceof NaoyaEntity naoya) {
                naoya.setDashing(false);
            }
            return true;
        }

        if (this.waypointIndex < this.dashWaypoints.size()) {
            Vec3d nextPos = this.dashWaypoints.get(this.waypointIndex);
            boss.refreshPositionAndAngles(nextPos.x, nextPos.y, nextPos.z, boss.getYaw(), boss.getPitch());
            boss.fallDistance = 0.0F;

            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.CRIT, nextPos.x, nextPos.y + 0.8, nextPos.z, 5, 0.2, 0.4, 0.2, 0.1);
                sw.playSound(null, nextPos.x, nextPos.y, nextPos.z,
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.7F, 1.6F);
            }
            this.waypointIndex++;
            return false;
        } else {
            // Arrived right behind target: strike immediately!
            AbstractBossAttackGoal.snapLook(boss, target);
            if (boss instanceof NaoyaEntity naoya) {
                naoya.triggerAttackAnim();

                if (world instanceof ServerWorld sw && target != null && target.isAlive()) {
                    float damage = 12.0F + (naoya.getSpeedStacks() * 0.8F);
                    DamageSource source = naoya.getDamageSources().mobAttack(naoya);
                    if (target.damage(sw, source, damage)) {
                        Vec3d push = naoya.getRotationVec(1.0F).normalize().multiply(0.8).add(0, 0.2, 0);
                        target.setVelocity(target.getVelocity().add(push));
                        target.velocityModified = true;
                    }
                }

                naoya.addSpeedStacks(2);
                this.cooldown = Math.max(50, 140 - (naoya.getSpeedStacks() * 5));
                naoya.setDashing(false);
            }
            return true;
        }
    }

    @Override
    public int getRecoveryTicks() {
        return 8;
    }

    @Override
    public void stop(MobEntity boss) {
        this.dashWaypoints.clear();
        this.waypointIndex = 0;
        if (boss instanceof NaoyaEntity naoya) {
            naoya.setDashing(false);
        }
    }
}
