package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.BossCombatHelper;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Naoya's Sonic Uppercut:
 * Projects vertical 24-FPS frames upwards to intercept elevated or airborne targets
 * (e.g., pillaring players or jump-kiters), delivering a supersonic rising uppercut
 * that shatters guards and launches the target with a Mach shockwave ring.
 */
public class NaoyaSonicUppercutAbility implements BossAbility {
    private static final DustParticleEffect SONIC_CYAN_DUST = new DustParticleEffect(0x00E5FF, 1.15f);

    private int cooldown = 0;
    private final List<Vec3d> ascentWaypoints = new ArrayList<>();
    private int waypointIndex = 0;

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

        double dy = target.getY() - boss.getY();
        double dx = target.getX() - boss.getX();
        double dz = target.getZ() - boss.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // Trigger when target is elevated (pillaring, jumping high, or on high terrain) and within 16 blocks horizontally
        return dy >= 2.0 && dy <= 18.0 && horizDist <= 16.0;
    }

    @Override
    public int getWindupTicks() {
        return 6;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        AbstractBossAttackGoal.snapLook(boss, target);
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw) {
            // Gathering ground-level supersonic slipstream & wind vortex around feet
            double radius = 1.4 - (remainingWindup * 0.15);
            for (int i = 0; i < 6; i++) {
                double angle = (remainingWindup * 0.8) + (i * Math.PI / 3.0);
                double px = boss.getX() + Math.cos(angle) * radius;
                double pz = boss.getZ() + Math.sin(angle) * radius;
                sw.spawnParticles(ParticleTypes.CLOUD, px, boss.getY() + 0.15, pz, 1, 0.05, 0.05, 0.05, 0.02);
                sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, boss.getY() + 0.3, pz, 1, 0.05, 0.1, 0.05, 0.05);
            }

            if (remainingWindup == 5) {
                sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.ENTITY_BREEZE_CHARGE, SoundCategory.HOSTILE, 1.5F, 1.4F);
            }
            if (remainingWindup == 2) {
                sw.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        ModSounds.FRAME_FREEZE, SoundCategory.HOSTILE, 1.2F, 1.5F);
            }
        }
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        this.ascentWaypoints.clear();
        this.waypointIndex = 0;

        if (target == null) return;
        World world = boss.getWorld();

        Vec3d startPos = boss.getPos();
        // Aim to arrive right beside/under the elevated target at apex
        Vec3d toTargetHoriz = new Vec3d(target.getX() - startPos.x, 0, target.getZ() - startPos.z);
        Vec3d offsetDir = toTargetHoriz.lengthSquared() > 1e-4 ? toTargetHoriz.normalize() : boss.getRotationVec(1.0F);
        Vec3d endPos = target.getPos().subtract(offsetDir.multiply(0.9));

        int frames = 5;
        for (int i = 1; i <= frames; i++) {
            double t = (double) i / frames;
            // Smooth curve that surges horizontally early and rockets vertically upward
            double horizT = Math.sin(t * (Math.PI / 2.0));
            double vertT = Math.pow(t, 0.85);

            double wx = startPos.x + (endPos.x - startPos.x) * horizT;
            double wy = startPos.y + (endPos.y - startPos.y) * vertT;
            double wz = startPos.z + (endPos.z - startPos.z) * horizT;

            Vec3d framePos = new Vec3d(wx, wy, wz);
            this.ascentWaypoints.add(framePos);
            ProjectionSorcery.broadcastFrameImage(world, boss, framePos, 35);
        }

        if (world instanceof ServerWorld sw) {
            // Explosive ground launch shockwave
            sw.playSound(null, startPos.x, startPos.y, startPos.z,
                    SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), SoundCategory.HOSTILE, 1.8F, 1.15F);
            sw.playSound(null, startPos.x, startPos.y, startPos.z,
                    ModSounds.FRAME_SHATTER, SoundCategory.HOSTILE, 1.3F, 1.35F);
            sw.playSound(null, startPos.x, startPos.y, startPos.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.1F, 1.6F);

            sw.spawnParticles(ParticleTypes.GUST_EMITTER_SMALL, startPos.x, startPos.y + 0.2, startPos.z, 1, 0, 0, 0, 0);
            for (int deg = 0; deg < 360; deg += 20) {
                double rad = Math.toRadians(deg);
                double rx = startPos.x + Math.cos(rad) * 1.5;
                double rz = startPos.z + Math.sin(rad) * 1.5;
                sw.spawnParticles(ParticleTypes.CLOUD, rx, startPos.y + 0.15, rz, 1, Math.cos(rad) * 0.15, 0.05, Math.sin(rad) * 0.15, 0.1);
                sw.spawnParticles(SONIC_CYAN_DUST, rx, startPos.y + 0.25, rz, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

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

        if (this.ascentWaypoints.isEmpty() || !(boss instanceof NaoyaEntity naoya)) {
            stop(boss);
            return true;
        }

        if (this.waypointIndex < this.ascentWaypoints.size()) {
            Vec3d nextPos = this.ascentWaypoints.get(this.waypointIndex);
            boss.refreshPositionAndAngles(nextPos.x, nextPos.y, nextPos.z, boss.getYaw(), boss.getPitch());
            boss.fallDistance = 0.0F;

            if (world instanceof ServerWorld sw) {
                // Rising helical Mach slipstream along vertical trajectory
                for (int c = 0; c < 4; c++) {
                    double angle = (this.waypointIndex * 1.3) + (c * Math.PI / 2.0);
                    double sx = nextPos.x + Math.cos(angle) * 0.85;
                    double sz = nextPos.z + Math.sin(angle) * 0.85;
                    sw.spawnParticles(SONIC_CYAN_DUST, sx, nextPos.y + 0.6, sz, 1, 0.02, 0.05, 0.02, 0.02);
                    sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, sx, nextPos.y + 0.6, sz, 2, 0.05, 0.1, 0.05, 0.04);
                }
                sw.spawnParticles(ParticleTypes.CLOUD, nextPos.x, nextPos.y + 0.4, nextPos.z, 3, 0.2, 0.2, 0.2, 0.05);
                sw.playSound(null, nextPos.x, nextPos.y, nextPos.z,
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.9F, 1.5F + (this.waypointIndex * 0.1F));
            }

            this.waypointIndex++;
            return false;
        } else {
            // Reached apex! Deliver the Sonic Uppercut impact!
            if (target != null && target.isAlive()) {
                AbstractBossAttackGoal.snapLook(boss, target);
            }
            naoya.triggerAttackAnim();
            boss.fallDistance = 0.0F;

            if (world instanceof ServerWorld sw) {
                Vec3d apexPos = boss.getPos();
                boolean inDomain = naoya.isInOwnDomain();
                int speedStacks = naoya.getSpeedStacks();

                // Climax sonic boom audio
                sw.playSound(null, apexPos.x, apexPos.y + 1.0, apexPos.z,
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.7F, 1.45F);
                sw.playSound(null, apexPos.x, apexPos.y + 1.0, apexPos.z,
                        ModSounds.FRAME_SHATTER, SoundCategory.HOSTILE, 1.4F, 1.25F);
                sw.playSound(null, apexPos.x, apexPos.y + 1.0, apexPos.z,
                        SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), SoundCategory.HOSTILE, 1.6F, 1.3F);

                // Visual Mach cone / shockwave ring at apex
                sw.spawnParticles(ParticleTypes.SONIC_BOOM, apexPos.x, apexPos.y + 1.1, apexPos.z, 1, 0, 0, 0, 0);
                sw.spawnParticles(ModParticles.FRAME_SHATTER_PARTICLE, apexPos.x, apexPos.y + 1.1, apexPos.z, 1, 0.1, 0.1, 0.1, 0.05);
                sw.spawnParticles(ParticleTypes.FLASH, apexPos.x, apexPos.y + 1.1, apexPos.z, 1, 0, 0, 0, 0);

                for (int deg = 0; deg < 360; deg += 15) {
                    double rad = Math.toRadians(deg);
                    double rx = apexPos.x + Math.cos(rad) * 2.2;
                    double rz = apexPos.z + Math.sin(rad) * 2.2;
                    sw.spawnParticles(ParticleTypes.END_ROD, rx, apexPos.y + 1.0, rz, 1, Math.cos(rad) * 0.12, 0.08, Math.sin(rad) * 0.12, 0.05);
                    sw.spawnParticles(SONIC_CYAN_DUST, rx, apexPos.y + 1.0, rz, 2, 0.1, 0.1, 0.1, 0.02);
                    sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, rx, apexPos.y + 1.0, rz, 2, 0.15, 0.15, 0.15, 0.08);
                }

                // Hit all enemies in uppercut reach around the apex
                Box hitBox = boss.getBoundingBox().expand(2.8, 2.5, 2.8);
                List<LivingEntity> hitEntities = sw.getEntitiesByClass(
                        LivingEntity.class,
                        hitBox,
                        e -> e != boss && !(e instanceof NaoyaEntity) && e.isAlive() && !e.isTeammate(boss)
                );

                float damage = 15.0F + (speedStacks * 1.1F);

                for (LivingEntity hit : hitEntities) {
                    // Shatter shield if blocking the sonic uppercut
                    if (hit.isBlocking() && hit instanceof PlayerEntity player) {
                        player.getItemCooldownManager().set(player.getActiveItem(), 70);
                        player.clearActiveItem();
                        sw.sendEntityStatus(player, (byte) 30);
                        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.2F, 1.0F);
                    }

                    BossCombatHelper.applySignatureDamage(sw, hit, boss, damage);

                    if (hit instanceof WallSlamData wsd) {
                        wsd.yunos$setWallSlamTimer(inDomain ? 55 : 40);
                    }

                    Vec3d horizDir = new Vec3d(hit.getX() - boss.getX(), 0, hit.getZ() - boss.getZ());
                    if (horizDir.lengthSquared() > 1e-4) {
                        horizDir = horizDir.normalize();
                    } else {
                        horizDir = boss.getRotationVec(1.0F).normalize();
                    }

                    double horizKb = inDomain ? 2.4 : 1.1;
                    double vertKb = inDomain ? 1.15 : 0.78;

                    hit.setVelocity(horizDir.x * horizKb, vertKb, horizDir.z * horizKb);
                    hit.velocityModified = true;

                    sw.spawnParticles(ParticleTypes.CRIT, hit.getX(), hit.getY() + 1.0, hit.getZ(), 16, 0.3, 0.4, 0.3, 0.18);
                }

                naoya.addSpeedStacks(2);
                this.cooldown = Math.max(50, 110 - (speedStacks * 4));
                naoya.setDashing(false);
            }

            return true;
        }
    }

    @Override
    public int getRecoveryTicks() {
        return 10;
    }

    @Override
    public void stop(MobEntity boss) {
        this.ascentWaypoints.clear();
        this.waypointIndex = 0;
        if (boss instanceof NaoyaEntity naoya) {
            naoya.setDashing(false);
        }
    }
}
