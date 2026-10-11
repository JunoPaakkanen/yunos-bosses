package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ShrineAtmosphereSoundInstance;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Handles immersive atmospheric weather, fallout effects, and ambient audio within Domain Expansions.
 * Simulates Malevolent Shrine (pulverized concrete storm, cursed ash, cyclonic vortex)
 * and Time Cell Moon Palace (lunar starlight dust, cellular dust, optical resonance).
 */
public class DomainAtmosphereRenderer {
    private static final DustParticleEffect CONCRETE_DUST = new DustParticleEffect(0x9E9994, 0.75f);
    private static final DustParticleEffect CURSED_ASH = new DustParticleEffect(0x8C141C, 0.65f);
    private static final DustParticleEffect TCMP_LUNAR_DUST = new DustParticleEffect(0xE0F7FA, 0.75f);
    private static final DustParticleEffect TCMP_CELL_DUST = new DustParticleEffect(0xC2185B, 0.65f);

    // Looping audio instances
    private static ShrineAtmosphereSoundInstance activeDroneSound = null;
    private static ShrineAtmosphereSoundInstance activeWindSound = null;
    private static ShrineAtmosphereSoundInstance activeResonanceSound = null;
    private static int stingerCooldown = 40;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        Random rand = world.getRandom();

        boolean insideAnyShrine = false;
        boolean insideAnyTcmp = false;

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
            if (!barrier.getDirection().equals(Vec3d.ZERO)) continue;

            boolean isShrine = barrier.getTexture() != null && barrier.getTexture().getPath().contains("shrine");
            boolean isTcmp = barrier.getTexture() != null && barrier.getTexture().getPath().contains("tcmp");
            if (!isShrine && !isTcmp) continue;

            Vec3d center = barrier.getPosition();
            float radius = barrier.getRadius();
            double distToCenter = player.getPos().distanceTo(center);

            if (distToCenter <= radius + 3.0) {
                if (isShrine) insideAnyShrine = true;
                if (isTcmp) insideAnyTcmp = true;
            }

            if (distToCenter > radius + 12.0) continue;

