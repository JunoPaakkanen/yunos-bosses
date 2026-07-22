package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

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

        DelayedServerEffects.delay(80, () -> finishDomainExpansionCast(world, caster, staff, 20.0F));
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        float potency = switch (chargeLevel) {
            case 2 -> 1.5F; // Charge level 2 = 150% potency
            case 3 -> 3.0F; // Charge level 3 = 300% potency
            default -> 1.0F; // Charge level 1 (Instant) = Base potency
        };

        // --- VOICE LINE ---
        caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.DOMAIN_EXPANSION_SHRINE_2, SoundCategory.NEUTRAL, 1.2f, 1.0f);

        // Apply a brief darkness effect to players
        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 110, 1));
        }

        // Play cast animation
        startDomainExpansionCast(world, caster, "Malevolent Shrine");

        // Calculate dynamic radius
        float dynamicRadius = getRadius(chargeLevel);

        DelayedServerEffects.delay(80, () -> finishDomainExpansionCast(world, caster, staff, dynamicRadius));
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.domain_expansion_shrine");
    }

    @Override
    public boolean canBeCharged() {
        return true;
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
    public void onDomainCreated(ServerWorld world, LivingEntity caster, Vec3d barrierCenter) {
        // Calculate Shrine spawn coordinates
        // Spawn Shrine 1.5 blocks behind the caster
        Vec3d forwardVec = Vec3d.fromPolar(0.0F, caster.getYaw()).normalize();
        double spawnX = caster.getX() - (forwardVec.x * 1.5);
        double spawnY = caster.getY();
        double spawnZ = caster.getZ() - (forwardVec.z * 1.5);

        BlockPos shrineCenter = BlockPos.ofFloored(spawnX, spawnY, spawnZ);

        // Instantly clear the area to make room for the Shrine entity
        int clearRadius = 4;  // 4 blocks in each direction creates a 9x9 horizontal clearing
        int clearHeight = 10; // 10 blocks tall

        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int y = 0; y < clearHeight; y++) {
                for (int z = -clearRadius; z <= clearRadius; z++) {

                    // Check if the current coordinate falls inside the cylinder shape
                    if ((x * x) + (z * z) <= (clearRadius * clearRadius)) {
                        BlockPos targetPos = shrineCenter.add(x, y, z);
                        BlockState state = world.getBlockState(targetPos);

                        // Delete the block instantly if it is breakable
                        if (!state.isAir() && state.getHardness(world, targetPos) >= 0.0F) {
                            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }

        // Spawn Shrine entity
        DomainShrineEntity shrine = new DomainShrineEntity(ModEntities.DOMAIN_SHRINE, world);

        shrine.refreshPositionAndAngles(spawnX, spawnY, spawnZ, caster.getYaw(), 0.0F);
        world.spawnEntity(shrine);

        // Levitate the player slightly
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 3));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 140, 1));
    }

    @Override
    public void onDomainRemoved(ServerWorld world, ActiveBarrier barrier) {
        // Create a search area covering the entire domain
        Box searchBox = Box.from(barrier.getPosition()).expand(barrier.getRadius());

        // Find any Shrine Entities inside that box
        List<DomainShrineEntity> shrines = world.getEntitiesByClass(
                DomainShrineEntity.class,
                searchBox,
                entity -> true
        );

        // Delete them
        for (DomainShrineEntity shrine : shrines) {
            shrine.discard();
        }
    }

    @Override
    public void onGlobalTick(World world, ActiveBarrier barrier) {
        if (world.isClient) return;

        // --- SPAWN SLASHES ---
        Vec3d center = barrier.getPosition();
        float radius = barrier.getRadius();
        Random rand = world.getRandom();
        double floorY = center.getY() - 3.0; // Reference point of our custom floor layer

        // Slash particle pool
        SimpleParticleType[] particlePool = {
                ModParticles.DISMANTLE_A_PARTICLE,
                ModParticles.DISMANTLE_B_PARTICLE
        };

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

            // 2. Validate position
            if (spawnPos.getY() >= floorY + 0.5) {

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

        // -- RANDOMLY DESTROY BLOCKS --
        int blocksToBreakPerTick = 250;

        for (int i = 0; i < blocksToBreakPerTick; i++) {
            // Generate random spherical coordinates
            double u = rand.nextDouble();
            double v = rand.nextDouble();
            double w = rand.nextDouble();

            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);

            // Math.cbrt ensures destruction is spread evenly to the very edges
            double r = radius * Math.cbrt(w);

            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.sin(phi) * Math.sin(theta);
            double dz = r * Math.cos(phi);

            // Convert to a block grid position
            BlockPos targetPos = BlockPos.ofFloored(center.x + dx, center.y + dy, center.z + dz);

            // Determine level of destruction based on Domain radius

            // Charge Level 1 (radius < 30): 0 to 0 (1x1x1 single block)
            int minX = (radius >= 50.0F) ? -1 : 0;
            int maxX = (radius >= 30.0F) ? 1 : 0;
            // Charge Level 2 (30 <= radius < 50): 0 to 1 (2x2x2 cube)
            int minY = (radius >= 50.0F) ? -1 : 0;
            int maxY = (radius >= 30.0F) ? 1 : 0;
            // Charge Level 3 (radius >= 50): -1 to 1 (3x3x3 cube)
            int minZ = (radius >= 50.0F) ? -1 : 0;
            int maxZ = (radius >= 30.0F) ? 1 : 0;


            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos currentPos = targetPos.add(x, y, z);

                        // Do not break the custom domain floor or anything beneath it
                        if (currentPos.getY() <= floorY) continue;

                        BlockState state = world.getBlockState(currentPos);

                        // Only attempt to break if it's an actual block (not air or water)
                        if (!state.isAir() && state.getFluidState().isEmpty()) {
                            // Check if the block is breakable
                            if (state.getHardness(world, currentPos) >= 0.0F) {
                                // Break the block!
                                //world.breakBlock(targetPos, false);
                                world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);

                                // Spawn a random Slash particle on the broken block from the particle pool
                                int randomIndex = world.getRandom().nextInt(particlePool.length);
                                SimpleParticleType particleType = particlePool[randomIndex];

                                // Spawn the particle
                                ((ServerWorld) world).spawnParticles(
                                        particleType,
                                        currentPos.getX() + 0.5, currentPos.getY(), currentPos.getZ(),
                                        1,
                                        0.3, 0.3, 0.3,
                                        0.05
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public float manaCost = 100.0F;

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}

}
