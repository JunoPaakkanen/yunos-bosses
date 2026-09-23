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
 * Specifically simulates the apocalyptic pulverized concrete storm,
 * volcanic ash flakes, cyclonic cursed debris vortex, and sub-bass ambient audio drone within Malevolent Shrine.
 */
public class DomainAtmosphereRenderer {
    private static final DustParticleEffect CONCRETE_DUST = new DustParticleEffect(0x9E9994, 0.75f);
    private static final DustParticleEffect CURSED_ASH = new DustParticleEffect(0x8C141C, 0.65f);

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

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
            // Check if this barrier is an active Malevolent Shrine domain
            if (!barrier.getDirection().equals(Vec3d.ZERO)) continue;
            boolean isShrine = barrier.getTexture() != null && barrier.getTexture().getPath().contains("shrine");
            if (!isShrine) continue;

            Vec3d center = barrier.getPosition();
            float radius = barrier.getRadius();
            double distToCenter = player.getPos().distanceTo(center);

            if (distToCenter <= radius + 3.0) {
                insideAnyShrine = true;
            }

            // Render atmospheric storm if the player is inside the domain or near the edge
            if (distToCenter > radius + 12.0) continue;

            // Density of atmospheric particles per client tick
            int particleCount = 30;
            double spawnRadius = Math.min(24.0, radius);

            for (int i = 0; i < particleCount; i++) {
                // Cylindrical spawn area around the player (above camera level to ground)
                double angle = rand.nextDouble() * 2.0 * Math.PI;
                double dist = rand.nextDouble() * spawnRadius;
                double px = player.getX() + Math.cos(angle) * dist;
                double pz = player.getZ() + Math.sin(angle) * dist;
                double py = player.getY() + 1.0 + (rand.nextDouble() * 12.0);

                // Ensure spawned particles stay inside the domain radius perimeter
                double dx = px - center.x;
                double dz = pz - center.z;
                double distSq = dx * dx + dz * dz;
                if (distSq > (radius * 1.05) * (radius * 1.05)) continue;

                // Cyclonic vortex calculations: wind swirls tangentially around Malevolent Shrine
                double dLen = Math.sqrt(distSq);
                double swirlX = 0.0;
                double swirlZ = 0.0;
                if (dLen > 0.5) {
                    // Tangential cyclonic swirl
                    swirlX = (-dz / dLen) * 0.045;
                    swirlZ = (dx / dLen) * 0.045;

                    // Outward blast expansion pressure
                    swirlX += (dx / dLen) * 0.012;
                    swirlZ += (dz / dLen) * 0.012;
                }

                // Downward fall velocity with gentle turbulence
                double vx = swirlX + (rand.nextDouble() - 0.5) * 0.025;
                double vy = -0.04 - (rand.nextDouble() * 0.05);
                double vz = swirlZ + (rand.nextDouble() - 0.5) * 0.025;

                int typeRoll = rand.nextInt(12);
                if (typeRoll < 5) {
                    // White ash
                    world.addParticleClient(ParticleTypes.WHITE_ASH, px, py, pz, vx, vy, vz);
                } else if (typeRoll < 8) {
                    // Charred volcanic cinder ash flakes
                    world.addParticleClient(ParticleTypes.ASH, px, py, pz, vx, vy, vz);
                } else if (typeRoll < 10) {
                    // Fine pulverized stone dust
                    world.addParticleClient(CONCRETE_DUST, px, py, pz, vx * 0.5, vy * 0.6, vz * 0.5);
                } else {
                    // Drifting crimson cursed ash
                    world.addParticleClient(CURSED_ASH, px, py, pz, vx * 0.5, vy * 0.6, vz * 0.5);
                }

                // Rare glowing ember caught in the vortex drafts
                if (rand.nextInt(15) == 0) {
                    world.addParticleClient(ModParticles.FLAME_EMBER_PARTICLE, px, py, pz, vx * 1.4, vy * 0.4, vz * 1.4);
                }
            }
        }

        // --- SUB-BASS CURSED AMBIENT AUDIO SYSTEM ---
        if (insideAnyShrine) {
            // Layer 1: Sub-Bass Low Frequency Drone (Deep humming vibration)
            if (activeDroneSound == null || activeDroneSound.isDone() || !client.getSoundManager().isPlaying(activeDroneSound)) {
                activeDroneSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_BEACON_AMBIENT, player, 0.85f, 0.42f);
                client.getSoundManager().play(activeDroneSound);
            }

            // Layer 2: Howling Cursed Wind Chasm
            if (activeWindSound == null || activeWindSound.isDone() || !client.getSoundManager().isPlaying(activeWindSound)) {
                activeWindSound = new ShrineAtmosphereSoundInstance(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(), player, 0.70f, 0.60f);
                client.getSoundManager().play(activeWindSound);
            }

            // Layer 3: Ominous Cursed Resonance Drone
            if (activeResonanceSound == null || activeResonanceSound.isDone() || !client.getSoundManager().isPlaying(activeResonanceSound)) {
                activeResonanceSound = new ShrineAtmosphereSoundInstance(SoundEvents.BLOCK_PORTAL_AMBIENT, player, 0.38f, 0.45f);
                client.getSoundManager().play(activeResonanceSound);
            }

            // Atmospheric Audio Stingers (blade sweeps and cursed pulses)
            stingerCooldown--;
            if (stingerCooldown <= 0) {
                stingerCooldown = 60 + rand.nextInt(70);
                if (rand.nextBoolean()) {
                    player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.60f);
                } else {
                    player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f + rand.nextFloat() * 0.4f);
                }
            }
        }
    }
}
