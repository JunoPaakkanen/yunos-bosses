package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.network.SimpleDomainPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleDomainManager {
    public static class SimpleDomainInstance {
        private final UUID casterUuid;
        private final RegistryKey<World> worldKey;
        private final Vec3d center;
        private final float radius;
        private final int maxTicks;
        private int remainingTicks;
        private final float maxIntegrity;
        private float currentIntegrity;

        public SimpleDomainInstance(UUID casterUuid, RegistryKey<World> worldKey, Vec3d center, float radius, int durationTicks, float maxIntegrity) {
            this.casterUuid = casterUuid;
            this.worldKey = worldKey;
            this.center = center;
            this.radius = radius;
            this.maxTicks = durationTicks;
            this.remainingTicks = durationTicks;
            this.maxIntegrity = maxIntegrity;
            this.currentIntegrity = maxIntegrity;
        }

        public UUID getCasterUuid() { return casterUuid; }
        public RegistryKey<World> getWorldKey() { return worldKey; }
        public Vec3d getCenter() { return center; }
        public float getRadius() { return radius; }
        public int getMaxTicks() { return maxTicks; }
        public int getRemainingTicks() { return remainingTicks; }
        public float getCurrentIntegrity() { return currentIntegrity; }

        public boolean damageIntegrity(float amount) {
            this.currentIntegrity -= amount;
            return this.currentIntegrity <= 0.0F;
        }
    }

    public static class ClientSimpleDomain {
        private final UUID casterUuid;
        private final Vec3d center;
        private final float radius;
        private final int maxTicks;
        private int currentTicks;

        public ClientSimpleDomain(UUID casterUuid, Vec3d center, float radius, int maxTicks) {
            this.casterUuid = casterUuid;
            this.center = center;
            this.radius = radius;
            this.maxTicks = maxTicks;
            this.currentTicks = 0;
        }

        public void tick() {
            this.currentTicks++;
        }

        public boolean isExpired() {
            return this.currentTicks >= this.maxTicks;
        }

        public UUID getCasterUuid() { return casterUuid; }
        public Vec3d getCenter() { return center; }
        public float getRadius() { return radius; }
        public int getMaxTicks() { return maxTicks; }
        public int getCurrentTicks() { return currentTicks; }
    }

    private static final List<SimpleDomainInstance> ACTIVE_SIMPLE_DOMAINS = new ArrayList<>();
    public static final List<ClientSimpleDomain> ACTIVE_SIMPLE_DOMAINS_CLIENT = new CopyOnWriteArrayList<>();
    private static final DustParticleEffect SIMPLE_DOMAIN_DUST = new DustParticleEffect(0x80D8FF, 0.9F); // Cyan/whitish barrier glow

    public static void addClientSimpleDomain(UUID casterUuid, Vec3d center, float radius, int durationTicks) {
        ACTIVE_SIMPLE_DOMAINS_CLIENT.removeIf(inst -> inst.getCasterUuid().equals(casterUuid));
        ACTIVE_SIMPLE_DOMAINS_CLIENT.add(new ClientSimpleDomain(casterUuid, center, radius, durationTicks));
    }

    public static void removeClientSimpleDomain(UUID casterUuid) {
        if (casterUuid == null) return;
        ACTIVE_SIMPLE_DOMAINS_CLIENT.removeIf(inst -> inst.getCasterUuid().equals(casterUuid));
    }

    public static void tickClient() {
        for (int i = ACTIVE_SIMPLE_DOMAINS_CLIENT.size() - 1; i >= 0; i--) {
            if (i >= ACTIVE_SIMPLE_DOMAINS_CLIENT.size()) continue;
            ClientSimpleDomain inst = ACTIVE_SIMPLE_DOMAINS_CLIENT.get(i);
            inst.tick();
            if (inst.isExpired()) {
                ACTIVE_SIMPLE_DOMAINS_CLIENT.remove(inst);
            }
        }
    }

    private static void sendSyncPacket(ServerWorld world, UUID casterUuid, Vec3d center, float radius, int durationTicks, boolean active) {
        SimpleDomainPayload payload = new SimpleDomainPayload(casterUuid, center, radius, durationTicks, active);
        for (ServerPlayerEntity player : PlayerLookup.world(world)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void activate(ServerWorld world, LivingEntity caster, Vec3d center, float radius, int durationTicks) {
        // Remove existing instance for this caster if any
        ACTIVE_SIMPLE_DOMAINS.removeIf(inst -> inst.casterUuid.equals(caster.getUuid()));

        SimpleDomainInstance instance = new SimpleDomainInstance(
                caster.getUuid(), world.getRegistryKey(), center, radius, durationTicks, 140.0F);
        ACTIVE_SIMPLE_DOMAINS.add(instance);

        // Sync active simple domain to clients in this world for persistent edge circle rendering
        sendSyncPacket(world, caster.getUuid(), center, radius, durationTicks, true);

        // Sound cues
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.8F, 1.35F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2F, 1.85F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 0.8F);

        // Burst perimeter particle ring on activation
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
        RegistryKey<World> entityWorldKey = entity.getWorld().getRegistryKey();

        for (SimpleDomainInstance inst : ACTIVE_SIMPLE_DOMAINS) {
            if (inst.worldKey.equals(entityWorldKey) && entityPos.distanceTo(inst.center) <= inst.radius) {
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
            if (inst.worldKey.equals(world.getRegistryKey()) && entity.getPos().distanceTo(inst.center) <= inst.radius) {
                // Each sure-hit tick erodes a small amount of integrity
                boolean shattered = inst.damageIntegrity(incomingDamage * 0.35F);
                if (shattered) {
                    it.remove();
                    sendSyncPacket(world, inst.casterUuid, inst.center, inst.radius, 0, false);

                    Entity caster = world.getEntity(inst.casterUuid);
                    if (caster instanceof PlayerEntity player) {
                        player.sendMessage(Text.literal("\u00a7c[Simple Domain] Shattered under the domain's overwhelming output!\u00a7r"), true);
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

        RegistryKey<World> currentWorldKey = world.getRegistryKey();
        Iterator<SimpleDomainInstance> it = ACTIVE_SIMPLE_DOMAINS.iterator();
        while (it.hasNext()) {
            SimpleDomainInstance inst = it.next();
            if (!inst.worldKey.equals(currentWorldKey)) {
                continue;
            }

            Entity caster = world.getEntity(inst.casterUuid);

            // 1. Caster died or disappeared
            if (caster == null || !caster.isAlive()) {
                it.remove();
                sendSyncPacket(world, inst.casterUuid, inst.center, inst.radius, 0, false);
                continue;
            }

            // 2. Stepped outside simple domain circle stance
            if (caster.getPos().distanceTo(inst.center) > inst.radius + 1.2) {
                if (caster instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("\u00a7cSimple Domain dissolved! You stepped outside the stance.\u00a7r"), true);
                }
                world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                it.remove();
                sendSyncPacket(world, inst.casterUuid, inst.center, inst.radius, 0, false);
                continue;
            }

            // 3. Subtle center floor runes while active (outer edge is rendered as a spinning blue barrier circle)
            if (world.getTime() % 6 == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, inst.center.x, inst.center.y + 0.2, inst.center.z, 2, 0.6, 0.1, 0.6, 0.05);
            }

            // 4. Tick remaining time
            inst.remainingTicks--;
            if (inst.remainingTicks <= 0) {
                if (caster instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("\u00a77[Simple Domain] Expired.\u00a7r"), true);
                }
                world.playSound(null, inst.center.x, inst.center.y, inst.center.z,
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                it.remove();
                sendSyncPacket(world, inst.casterUuid, inst.center, inst.radius, 0, false);
            }
        }
    }
}
