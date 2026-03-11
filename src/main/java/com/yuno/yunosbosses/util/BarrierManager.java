package com.yuno.yunosbosses.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BarrierManager {
    // Lists for active barriers, separate for server and client
    public static final List<ActiveBarrier> ACTIVE_BARRIERS = new ArrayList<>(); // SERVER
    public static final List<ActiveBarrier> ACTIVE_BARRIERS_CLIENT = new ArrayList<>(); // CLIENT

    // Add barrier to ACTIVE_BARRIERS
    public static void addBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks, boolean isClient) {
        if (isClient) {
            ACTIVE_BARRIERS_CLIENT.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks));
        } else {
            ACTIVE_BARRIERS.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks));
        }
    }

    public static void tick(World world) {

        List<ActiveBarrier> listToTick = world.isClient ? ACTIVE_BARRIERS_CLIENT : ACTIVE_BARRIERS;

        for (int i = listToTick.size() - 1; i >= 0; i--) {
            ActiveBarrier barrier = listToTick.get(i);
            if (world.isClient || (world instanceof ServerWorld sw && sw.getRegistryKey() == World.OVERWORLD)) {
                barrier.tick();
            }

            // Blocking logic (Server-side)
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                Box shieldBox = Box.from(barrier.getPosition()).expand(0.8F);

                world.getEntitiesByClass(ProjectileEntity.class, shieldBox, p -> true).forEach(projectile -> {
                    Entity owner = projectile.getOwner();
                    if (owner != null && owner.getUuid().equals(barrier.getOwnerUuid())) {
                        return;
                    }
                    // Reflect projectile
                    Vec3d motion = projectile.getVelocity();
                    Vec3d reflectedMotion = new Vec3d(-motion.x, -motion.y + 0.2, -motion.z).multiply(2);
                    projectile.setVelocity(reflectedMotion);

                    // Update visual rotation
                    float yaw = (float) (Math.atan2(reflectedMotion.x, reflectedMotion.z) * (180 / Math.PI));
                    float pitch = (float) (Math.atan2(reflectedMotion.y, reflectedMotion.horizontalLength()) * (180 / Math.PI));
                    projectile.setYaw(yaw);
                    projectile.setPitch(pitch);

                    projectile.velocityModified = true;
                    projectile.setOwner(world.getPlayerByUuid(barrier.getOwnerUuid()));

                    world.playSound(null, barrier.getPosition().x, barrier.getPosition().y, barrier.getPosition().z,
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.5F);
                });
            }

            if (barrier.isExpired()) {
                long currentTime = System.currentTimeMillis();
                if (!world.isClient) {
                    System.out.println("[SERVER] Barrier Expired at: " + currentTime + " ms");
                    BlockPos blockPos = BlockPos.ofFloored(barrier.getPosition());
                    // Glass shatter effect, visual and sound
                    world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(Blocks.GLASS.getDefaultState()));
                } else {
                    System.out.println("[CLIENT] Barrier Removed at: " + currentTime + " ms");
                }
                listToTick.remove(i);
            }
        }
    }
}
