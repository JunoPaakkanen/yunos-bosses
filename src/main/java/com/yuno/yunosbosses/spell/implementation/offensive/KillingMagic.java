package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;

public class KillingMagic extends Spell {

    public KillingMagic(Identifier id) {
        super(id);
    }

    @Override
    public void cast(World world, PlayerEntity player, ItemStack staff) {
        if (!world.isClient) {
            // Spell implementation
            Vec3d playerLookVector = player.getRotationVector();
            Vec3d start = player.getEyePos().add(playerLookVector.multiply(1.0));

            int maxRange = 20;
            int delay = 20;
            float stepDistance = 1.0F;
            float damageRadius = 0.9F;
            float baseDamage = 30.0F;
            float cooldown = 15.0F;
            
            fireBeam(world, player, start, maxRange, delay, stepDistance, damageRadius, baseDamage, false);
        }
    }

    protected void fireBeam(World world, PlayerEntity player, Vec3d start, int maxRange, int delay, float stepDistance,
                            float damageRadius, float baseDamage, boolean useCustomStart) {
        Vec3d playerLookVector = player.getRotationVector();

        // Pass the player UUID and start position to the renderer.
        ServerPlayNetworking.send((ServerPlayerEntity) player, new BeamPayload(player.getUuid(), start, maxRange, useCustomStart, null));

        // Create a runnable to be executed later
        DelayedServerEffects.delay(delay, () -> {

            HashSet<Entity> hitEntities = new HashSet<>();
            Vec3d playerLookVectorFinal = player.getRotationVector(); // Final player look vector for when the beam is fired
            Vec3d firingOrigin;
            if (useCustomStart) { firingOrigin = start; }
            else { firingOrigin = player.getEyePos().add(playerLookVectorFinal.multiply(1.0)); }
            Vec3d currentPoint = firingOrigin;

            for (int i = 0; i < maxRange; i++) {
                currentPoint = currentPoint.add(playerLookVectorFinal.multiply(stepDistance));
                    
                    // Create a properly centered box around the current point
                    Box attackHitbox = new Box(
                        currentPoint.x - damageRadius, currentPoint.y - damageRadius, currentPoint.z - damageRadius,
                        currentPoint.x + damageRadius, currentPoint.y + damageRadius, currentPoint.z + damageRadius
                    );

                    // Damage entities
                    world.getOtherEntities(player, attackHitbox, Entity::isAlive).forEach(entity -> {
                    if (hitEntities.contains(entity)) return;
                    if (entity instanceof ItemEntity) return;
                    entity.damage(world.getDamageSources().indirectMagic(player, player), baseDamage);
                    hitEntities.add(entity);
                });

                BlockPos minPos = BlockPos.ofFloored(attackHitbox.minX, attackHitbox.minY, attackHitbox.minZ);
                BlockPos maxPos = BlockPos.ofFloored(attackHitbox.maxX, attackHitbox.maxY, attackHitbox.maxZ);

                // Iterate over each block inside the attackHitbox and destroy them
                for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
                    BlockState state = world.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getHardness(world, pos) < 0.0F) {
                        continue;
                    }
                    if (!state.getFluidState().isEmpty()) {
                        continue;
                    }
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
            Vec3d dir = currentPoint.subtract(start);
            Vec3d unitDir = playerLookVectorFinal;

            if (world instanceof ServerWorld serverWorld) {
                // Spawn particles
                // Energy trail
                for (double d = 0; d < maxRange; d += 0.3) {
                    Vec3d pos = firingOrigin.add(unitDir.multiply(d));

                    // Core beam particles
                    serverWorld.spawnParticles(
                            ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.02);

                    // Occasional blue sparks
                    if (serverWorld.random.nextFloat() > 0.6f) {
                        serverWorld.spawnParticles(
                                ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        });
    }

    protected void fireBeamTowardTarget(World world, PlayerEntity player, Vec3d start, Vec3d direction, 
                                       int maxRange, int delay, float stepDistance, float damageRadius, float baseDamage) {
        // Pass the player UUID and start position to the renderer.
        ServerPlayNetworking.send((ServerPlayerEntity) player, new BeamPayload(player.getUuid(), start, maxRange, true, direction));

        // Create a runnable to be executed later
        DelayedServerEffects.delay(delay, () -> {

            HashSet<Entity> hitEntities = new HashSet<>();
            Vec3d firingOrigin = start;
            Vec3d firingDirection = direction; // Use the custom direction
            Vec3d currentPoint = firingOrigin;

            for (int i = 0; i < maxRange; i++) {
                currentPoint = currentPoint.add(firingDirection.multiply(stepDistance));
                
                // Create a properly centered box around the current point
                Box attackHitbox = new Box(
                    currentPoint.x - damageRadius, currentPoint.y - damageRadius, currentPoint.z - damageRadius,
                    currentPoint.x + damageRadius, currentPoint.y + damageRadius, currentPoint.z + damageRadius
                );

                // Damage entities
                world.getOtherEntities(player, attackHitbox, Entity::isAlive).forEach(entity -> {
                    if (hitEntities.contains(entity)) return;
                    if (entity instanceof ItemEntity) return;
                    entity.damage(world.getDamageSources().indirectMagic(player, player), baseDamage);
                    hitEntities.add(entity);
                });

                BlockPos minPos = BlockPos.ofFloored(attackHitbox.minX, attackHitbox.minY, attackHitbox.minZ);
                BlockPos maxPos = BlockPos.ofFloored(attackHitbox.maxX, attackHitbox.maxY, attackHitbox.maxZ);

                // Iterate over each block inside the attackHitbox and destroy them
                for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
                    BlockState state = world.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getHardness(world, pos) < 0.0F) {
                        continue;
                    }
                    if (!state.getFluidState().isEmpty()) {
                        continue;
                    }
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }

            if (world instanceof ServerWorld serverWorld) {
                // Spawn particles
                for (double d = 0; d < maxRange; d += 0.3) {
                    Vec3d pos = firingOrigin.add(firingDirection.multiply(d));

                    // Core beam particles
                    serverWorld.spawnParticles(
                            ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.02);

                    // Occasional blue sparks
                    if (serverWorld.random.nextFloat() > 0.6f) {
                        serverWorld.spawnParticles(
                                ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        });
    }
}
