package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class DomainExpansionShrine extends DomainExpansion {

    public DomainExpansionShrine(Identifier id) {
        super(id, castAnimation);
    }

    public static final Identifier castAnimation = ModAnimations.DOMAIN_EXPANSION_SHRINE_ANIM;

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {

        // --- VOICE LINE ---
        caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.DOMAIN_EXPANSION_SHRINE_1, SoundCategory.NEUTRAL, 1.2f, 1.0f);

        // Apply a brief darkness effect to players
        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 110, 1));
        }

        // Play cast animation
        startDomainExpansionCast(world, caster, "Malevolent Shrine");

        DelayedServerEffects.delay(80, () -> finishDomainExpansionCast(world, caster, staff));
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.domain_expansion_shrine");
    }

    @Override
    public Identifier getBarrierTexture() {
        return Identifier.of("yunosbosses", "textures/domain_expansion/shrine.png");
    }

    @Override
    public int getLifetimeTicks() {
        return 960; // 48 seconds
    }

    @Override
    public float getRadius() {
        return 20;
    }

    @Override
    public void onDomainEffect(Entity affectedEntity, ActiveBarrier barrier) {
        if (affectedEntity instanceof LivingEntity) {
            // Damage the entity every 10 ticks
            if (affectedEntity.age % 10 == 0) {
                ServerWorld serverWorld = (ServerWorld) affectedEntity.getWorld();
                Entity caster = serverWorld.getEntity(barrier.getOwnerUuid());

                // Play sound
                affectedEntity.getWorld().playSound(null, affectedEntity.getX(), affectedEntity.getY(), affectedEntity.getZ(),
                        ModSounds.REELSEIDEN_HIT, SoundCategory.NEUTRAL, 1.0f, 1.5f + (affectedEntity.getRandom().nextFloat() * 0.2f - 0.1f));

                // Apply damage to the target
                DamageSource source = ModDamageTypes.of(affectedEntity.getWorld(), ModDamageTypes.CUTTING_MAGIC, caster);
                Vec3d originalVelocity = affectedEntity.getVelocity(); // Store original velocity
                float damage = 1.5f;

                affectedEntity.damage(source, damage);

                affectedEntity.setVelocity(originalVelocity); // Restore original velocity (Undoes knockback)
                affectedEntity.velocityModified = true;

                // Spawn particles on hit
                Vec3d pos = affectedEntity.getBoundingBox().getCenter();
                serverWorld.spawnParticles(
                        ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                        pos.x, pos.y, pos.z,
                        1,
                        0.1, 0.1, 0.1,
                        0.0
                );
            }
        }
    }

    @Override
    public void onGlobalTick(World world, ActiveBarrier barrier) {
        if (world.isClient) return;

        // --- SPAWN SLASHES ---
        Vec3d center = barrier.getPosition();
        float radius = this.getRadius();
        Random rand = world.getRandom();

        // --- RANDOM STATIONARY SLASH SPAWNER ---
        int spawnCount = 4;
        for (int i = 0; i < spawnCount; i++) {

            // 1. Generate uniform spherical random distributions
            double u = rand.nextDouble();
            double v = rand.nextDouble();

            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);

            // Choose a random distance from the center up to the maximum radius boundary
            double r = rand.nextDouble() * (radius - 1.0);

            // Convert spherical points into standard cartesian space (X, Y, Z offsets)
            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.sin(phi) * Math.sin(theta);
            double dz = r * Math.cos(phi);

            Vec3d spawnPos = center.add(dx, dy, dz);
            double floorY = center.getY() - 1.0; // Reference point of our custom floor layer

            // 2. Validate position
            if (spawnPos.getY() >= floorY + 0.5) {

                // Slash particle pool
                SimpleParticleType[] particlePool = {
                        ModParticles.DISMANTLE_A_PARTICLE,
                        ModParticles.DISMANTLE_B_PARTICLE
                };

                // Randomly select a particle from the pool
                int randomIndex = world.getRandom().nextInt(particlePool.length);
                SimpleParticleType particleType = particlePool[randomIndex];

                // Spawn the particle
                ((ServerWorld) world).spawnParticles(
                        particleType,
                        spawnPos.x, spawnPos.y, spawnPos.z,
                        1,
                        0.3, 0.3, 0.3,
                        0.05
                );
            }
        }
    }

    public float manaCost = 100.0F;

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}

}
