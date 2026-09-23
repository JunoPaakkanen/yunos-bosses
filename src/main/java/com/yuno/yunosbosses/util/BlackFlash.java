package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.TransformationComponent;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.network.BlackFlashPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.BlackFlashSoundManager;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.unlock.UnlockManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class BlackFlash {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public static void blackFlash(LivingEntity user, LivingEntity target) {
        triggerBlackFlash(user, target, false, 1);
    }

    public static void blackFlash(LivingEntity user, LivingEntity target, int chainCount) {
        triggerBlackFlash(user, target, false, chainCount);
    }

    public static void blackFlashFinisher(LivingEntity user, LivingEntity target) {
        triggerBlackFlash(user, target, true, 4);
    }

    public static boolean blackFlashChance(LivingEntity user, LivingEntity target) {
        return blackFlashChance(user, target, 0.03f);
    }

    public static boolean blackFlashChance(LivingEntity user, LivingEntity target, float chance, int chainCount) {
        return blackFlashChance(user, target, chance);
    }

    public static boolean blackFlashChance(LivingEntity user, LivingEntity target, float baseChance) {
        if (user.getWorld().isClient) return false;

        TransformationComponent component = ModEntityComponents.TRANSFORMATION_DATA.getNullable(user);
        float effectiveChance = baseChance;
        int currentChain = 0;

        if (component != null) {
            currentChain = component.getBlackFlashChain();
            if (component.isInTheZone()) {
                if (currentChain > 0) {
                    // Previous hit was a Black Flash: 60% chance to chain
                    effectiveChance = 0.60f;
                } else {
                    // In The Zone, but did not hit one on previous hit: 15% chance
                    effectiveChance = 0.15f;
                }
            }
        }

        if (user.getWorld().random.nextFloat() < effectiveChance) {
            int nextChain = currentChain + 1;
            if (nextChain >= 4) {
                // 4 consecutive Black Flashes: Finisher release
                blackFlashFinisher(user, target);
                if (component != null) {
                    component.resetBlackFlashChain();
                }
            } else {
                triggerBlackFlash(user, target, false, nextChain);
                if (component != null) {
                    component.setBlackFlashChain(nextChain);
                }
            }
            return true;
        } else {
            // Did not hit Black Flash on this strike: break chain back to 0
            if (component != null) {
                component.resetBlackFlashChain();
            }
            return false;
        }
    }

    private static void triggerBlackFlash(LivingEntity user, LivingEntity target, boolean isFinisher, int chainCount) {
        if (user.getWorld().isClient) return;
        if (!(user.getWorld() instanceof ServerWorld serverWorld)) return;

        Vec3d impactPos = target.getBoundingBox().getCenter();
        UUID attackerUuid = user != null ? user.getUuid() : NIL_UUID;
        UUID targetUuid = target.getUuid() != null ? target.getUuid() : NIL_UUID;

        // 1. Initial Damage (Instant contact)
        float baseDamage = isFinisher ? 26.0f : (12.0f + (chainCount * 1.5f));
        target.damage(serverWorld, target.getDamageSources().indirectMagic(user, user), baseDamage);

        // 2. Anime Hitstop / Micro-pause
        // Apply Frame Freeze status effect so target freezes during the inverted negative flash
        int hitstopTicks = isFinisher ? 7 : 4;
        // Apply hitstop to target
        if (target instanceof HitstopData data) {
            data.yunos$setHitstopTicks(hitstopTicks);
            target.setVelocity(Vec3d.ZERO);
            target.velocityModified = true;
        }
        if (user instanceof HitstopData userData) {
            // Two tick hitstop for the user
            userData.yunos$setHitstopTicks(2);
            user.setVelocity(user.getVelocity().multiply(0.1));
            user.velocityModified = true;
        }


        // 3. Broadcast S2C Network Packet to trigger Inverted Negative Shader & 3D Lightning on nearby clients
        BlackFlashPayload payload = new BlackFlashPayload(
                impactPos.x, impactPos.y, impactPos.z,
                attackerUuid, targetUuid, isFinisher, chainCount
        );
        for (ServerPlayerEntity player : serverWorld.getPlayers(p -> p.squaredDistanceTo(impactPos) <= 64 * 64)) {
            ServerPlayNetworking.send(player, payload);
        }

        // 4. Initial Hit Audio: Sharp electric vacuum + Heavy bone-cracking impact
        playInitialSounds(serverWorld, impactPos, isFinisher);

        // 5. Initial Visuals: Server-side procedural branching lightning & cursed sparks
        spawnInitialParticles(serverWorld, impactPos, isFinisher, chainCount);

        // 6. Enter "The Zone" (120% Potential) for User
        applyTheZoneBuffs(user, isFinisher);

        // 7. Delayed Detonation: After hitstop ticks, release explosive kinetic energy and wall slam
        DelayedServerEffects.delay(hitstopTicks, () -> {
            if (!target.isAlive() && target.getHealth() <= 0) return;

            // Delayed Knockback detonation
            Vec3d pushDirection = target.getPos().subtract(user.getPos()).normalize();
            if (pushDirection.lengthSquared() < 0.001) {
                pushDirection = user.getRotationVector();
            }
            double pushStrength = isFinisher ? 7.2 : (4.2 + (chainCount * 0.4));
            double verticalLift = isFinisher ? 0.42 : 0.28;
            target.addVelocity(pushDirection.x * pushStrength, verticalLift, pushDirection.z * pushStrength);
            target.velocityModified = true;

            // Activate Wall Slam timer for massive collision damage
            if (target instanceof WallSlamData data) {
                data.yunos$setWallSlamTimer(isFinisher ? 60 : 40);
            }

            // Delayed Detonation Audio & Shockwave Particles
            playDetonationSounds(serverWorld, target.getPos(), isFinisher);
            spawnDetonationParticles(serverWorld, target.getBoundingBox().getCenter(), pushDirection, isFinisher);

            // Deterministic voiceline playback for this entity with randomized speed & pitch variation
            playBlackFlashSound(serverWorld, target.getPos(), user, isFinisher);
        });

        // Notify unlock progression manager
        UnlockManager.onBlackFlash(user, target);
    }

    private static void spawnInitialParticles(ServerWorld serverWorld, Vec3d center, boolean isFinisher, int chainCount) {
        int branchCount = isFinisher ? 14 : Math.min(12, 8 + chainCount);
        double maxRadius = isFinisher ? 4.5 : 3.0;

        DustParticleEffect crimsonDust = new DustParticleEffect(0xFF0028, 1.8f);
        DustParticleEffect blackDust = new DustParticleEffect(0x050508, 2.2f);

        // Procedural branching lightning rays radiating outward
        for (int b = 0; b < branchCount; b++) {
            double theta = serverWorld.random.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos((serverWorld.random.nextDouble() * 2.0) - 1.0);
            Vec3d dir = new Vec3d(
                    Math.sin(phi) * Math.cos(theta),
                    Math.sin(phi) * Math.sin(theta),
                    Math.cos(phi)
            ).normalize();

            int segments = isFinisher ? 8 : 5;
            double segLength = maxRadius / segments;
            Vec3d current = center;

            for (int s = 0; s < segments; s++) {
                Vec3d jitter = new Vec3d(
                        (serverWorld.random.nextDouble() - 0.5) * 0.35,
                        (serverWorld.random.nextDouble() - 0.5) * 0.35,
                        (serverWorld.random.nextDouble() - 0.5) * 0.35
                );
                current = current.add(dir.multiply(segLength)).add(jitter);

                // Alternating dense crimson and pitch black dust along the lightning bolt
                serverWorld.spawnParticles(crimsonDust, current.x, current.y, current.z, 1, 0.02, 0.02, 0.02, 0.0);
                serverWorld.spawnParticles(blackDust, current.x, current.y, current.z, 1, 0.02, 0.02, 0.02, 0.0);
                if (serverWorld.random.nextFloat() < 0.45f) {
                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, current.x, current.y, current.z, 1, 0.05, 0.05, 0.05, 0.08);
                }
            }
        }

        // Center contact blast
        serverWorld.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.15, 0.15, 0.15, 0.0);
        serverWorld.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, isFinisher ? 30 : 15, 0.3, 0.3, 0.3, 0.25);
        serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, center.x, center.y, center.z, isFinisher ? 25 : 12, 0.4, 0.4, 0.4, 0.15);
    }

    private static void spawnDetonationParticles(ServerWorld serverWorld, Vec3d pos, Vec3d pushDir, boolean isFinisher) {
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, isFinisher ? 3 : 1, 0.3, 0.3, 0.3, 0.0);
        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, isFinisher ? 18 : 8, 0.5, 0.3, 0.5, 0.04);
        serverWorld.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, pos.x, pos.y, pos.z, isFinisher ? 2 : 1, 0.0, 0.0, 0.0, 0.0);

        if (isFinisher) {
            serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverWorld.spawnParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Debris burst in direction of knockback
        serverWorld.spawnParticles(
                ParticleTypes.CRIT,
                pos.x, pos.y, pos.z,
                isFinisher ? 25 : 12,
                pushDir.x * 0.4, 0.2, pushDir.z * 0.4,
                0.3
        );
    }

    private static void applyTheZoneBuffs(LivingEntity user, boolean isFinisher) {
        if (user == null) return;
        // "The Zone" (120% Potential)
        int duration = isFinisher ? 400 : 300; // 15-20 seconds
        TransformationComponent component = ModEntityComponents.TRANSFORMATION_DATA.getNullable(user);
        if (component != null) {
            component.setInTheZone(true, duration);
        }

        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1, false, false, true));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 1, false, false, true));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1, false, false, true));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0, false, false, true));

        // Restore Mana
        var manaComponent = ModEntityComponents.MANA.getNullable(user);
        if (manaComponent != null) {
            float manaRatio = 0.15f;
            manaComponent.addMana(manaComponent.getMaxMana() * manaRatio);
        }
    }

    private static void playInitialSounds(ServerWorld world, Vec3d pos, boolean isFinisher) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.8F, 0.55F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.4F, 1.6F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.8F, 0.6F);
        world.playSound(null, pos.x, pos.y, pos.z, ModSounds.FRAME_FREEZE, SoundCategory.PLAYERS, 1.2F, 1.15F);
        if (isFinisher) {
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.5F, 0.6F);
        }
    }

    private static void playDetonationSounds(ServerWorld world, Vec3d pos, boolean isFinisher) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0F, isFinisher ? 0.60F : 0.75F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.8F, isFinisher ? 1.15F : 1.35F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.8F, 0.45F);
    }

    /**
     * Delegates to BlackFlashSoundManager to play the deterministic voice line for this entity
     * with randomized pitch and speed variation.
     */
    private static void playBlackFlashSound(ServerWorld world, Vec3d pos, LivingEntity user, boolean isFinisher) {
        BlackFlashSoundManager.playBlackFlashSound(world, pos, user, isFinisher);
    }
}