            if (isShrine) {
                // --- MALEVOLENT SHRINE PARTICLES ---
                int particleCount = 35;
                double spawnRadius = Math.min(24.0, radius);

                for (int i = 0; i < particleCount; i++) {
                    double angle = rand.nextDouble() * 2.0 * Math.PI;
                    double minClearDist = 0.8;
                    double dist = minClearDist + (rand.nextDouble() * Math.max(0.1, spawnRadius - minClearDist));
                    double px = player.getX() + Math.cos(angle) * dist;
                    double pz = player.getZ() + Math.sin(angle) * dist;
                    double py = player.getY() + 1.0 + (rand.nextDouble() * 12.0);

                    double dx = px - center.x;
                    double dz = pz - center.z;
                    double distSq = dx * dx + dz * dz;
                    if (distSq > (radius * 1.05) * (radius * 1.05)) continue;

                    double dLen = Math.sqrt(distSq);
                    double swirlX = 0.0;
                    double swirlZ = 0.0;
                    if (dLen > 0.5) {
                        swirlX = (-dz / dLen) * 0.045;
                        swirlZ = (dx / dLen) * 0.045;
                        swirlX += (dx / dLen) * 0.012;
                        swirlZ += (dz / dLen) * 0.012;
                    }

                    double vx = swirlX + (rand.nextDouble() - 0.5) * 0.025;
                    double vy = -0.04 - (rand.nextDouble() * 0.05);
                    double vz = swirlZ + (rand.nextDouble() - 0.5) * 0.025;

                    int typeRoll = rand.nextInt(12);
                    if (typeRoll < 5) {
                        world.addParticleClient(ParticleTypes.WHITE_ASH, px, py, pz, vx, vy, vz);
                    } else if (typeRoll < 8) {
                        world.addParticleClient(ParticleTypes.ASH, px, py, pz, vx, vy, vz);
                    } else if (typeRoll < 10) {
                        world.addParticleClient(CONCRETE_DUST, px, py, pz, vx * 0.5, vy * 0.6, vz * 0.5);
                    } else {
                        world.addParticleClient(CURSED_ASH, px, py, pz, vx * 0.5, vy * 0.6, vz * 0.5);
                    }

                    if (rand.nextInt(15) == 0) {
                        world.addParticleClient(ModParticles.FLAME_EMBER_PARTICLE, px, py, pz, vx * 1.4, vy * 0.4, vz * 1.4);
                    }
                }
            } else if (isTcmp) {
                // --- TIME CELL MOON PALACE PARTICLES ---
                int particleCount = 20;
                double spawnRadius = Math.min(20.0, radius);

                for (int i = 0; i < particleCount; i++) {
                    double angle = rand.nextDouble() * 2.0 * Math.PI;
                    double dist = 0.8 + (rand.nextDouble() * Math.max(0.1, spawnRadius - 0.8));
                    double px = player.getX() + Math.cos(angle) * dist;
                    double pz = player.getZ() + Math.sin(angle) * dist;
                    double py = player.getY() + 0.5 + (rand.nextDouble() * 8.0);

                    double dx = px - center.x;
                    double dz = pz - center.z;
                    if (dx * dx + dz * dz > (radius * 0.98) * (radius * 0.98)) continue;

                    // Slow floating suspension (lunar & cellular dust)
                    double vx = (rand.nextDouble() - 0.5) * 0.012;
                    double vy = -0.01 - (rand.nextDouble() * 0.02);
                    double vz = (rand.nextDouble() - 0.5) * 0.012;

                    int roll = rand.nextInt(10);
                    if (roll < 5) {
                        world.addParticleClient(TCMP_LUNAR_DUST, px, py, pz, vx, vy, vz);
                    } else if (roll < 9) {
                        world.addParticleClient(TCMP_CELL_DUST, px, py, pz, vx, vy, vz);
                    } else {
                        world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, px, py, pz, vx * 2.0, vy * 2.0, vz * 2.0);
                    }
                }
            }
        }

        // --- AMBIENT AUDIO SYSTEM ---
        if (insideAnyShrine) {
            if (activeDroneSound == null || activeDroneSound.isDone() || !client.getSoundManager().isPlaying(activeDroneSound)) {
                activeDroneSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_BEACON_AMBIENT, player, 0.85f, 0.42f);
                client.getSoundManager().play(activeDroneSound);
            }
            if (activeWindSound == null || activeWindSound.isDone() || !client.getSoundManager().isPlaying(activeWindSound)) {
                activeWindSound = new ShrineAtmosphereSoundInstance(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(), player, 0.70f, 0.60f);
                client.getSoundManager().play(activeWindSound);
            }
            if (activeResonanceSound == null || activeResonanceSound.isDone() || !client.getSoundManager().isPlaying(activeResonanceSound)) {
                activeResonanceSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_PORTAL_AMBIENT, player, 0.38f, 0.45f);
                client.getSoundManager().play(activeResonanceSound);
            }

            stingerCooldown--;
            if (stingerCooldown <= 0) {
                stingerCooldown = 60 + rand.nextInt(70);
                if (rand.nextBoolean()) {
                    player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.60f);
                } else {
                    player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f + rand.nextFloat() * 0.4f);
                }
            }
        } else if (insideAnyTcmp) {
            // TCMP Ambient Audio: Eerie low-pitch drone & optical lens resonance
            if (activeDroneSound == null || activeDroneSound.isDone() || !client.getSoundManager().isPlaying(activeDroneSound)) {
                activeDroneSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_BEACON_AMBIENT, player, 0.75f, 0.50f);
                client.getSoundManager().play(activeDroneSound);
            }
            if (activeResonanceSound == null || activeResonanceSound.isDone() || !client.getSoundManager().isPlaying(activeResonanceSound)) {
                activeResonanceSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT, player, 0.40f, 0.75f);
                client.getSoundManager().play(activeResonanceSound);
            }

            stingerCooldown--;
            if (stingerCooldown <= 0) {
                stingerCooldown = 50 + rand.nextInt(50);
                if (rand.nextBoolean()) {
                    player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.70f);
                } else {
                    player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.55f + rand.nextFloat() * 0.3f);
                }
            }
        }
    }
}
