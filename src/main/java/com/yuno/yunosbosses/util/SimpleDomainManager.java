package com.yuno.yunosbosses.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class SimpleDomainManager {
    public static class SimpleDomainInstance {
        private final UUID casterUuid;
        private final Vec3d center;
        private final float radius;
        private int remainingTicks;
        private float maxIntegrity;
        private float currentIntegrity;

        public SimpleDomainInstance(UUID casterUuid, Vec3d center, float radius, int durationTicks, float maxIntegrity) {
            this.casterUuid = casterUuid;
            this.center = center;
            this.radius = radius;
            this.remainingTicks = durationTicks;
            this.maxIntegrity = maxIntegrity;
            this.currentIntegrity = maxIntegrity;
        }

        public UUID getCasterUuid() { return casterUuid; }
        public Vec3d getCenter() { return center; }
        public float getRadius() { return radius; }
        public int getRemainingTicks() { return remainingTicks; }
        public float getCurrentIntegrity() { return currentIntegrity; }

        public boolean damageIntegrity(float amount) {
            this.currentIntegrity -= amount;
            return this.currentIntegrity <= 0.0F;
        }
    }

    private static final List<SimpleDomainInstance> ACTIVE_SIMPLE_DOMAINS = new ArrayList<>();
    private static final DustParticleEffect SIMPLE_DOMAIN_DUST = new DustParticleEffect(0x80D8FF, 0.9F); // Cyan/whitish barrier glow

    public static void activate(ServerWorld world, LivingEntity caster, Vec3d center, float radius, int durationTicks) {
        // Remove existing instance for this caster if any
        ACTIVE_SIMPLE_DOMAINS.removeIf(inst -> inst.casterUuid.equals(caster.getUuid()));

        SimpleDomainInstance instance = new SimpleDomainInstance(caster.getUuid(), center, radius, durationTicks, 140.0F);
        ACTIVE_SIMPLE_DOMAINS.add(instance);

        // Sound cues
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.8F, 1.35F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2F, 1.85F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 0.8F);

        // Burst perimeter particle ring
        int ringCount = 36;
        for (int i = 0; i < ringCount; i++) {
            double angle = (2.0 * Math.PI * i) / ringCount;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD, px, center.y + 0.15, pz, 1, 0, 0.05, 0, 0.02);
            world.spawnParticles(SIMPLE_DOMAIN_DUST, px, center.y + 0.1, pz, 2, 0.05, 0.05, 0.05, 0.0);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, center.y + 0.1, pz, 1, 0.02, 0.05, 0.02, 0.03);
        }
    }

    public static boolean isProtected(LivingEntity entity) {
        if (entity == null || entity.getWorld().isClient) return false;
        Vec3d entityPos = entity.getPos();

        for (SimpleDomainInstance inst : ACTIVE_SIMPLE_DOMAINS) {
            if (entityPos.distanceTo(inst.center) <= inst.radius) {
                return true;
            }
        }
        return false;
    }

    public static void onSureHitBlocked(ServerWorld world, LivingEntity entity, float incomingDamage) {
        Vec3d pos = entity.getEyePos().subtract(0, 0.4, 0);

        // Shield deflection audio & sparks
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.85F, 1.4F + world.random.nextFloat() * 0.2F);
        world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 4, 0.2, 0.2, 0.2, 0.1);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 3, 0.15, 0.15, 0.15, 0.05);

        // Slightly erode integrity of the protecting simple domain
        for (Iterator<SimpleDomainInstance> it = ACTIVE_SIMPLE_DOMAINS.iterator(); it.hasNext(); ) {
            SimpleDomainInstance inst = it.next();
            if (entity.getPos().distanceTo(inst.center) <= inst.radius) {
                // Each sure-hit tick erodes a small amount of integrity
                boolean shattered = inst.damageIntegrity(incomingDamage * 0.35F);
                if (shattered) {
                    it.remove();
                    Entity caster = world.getEntity(inst.casterUuid);
                    if (caster instanceof PlayerEntity player) {
                        player.sendMessage(Text.literal("§c[Simple Domain] Shattered under the domain's overwhelming output!§r"), true);
                    }
                    world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                            SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0F, 1.1F);
                    world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 1.8F, 1.4F);

                    int count = 28;
                    for (int i = 0; i < count; i++) {
                        double angle = (2.0 * Math.PI * i) / count;
                        double px = inst.center.x + Math.cos(angle) * inst.radius;
                        double pz = inst.center.z + Math.sin(angle) * inst.radius;
                        world.spawnParticles(ParticleTypes.CRIT, px, inst.center.y + 0.3, pz, 3, 0.2, 0.2, 0.2, 0.1);
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, inst.center.y + 0.3, pz, 4, 0.2, 0.2, 0.2, 0.1);
                    }
                    break;
                }
            }
        }
    }

    public static void tick(ServerWorld world) {
        if (ACTIVE_SIMPLE_DOMAINS.isEmpty()) return;

        Iterator<SimpleDomainInstance> it = ACTIVE_SIMPLE_DOMAINS.iterator();
        while (it.hasNext()) {
            SimpleDomainInstance inst = it.next();
            Entity caster = world.getEntity(inst.casterUuid);

            // 1. Caster died or disappeared
            if (caster == null || !caster.isAlive()) {
                it.remove();
                continue;
            }

            // 2. Stepped outside simple domain circle stance
            if (caster.getPos().distanceTo(inst.center) > inst.radius + 1.2) {
                if (caster instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("§cSimple Domain dissolved! You stepped outside the stance.§r"), true);
                }
                world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                it.remove();
                continue;
            }

            // 3. Render circular boundary particles
            int points = 24;
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points + (world.getTime() * 0.05);
                double px = inst.center.x + Math.cos(angle) * inst.radius;
                double pz = inst.center.z + Math.sin(angle) * inst.radius;

                world.spawnParticles(SIMPLE_DOMAIN_DUST, px, inst.center.y + 0.08, pz, 1, 0, 0.02, 0, 0.0);
                if (i % 3 == 0) {
                    world.spawnParticles(ParticleTypes.END_ROD, px, inst.center.y + 0.1, pz, 1, 0, 0.03, 0, 0.01);
                }
            }

            // Center floor runes
            if (world.getTime() % 4 == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, inst.center.x, inst.center.y + 0.2, inst.center.z, 3, 0.6, 0.1, 0.6, 0.05);
            }

            // 4. Tick remaining time
            inst.remainingTicks--;
            if (inst.remainingTicks <= 0) {
                if (caster instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("§7[Simple Domain] Expired.§r"), true);
                }
                world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                it.remove();
            }
        }
    }
}
