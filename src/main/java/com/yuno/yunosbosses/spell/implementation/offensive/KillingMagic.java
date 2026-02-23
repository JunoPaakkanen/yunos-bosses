package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.spell.Spell;
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

public class KillingMagic extends Spell {

    public KillingMagic(Identifier id) {
        super(id);
    }

    @Override
    public void cast(World world, PlayerEntity player, ItemStack staff) {
        if (!world.isClient) {
            // Spell implementation
            HashSet<Entity> hitEntities = new HashSet<>();
            Vec3d currentPoint = player.getEyePos();
            Vec3d playerLookVector = player.getRotationVector();

            int maxRange = 20;
            float stepDistance = 1.0F;
            float damageRadius = 0.8F;
            float baseDamage = 30.0F;
            float cooldown = 15.0F;

            for (int i = 0; i < maxRange; i++) {
                currentPoint = currentPoint.add(playerLookVector.multiply(stepDistance));
                Box attackHitbox = Box.from(currentPoint).expand(damageRadius);

                world.getOtherEntities(player, attackHitbox, Entity::isAlive).forEach(entity -> {
                    if (hitEntities.contains(entity)) return;
                    entity.damage(world.getDamageSources().indirectMagic(player, player), baseDamage);
                    hitEntities.add(entity);
                });

                // Iterate over each block inside the attackHitbox and destroy them
                for (BlockPos pos : BlockPos.iterate(
                        (int)attackHitbox.minX, (int)attackHitbox.minY, (int)attackHitbox.minZ,
                        (int)attackHitbox.maxX, (int)attackHitbox.maxY, (int)attackHitbox.maxZ)) {
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

            Vec3d start = player.getEyePos().add(player.getRotationVector().multiply(0.1)); // Calculate start position and offset by 0.1 blocks
            Vec3d dir = currentPoint.subtract(start);
            double distance = dir.length();
            Vec3d unitDir = dir.normalize();


            if (world instanceof ServerWorld serverWorld) {
                // Spawn particles
                // Sonic boom at the start position
                serverWorld.spawnParticles(
                        ParticleTypes.SONIC_BOOM, start.x, start.y, start.z, 1, 0 ,0, 0, 0);

                // Energy trail
                for (double d = 0; d < distance; d += 0.3) {
                    Vec3d pos = start.add(unitDir.multiply(d));

                    // Core beam particles
                    serverWorld.spawnParticles(
                            ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.1 ,0.1, 0.1, 0.02);

                    // Occasional blue sparks
                    if (serverWorld.random.nextFloat() > 0.6f) {
                        serverWorld.spawnParticles(
                                ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.2 ,0.2, 0.2, 0.1);
                    }
                }

            }

            // Pass the start position and final currentpoint to the renderer
            ServerPlayNetworking.send((ServerPlayerEntity) player, new BeamPayload(start, currentPoint));
        }
    }
}
