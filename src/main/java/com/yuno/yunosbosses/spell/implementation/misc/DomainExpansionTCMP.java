package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.block.ModBlocks;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainTcmpEyeEntity;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.util.SimpleDomainManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

/**
 * Naoya Zenin's Domain Expansion: Time Cell Moon Palace.
 * Encloses the targets in a closed opaque black barrier where Projection Sorcery
 * is forced onto every single cell.
 * Moving while subjected to the sure-hit tears cellular tissue apart, dealing damage
 * that scales directly with the target's movement velocity. Standing still causes baseline
 * cellular degradation unless protected by Simple Domain.
 * Cumulative damage per individual is capped at 240. If any target reaches this cap,
 * the domain instantly shatters and is destroyed.
 */
public class DomainExpansionTCMP extends DomainExpansion {

    public static final Identifier TCMP_TEXTURE = Identifier.of("yunosbosses", "textures/domain_expansion/tcmp.png");
    private static final DustParticleEffect LUNAR_PALE_DUST = new DustParticleEffect(0xE0F7FA, 0.85f);
    private static final DustParticleEffect CELL_TEAR_DUST = new DustParticleEffect(0xB71C1C, 0.95f);

    public DomainExpansionTCMP(Identifier id, SpellRarity rarity) {
        super(id, ModAnimations.DOMAIN_EXPANSION_SHRINE_ANIM, rarity);
    }

    @Override
    public Identifier getBarrierTexture() {
        return TCMP_TEXTURE;
    }

    @Override
    public Identifier getClosedBarrierTexture() {
        return TCMP_TEXTURE;
    }

    @Override
    public boolean isOpenBarrier() {
        return false; // Time Cell Moon Palace is fundamentally a closed domain
    }

    @Override
    public Block getDomainFloorBlock() {
        return ModBlocks.DOMAIN_FLOOR_VOID;
    }

    @Override
    public boolean shouldClearBlocksInside() {
        return true; // Instantly hollow out/destroy interior blocks
    }

    @Override
    public int getLifetimeTicks() {
        return 960; // 48 seconds
    }

