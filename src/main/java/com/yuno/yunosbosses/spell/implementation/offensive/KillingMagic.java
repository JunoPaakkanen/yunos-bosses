package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
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

// Problems:
// The players lookVector is locked when casting, so the beam shoots in that same direction even if the player turns around
// The player is not able to move the magic circle, it should follow the players eyes as well
// The beams final hit point is calculated at the moment of casting, if the player moves the visual hit point for the beam does not change

// I want the beam and the circle to decouple from the player once the firing happens, before that I want the circle to follow wherever the player is looking
// ...so that the beam can be aimed while charging

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

            // Pass the player UUID and start position to the renderer.
            ServerPlayNetworking.send((ServerPlayerEntity) player, new BeamPayload(player.getUuid(), start, maxRange));

            // Create a runnable to be executed later
            DelayedServerEffects.delay(delay, () -> {

                HashSet<Entity> hitEntities = new HashSet<>();
                Vec3d playerLookVectorFinal = player.getRotationVector(); // Final player look vector for when the beam is fired
                Vec3d firingOrigin = player.getEyePos().add(playerLookVectorFinal.multiply(1.0));
                Vec3d currentPoint = firingOrigin;


                for (int i = 0; i < maxRange; i++) {
                    currentPoint = currentPoint.add(playerLookVectorFinal.multiply(stepDistance));
                    Box attackHitbox = Box.from(currentPoint).expand(damageRadius);

                    world.getOtherEntities(player, attackHitbox, Entity::isAlive).forEach(entity -> {
                        if (hitEntities.contains(entity)) return;
                        entity.damage(world.getDamageSources().indirectMagic(player, player), baseDamage);
                        hitEntities.add(entity);
                    });

                    // Iterate over each block inside the attackHitbox and destroy them
                    for (BlockPos pos : BlockPos.iterate(
                            (int) attackHitbox.minX, (int) attackHitbox.minY, (int) attackHitbox.minZ,
                            (int) attackHitbox.maxX, (int) attackHitbox.maxY, (int) attackHitbox.maxZ)) {
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
    }
}
