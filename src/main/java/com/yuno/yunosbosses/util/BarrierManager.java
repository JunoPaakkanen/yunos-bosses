package com.yuno.yunosbosses.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
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
    public static void addBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks, Identifier texture, boolean isClient) {
        if (isClient) {
            ACTIVE_BARRIERS_CLIENT.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks, texture, null));
        } else {
            ACTIVE_BARRIERS.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks, texture, (entity, barrier) -> {}));
        }
    }

    // Legacy method for adding a barrier with the default texture
    public static void addBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks, boolean isClient) {
        Identifier hexTexture = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");
        addBarrier(ownerUuid, position, direction, maxTicks, hexTexture, isClient);
    }

    public static void tick(World world) {

        List<ActiveBarrier> listToTick = world.isClient ? ACTIVE_BARRIERS_CLIENT : ACTIVE_BARRIERS;

        for (int i = listToTick.size() - 1; i >= 0; i--) {
            ActiveBarrier barrier = listToTick.get(i);
            if (world.isClient || (world instanceof ServerWorld sw && sw.getRegistryKey() == World.OVERWORLD)) {
                barrier.tick();
            }

            // Blocking, Physics and Domain logic (Server-side)
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                if (barrier.getDirection().equals(Vec3d.ZERO)) {
                    double radius = 20.0;
                    // Apply the spherical pushing physics
                    SpherePhysics.apply(world, barrier, radius);

                    // Apply domain effect on entities inside
                    Box domainBox = new Box(barrier.getPosition().subtract(radius, radius, radius), barrier.getPosition().add(radius, radius, radius));
                    serverWorld.getOtherEntities(null, domainBox).forEach(entity -> {
                        if (!entity.isSpectator() && !entity.getUuid().equals(barrier.getOwnerUuid())) {
                            if (entity.getPos().distanceTo(barrier.getPosition()) < radius) {
                                barrier.getDomainEffect().accept(entity, barrier);
                            }
                        }
                    });

                    // Apply the domain expansion logic each tick (if applicable)
                    if (barrier.getDomainExpansion() != null) {
                        barrier.getDomainExpansion().onGlobalTick(world, barrier);
                    }

                    // Projectile logic for SPHERE
                    Box sphereBox = new Box(barrier.getPosition().subtract(radius, radius, radius),
                            barrier.getPosition().add(radius, radius, radius));
                    handleProjectiles(world, barrier, sphereBox, radius, true);
                }
                // --- 2. HEX SHIELD LOGIC ---
                else {
                    // Keep the small box for the directional shield
                    Box hexBox = Box.from(barrier.getPosition()).expand(0.8F);
                    handleProjectiles(world, barrier, hexBox, 0.8, false);
                }
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

    public static void handleProjectiles(World world, ActiveBarrier barrier, Box shieldBox, double radius, boolean isSphere) {

        world.getEntitiesByClass(ProjectileEntity.class, shieldBox, p -> true).forEach(projectile -> {
            Entity owner = projectile.getOwner();
            if (owner != null && owner.getUuid().equals(barrier.getOwnerUuid())) {
                return;
            }

            // Distance check
            if (isSphere) {
                double dist = projectile.getPos().distanceTo(barrier.getPosition());
                // Only reflect if it's hitting the "skin" (radius +/- 0.5 blocks)
                if (dist > radius + 0.5 || dist < radius - 0.5) {
                    return;
                }
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
}