    @Override
    public int getLifetimeTicks(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> 520; // 26 seconds
            case 3 -> 800; // 40 seconds
            default -> 320; // 16 seconds
        };
    }

    @Override
    public float getRadius() {
        return 20.0F;
    }

    public float getRadius(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> 26.0F;
            case 3 -> 36.0F;
            default -> 18.0F;
        };
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        this.cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        // 1. Technique Burnout Check
        if (DomainClashManager.hasBurnout(caster.getUuid())) {
            int remainingSec = DomainClashManager.getBurnoutSeconds(caster.getUuid());
            if (caster instanceof PlayerEntity player) {
                player.sendMessage(Text.literal("§cCannot expand domain! Technique Burnout (" + remainingSec + "s remaining)§r"), true);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 0.8f);
            }
            return;
        }

        // 2. Play Domain Expansion Voiceline / Chant
        caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.DOMAIN_EXPANSION_TCMP, SoundCategory.PLAYERS, 2.5f, 1.0f);

        // 3. Cinematic visual cue & darkness
        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 90, 1));
        }

        // 4. Trigger domain cutscene & player animation with 80-tick (4.0s) cast duration
        startDomainExpansionCast(world, caster, "Time Cell Moon Palace", false, 80);

        float dynamicRadius = getRadius(chargeLevel);

        // 5. Register with Domain Clash Manager for 80-tick cast window
        DomainClashManager.registerDomainCast(
                (ServerWorld) world,
                caster,
                this,
                staff,
                dynamicRadius,
                false,
                "Time Cell Moon Palace",
                chargeLevel,
                80,
                () -> finishDomainExpansionCast(world, caster, staff, dynamicRadius, false, chargeLevel)
        );
    }

    @Override
    public void onDomainCreated(ServerWorld world, LivingEntity caster, Vec3d barrierCenter) {
        // Spawn the centerpiece eye in the air behind the caster (2 blocks behind, slightly above eye level)
        float casterYaw = caster.getYaw();
        Vec3d forward = Vec3d.fromPolar(0.0f, casterYaw).normalize();
        Vec3d eyePos = caster.getPos().subtract(forward.multiply(2.0));
        double eyeY = caster.getEyeY() - 0.9; // Center of eye hovers slightly above caster eye level

        DomainTcmpEyeEntity eye = new DomainTcmpEyeEntity(ModEntities.DOMAIN_TCMP_EYE, world);
        eye.refreshPositionAndAngles(eyePos.x, eyeY, eyePos.z, casterYaw, 0.0F);
        eye.setOwner(caster);
        world.spawnEntity(eye);

        // Celestial awakening burst
        for (int deg = 0; deg < 360; deg += 12) {
            double rad = Math.toRadians(deg);
            double px = eyePos.x + Math.cos(rad) * 2.5;
            double pz = eyePos.z + Math.sin(rad) * 2.5;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, eyeY + 1.25, pz, 2, 0.1, 0.05, 0.1, 0.05);
            world.spawnParticles(LUNAR_PALE_DUST, px, eyeY + 1.25, pz, 2, 0.1, 0.1, 0.1, 0.02);
        }

        // Caster empowerment inside Time Cell Moon Palace
        if (caster instanceof NaoyaEntity naoya) {
            naoya.setSpeedStacks(15); // Max speed stacks inside his own domain!
            naoya.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 960, 1, false, false, true));
            naoya.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 960, 1, false, false, true));
        }
    }

    @Override
    public void onDomainRemoved(ServerWorld world, ActiveBarrier barrier) {
        // Discard any active centerpiece eye entities
        Box searchBox = Box.from(barrier.getPosition()).expand(barrier.getRadius() + 10.0);
        List<DomainTcmpEyeEntity> eyes = world.getEntitiesByClass(
                DomainTcmpEyeEntity.class,
                searchBox,
                Entity::isAlive
        );
        for (DomainTcmpEyeEntity eye : eyes) {
            eye.discard();
        }
    }

    @Override
    public void onDomainEffect(Entity affectedEntity, ActiveBarrier barrier) {
        if (barrier.isExpired()) return;
        if (!(affectedEntity instanceof LivingEntity target)) return;

        // The caster is exempt from the sure-hit technique
        if (target.getUuid().equals(barrier.getOwnerUuid())) return;

        ServerWorld world = (ServerWorld) target.getWorld();
        Entity caster = world.getEntity(barrier.getOwnerUuid());

        // Measure velocity
        Vec3d vel = target.getVelocity();
        double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        double vertSpeed = target.isOnGround() ? 0.0 : Math.abs(vel.y);
        double totalSpeed = Math.sqrt(horizSpeed * horizSpeed + vertSpeed * vertSpeed);

        if (target.isSprinting() && totalSpeed < 0.25) {
            totalSpeed = Math.max(totalSpeed, 0.28);
        }

        // Sure-hit calculation:
        // Standing still: 1.0F per 4 ticks (5.0 raw DPS, 1.80 post-Prot4 DPS -> ~18-20s survival with Prot 4 + Golden Apples)
        // Moving / Panicking: scales with velocity:
        // Walking (~0.2 speed) -> 3.3F per 4 ticks (unchanged)
        // Sprinting (~0.28 speed) -> 5.2F per 4 ticks (unchanged)
        // High-speed (>0.35 speed, e.g., sprint-jumping, dashes, or being launched by Naoya's domain-boosted knockback):
        //   Applies a balanced upper-velocity bonus (+10% to +15% at top velocities, capped at +3.5F bonus)
        if (target.age % 4 == 0) {
            float rawDamage;
            if (totalSpeed < 0.04) {
                rawDamage = 1.0F;
            } else {
                rawDamage = (float) (1.5F + totalSpeed * 6.0F + (totalSpeed * totalSpeed * 15.0F));
                // Upper velocity tier bonus: only triggers above normal sprinting speed (> 0.35 blocks/tick)
                if (totalSpeed > 0.35) {
                    double excessSpeed = Math.min(totalSpeed - 0.35, 1.45);
                    float highVelocityBonus = (float) (excessSpeed * 1.6 + (excessSpeed * excessSpeed * 0.6));
                    rawDamage += Math.min(highVelocityBonus, 3.5F);
                }
                if (target.isSprinting()) {
                    rawDamage *= 1.2F;
                }
            }

            // Simple Domain protection check!
            if (SimpleDomainManager.isProtected(target)) {
                SimpleDomainManager.onSureHitBlocked(world, target, rawDamage);
                return;
            }

            Box searchBox = Box.from(barrier.getPosition()).expand(barrier.getRadius() + 10.0);
            List<DomainTcmpEyeEntity> eyes = world.getEntitiesByClass(
                    DomainTcmpEyeEntity.class,
                    searchBox,
                    e -> e.isAlive() && barrier.getOwnerUuid().equals(e.getOwnerUuid())
            );

            DomainTcmpEyeEntity eye = eyes.isEmpty() ? null : eyes.getFirst();
            float currentCumulative = eye != null ? eye.getDamageDealt(target.getUuid()) : 0.0f;

            // If the target has already reached the 240 cap, the domain cannot deal more damage and must shatter
            if (currentCumulative >= DomainTcmpEyeEntity.DAMAGE_CAP) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 2.0f, 1.2f);
                BarrierManager.destroyDomain(barrier, world);
                return;
            }

            // Strictly cap damage so cumulative damage to this individual cannot exceed 240.0
            float damage = Math.min(rawDamage, DomainTcmpEyeEntity.DAMAGE_CAP - currentCumulative);
            if (damage <= 0.0f) {
                BarrierManager.destroyDomain(barrier, world);
                return;
            }

            DamageSource source = ModDamageTypes.of(world, ModDamageTypes.DOMAIN_EXPANSION, caster);

            Vec3d prevVel = target.getVelocity();
            boolean wasSprinting = target.isSprinting();

            target.timeUntilRegen = 0;
            target.damage(world, source, damage);
            target.hurtTime = 0;

            target.setVelocity(prevVel);
            if (wasSprinting) {
                target.setSprinting(true);
            }

            float newTotal = currentCumulative + damage;
            if (eye != null) {
                eye.recordDomainDamage(target, damage);
            }

            // Flesh cutting sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 1.8f);

            // Cellular laceration particles
            double cx = target.getX();
            double cy = target.getY() + (target.getHeight() * 0.5);
            double cz = target.getZ();

            world.spawnParticles(CELL_TEAR_DUST, cx, cy, cz, 4, 0.2, 0.3, 0.2, 0.04);
            world.spawnParticles(ParticleTypes.CRIT, cx, cy, cz, 2, 0.15, 0.15, 0.15, 0.05);

            // If this individual hits the maximum cap of 240 cumulative damage, domain instantly shatters!
            if (newTotal >= DomainTcmpEyeEntity.DAMAGE_CAP) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 2.0f, 1.2f);
                world.spawnParticles(CELL_TEAR_DUST, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
                BarrierManager.destroyDomain(barrier, world);
            }
        }
    }

    @Override
    public void onGlobalTick(World world, ActiveBarrier barrier) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Vec3d center = barrier.getPosition();
        float radius = barrier.getRadius();
        Random rand = world.getRandom();

        // Periodic celestial pulses from the centerpiece eye (every 8 ticks)
        if (barrier.getCurrentTicks() % 8 == 0) {
            double angle = rand.nextDouble() * 2.0 * Math.PI;
            double dist = 2.0 + rand.nextDouble() * (radius * 0.85);
            double py = center.y - 1.5 + (rand.nextDouble() * 4.0);
            double px = center.x + Math.cos(angle) * dist;
            double pz = center.z + Math.sin(angle) * dist;

            serverWorld.spawnParticles(LUNAR_PALE_DUST, px, py, pz, 1, 0.02, 0.02, 0.02, 0.02);
            if (rand.nextBoolean()) {
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0.03, 0.03, 0.03, 0.05);
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.domain_expansion_tcmp");
    }

    @Override
    public boolean canBeCharged() {
        return true;
    }
}
