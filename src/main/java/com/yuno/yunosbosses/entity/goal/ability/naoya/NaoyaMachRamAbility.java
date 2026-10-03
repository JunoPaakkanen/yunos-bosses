package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NaoyaMachRamAbility implements BossAbility {
    private int cooldown = 160;
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
        if (this.cooldown > 0 || !(boss instanceof NaoyaEntity naoya)) return false;
        double distance = Math.sqrt(distanceSq);
        return distance >= 8.0 && distance <= 26.0 &&
                (naoya.getSpeedStacks() >= 4 || naoya.getHealth() < (naoya.getMaxHealth() * 0.5F));
    }

    @Override
    public int getWindupTicks() {
        return 10;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD,
                    boss.getX(), boss.getY() + 0.1, boss.getZ(),
                    4, 0.3, 0.1, 0.3, 0.05);
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    boss.getX(), boss.getY() + 1.0, boss.getZ(),
                    5, 0.3, 0.5, 0.3, 0.1);
        }
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        World world = boss.getWorld();
        Vec3d dir;
        if (target != null) {
            dir = target.getPos().subtract(boss.getPos());
            if (dir.lengthSquared() > 1e-4) {
                dir = dir.normalize();
            } else {
                dir = boss.getRotationVec(1.0F);
            }
        } else {
            dir = boss.getRotationVec(1.0F);
        }

        this.dashWaypoints = ProjectionSorcery.createStraightTrajectory(world, boss, boss.getPos(), dir, 7, 2.8);

        if (this.dashWaypoints.isEmpty()) {
            return;
        }

        // Spawn blue afterimages along the entire charge path
        for (Vec3d pt : this.dashWaypoints) {
            ProjectionSorcery.broadcastFrameImage(world, boss, pt, 50);
        }

        if (world instanceof ServerWorld sw) {
            sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.2F, 1.3F);
            sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    ModSounds.FRAME_SHATTER, SoundCategory.HOSTILE, 1.2F, 1.1F);
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
                sw.spawnParticles(ParticleTypes.EXPLOSION, nextPos.x, nextPos.y + 0.8, nextPos.z, 1, 0.1, 0.1, 0.1, 0.05);
                sw.spawnParticles(ParticleTypes.CRIT, nextPos.x, nextPos.y + 0.8, nextPos.z, 8, 0.3, 0.4, 0.3, 0.15);

                // Ram collision check
                Box box = boss.getBoundingBox().expand(1.8, 1.0, 1.8);
                List<LivingEntity> hitList = sw.getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        e -> e != boss && e.isAlive() && !e.isTeammate(boss)
                );

                int speedStacks = (boss instanceof NaoyaEntity naoya) ? naoya.getSpeedStacks() : 0;
                float ramDamage = 16.0F + (speedStacks * 1.5F);
                DamageSource source = boss.getDamageSources().mobAttack(boss);

                for (LivingEntity hitEntity : hitList) {
                    if (hitEntity.damage(sw, source, ramDamage)) {
                        if (hitEntity instanceof WallSlamData wsd) {
                            wsd.yunos$setWallSlamTimer(35);
                        }
                        Vec3d ramPush = boss.getRotationVec(1.0F).normalize().multiply(1.3).add(0, 0.25, 0);
                        hitEntity.setVelocity(hitEntity.getVelocity().add(ramPush));
                        hitEntity.velocityModified = true;
                    }
                }
            }
            this.waypointIndex++;
            return false;
        } else {
            if (boss instanceof NaoyaEntity naoya) {
                naoya.addSpeedStacks(2);
                naoya.setDashing(false);
            }
            this.cooldown = 180;
            return true;
        }
    }

    @Override
    public int getRecoveryTicks() {
        return 14;
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
