package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.entity.projectile.FlameArrowEntity;
import com.yuno.yunosbosses.network.RemoveBarrierPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansionShrine;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
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
import java.util.concurrent.CopyOnWriteArrayList;

public class BarrierManager {
    // Lists for active barriers, separate for server and client
    public static final List<ActiveBarrier> ACTIVE_BARRIERS = new CopyOnWriteArrayList<>(); // SERVER
    public static final List<ActiveBarrier> ACTIVE_BARRIERS_CLIENT = new CopyOnWriteArrayList<>(); // CLIENT

    // Add barrier to ACTIVE_BARRIERS
    public static void addBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks, Identifier texture, float radius, boolean isClient) {
        if (isClient) {
            ACTIVE_BARRIERS_CLIENT.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks, texture, null, radius));
        } else {
            ACTIVE_BARRIERS.add(new ActiveBarrier(ownerUuid, position, direction, maxTicks, texture, (entity, barrier) -> {}, radius));
        }
    }

    // Legacy method for adding a barrier with the default texture
    public static void addBarrier(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks, float radius, boolean isClient) {
        Identifier hexTexture = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");
        addBarrier(ownerUuid, position, direction, maxTicks, hexTexture, radius, isClient);
    }

    /**
     * Removes all active barriers owned by the specified UUID on the client side.
     */
    public static void removeBarrierClient(UUID ownerUuid) {
        if (ownerUuid == null) return;
        ACTIVE_BARRIERS_CLIENT.removeIf(barrier -> barrier.getOwnerUuid().equals(ownerUuid));
    }

    /**
     * Instantly destroys an active domain expansion barrier:
     * removes the domain floor, discards domain entities (e.g. shrine),
     * plays glass shatter sound and visual effect, sends client removal packet,
     * and removes it from the active barriers list.
     */
    public static void destroyDomain(ActiveBarrier barrier, ServerWorld world) {
        if (barrier == null || world == null) return;

        boolean wasActive = ACTIVE_BARRIERS.remove(barrier);
        if (!wasActive && barrier.isExpired()) {
            return;
        }
        barrier.expire();

        BlockPos blockPos = BlockPos.ofFloored(barrier.getPosition());
        // Glass shatter effect, visual and sound
        world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(Blocks.GLASS.getDefaultState()));
        world.playSound(null, barrier.getPosition().x, barrier.getPosition().y, barrier.getPosition().z,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0F, 0.9F);

        // If the barrier is of type Domain Expansion, clean up floor and custom domain entities
        if (barrier.getDomainExpansion() != null) {
            barrier.getDomainExpansion().removeDomainFloor(world, barrier);
            barrier.getDomainExpansion().onDomainRemoved(world, barrier);
        }

        // Notify all clients in the world to remove the barrier instantly
        RemoveBarrierPayload payload = new RemoveBarrierPayload(barrier.getOwnerUuid());
        for (ServerPlayerEntity player : PlayerLookup.world(world)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Called when an entity dies. If this entity was the owner of an active domain expansion,
     * the domain expansion disappears instantly.
     */
    public static void onOwnerDeath(LivingEntity owner, ServerWorld world) {
        if (owner == null || world == null) return;
        UUID ownerUuid = owner.getUuid();

        List<ActiveBarrier> toDestroy = new ArrayList<>();
        for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
            if (barrier.getOwnerUuid().equals(ownerUuid) && barrier.getDomainExpansion() != null && !barrier.isExpired()) {
                toDestroy.add(barrier);
            }
        }

        for (ActiveBarrier barrier : toDestroy) {
            destroyDomain(barrier, world);
        }

        DomainClashManager.handleEntityDeath(owner, world);
    }

    public static void tick(World world) {

        List<ActiveBarrier> listToTick = world.isClient ? ACTIVE_BARRIERS_CLIENT : ACTIVE_BARRIERS;

        for (int i = listToTick.size() - 1; i >= 0; i--) {
            if (i >= listToTick.size()) continue;
            ActiveBarrier barrier = listToTick.get(i);
            if (world.isClient || (world instanceof ServerWorld sw && sw.getRegistryKey() == World.OVERWORLD)) {
                barrier.tick();
            }

            // Blocking, Physics and Domain logic (Server-side)
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                if (barrier.getDirection().equals(Vec3d.ZERO)) {
                    double radius = barrier.getRadius();
                    boolean isOpen = barrier.isOpenBarrier();

                    // Only apply spherical wall collision physics if this domain is NOT an open barrier.
                    // Open barrier domains (Malevolent Shrine) allow entities to physically attempt an escape!
                    if (!isOpen) {
                        SpherePhysics.apply(world, barrier, radius);
                    }

                    // Apply domain effect on entities inside (neutralized during domain clashes!)
                    if (!barrier.isClashing()) {
                        Box domainBox = new Box(barrier.getPosition().subtract(radius, radius, radius), barrier.getPosition().add(radius, radius, radius));
                        serverWorld.getOtherEntities(null, domainBox).forEach(entity -> {
                            if (!entity.isSpectator() && !entity.getUuid().equals(barrier.getOwnerUuid())) {
                                if (entity.getPos().distanceTo(barrier.getPosition()) < radius) {
                                    barrier.getDomainEffect().accept(entity, barrier);
                                }
                            }
                        });
                    }

                    // Apply the domain expansion logic each tick (if applicable)
                    if (barrier.getDomainExpansion() != null) {
                        barrier.getDomainExpansion().onGlobalTick(world, barrier);
                    }

                    // Projectile logic for SPHERE
                    Box sphereBox = new Box(barrier.getPosition().subtract(radius, radius, radius),
                            barrier.getPosition().add(radius, radius, radius));

                    if (isOpen && barrier.getDomainExpansion() instanceof DomainExpansionShrine) {
                        // Open Barrier shredder: slices incoming projectiles out of the air
                        handleOpenDomainProjectiles(serverWorld, barrier, sphereBox, radius);
                    } else {
                        // Closed Barrier: reflects off outer barrier shell
                        handleProjectiles(world, barrier, sphereBox, radius, true);
                    }
                }
                // --- 2. HEX SHIELD LOGIC ---
                else {
                    // Expanded search box to reliably catch fast-moving projectiles (arrows travel at 3+ blocks/tick)
                    Box hexBox = Box.from(barrier.getPosition()).expand(2.2F);
                    handleProjectiles(world, barrier, hexBox, 2.2, false);
                }
            }

            if (barrier.isExpired()) {
                if (!world.isClient) {
                    BlockPos blockPos = BlockPos.ofFloored(barrier.getPosition());
                    // Glass shatter effect, visual and sound
                    world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(Blocks.GLASS.getDefaultState()));
                    world.playSound(null, barrier.getPosition().x, barrier.getPosition().y, barrier.getPosition().z,
                            SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.8F, 1.2F);

                    // If the barrier is of type Domain Expansion and clean up the Domain Floor
                    if (barrier.getDomainExpansion() != null && world instanceof ServerWorld serverWorld) {
                        barrier.getDomainExpansion().removeDomainFloor(world, barrier);
                        barrier.getDomainExpansion().onDomainRemoved(serverWorld, barrier);

                        RemoveBarrierPayload payload = new RemoveBarrierPayload(barrier.getOwnerUuid());
                        for (ServerPlayerEntity player : PlayerLookup.world(serverWorld)) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                }
                listToTick.remove(barrier);
            }
        }
    }

    /**
     * Handles projectiles entering an Open Barrier domain (Malevolent Shrine).
     * Unlike a closed barrier which bounces projectiles off the skin, the open domain's
     * slashes slice incoming hostile projectiles into pieces mid-flight.
     */
    public static void handleOpenDomainProjectiles(ServerWorld serverWorld, ActiveBarrier barrier, Box domainBox, double radius) {
        serverWorld.getEntitiesByClass(ProjectileEntity.class, domainBox, p -> true).forEach(projectile -> {
            Entity owner = projectile.getOwner();
            if (owner != null && owner.getUuid().equals(barrier.getOwnerUuid())) {
                return;
            }

            // Do not shred caster's Flame Arrow
            if (projectile instanceof FlameArrowEntity) {
                return;
            }

            Vec3d projPos = projectile.getPos();
            if (projPos.distanceTo(barrier.getPosition()) < radius) {
                // Sliced out of mid-air by domain slashes!
                serverWorld.playSound(null, projPos.x, projPos.y, projPos.z,
                        ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 0.9F, 1.6F);
                serverWorld.spawnParticles(ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                        projPos.x, projPos.y, projPos.z, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        projPos.x, projPos.y, projPos.z, 6, 0.15, 0.15, 0.15, 0.08);

                projectile.discard();
            }
        });
    }

    public static void handleProjectiles(World world, ActiveBarrier barrier, Box shieldBox, double radius, boolean isSphere) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        world.getEntitiesByClass(ProjectileEntity.class, shieldBox, p -> true).forEach(projectile -> {
            Entity owner = projectile.getOwner();
            if (owner != null && owner.getUuid().equals(barrier.getOwnerUuid())) {
                return;
            }

            Vec3d projPos = projectile.getPos();
            Vec3d barrierPos = barrier.getPosition();

            // Distance & Directional check
            if (isSphere) {
                double dist = projPos.distanceTo(barrierPos);
                // Only reflect if it's hitting the "skin" (radius +/- 0.6 blocks)
                if (dist > radius + 0.6 || dist < radius - 0.6) {
                    return;
                }
            } else {
                Vec3d shieldDir = barrier.getDirection().normalize();
                Vec3d toProj = projPos.subtract(barrierPos);
                double dist = toProj.length();

                // Check distance to shield center
                if (dist > 1.5) {
                    return;
                }

                // Check projectile velocity direction: ignore projectiles moving away from the shield
                Vec3d motion = projectile.getVelocity();
                if (motion.lengthSquared() > 0.01) {
                    double approachDot = motion.normalize().dotProduct(shieldDir);
                    if (approachDot > 0.35 && dist > 1.0) {
                        return; // Moving away from the shield
                    }
                }
            }

            // Do NOT deflect Flame Arrow! Cause it to explode on impact with defensive magic!
            if (projectile instanceof FlameArrowEntity flameArrow) {
                serverWorld.playSound(null, barrierPos.x, barrierPos.y, barrierPos.z,
                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 1.2F);
                flameArrow.detonate(flameArrow.getPos());
                return;
            }

            // Reflect projectile
            Vec3d motion = projectile.getVelocity();
            Vec3d shieldDir = barrier.getDirection().equals(Vec3d.ZERO)
                    ? projPos.subtract(barrierPos).normalize()
                    : barrier.getDirection().normalize();

            Vec3d reflectedMotion = new Vec3d(-motion.x, -motion.y + 0.15, -motion.z).multiply(1.5);
            if (reflectedMotion.lengthSquared() < 0.25) {
                reflectedMotion = shieldDir.multiply(1.2);
            }
            projectile.setVelocity(reflectedMotion);

            // Nudge projectile slightly outside the barrier so it doesn't re-collide
            projectile.setPosition(projPos.add(shieldDir.multiply(0.35)));

            // Update visual rotation
            float yaw = (float) (Math.atan2(reflectedMotion.x, reflectedMotion.z) * (180 / Math.PI));
            float pitch = (float) (Math.atan2(reflectedMotion.y, reflectedMotion.horizontalLength()) * (180 / Math.PI));
            projectile.setYaw(yaw);
            projectile.setPitch(pitch);
            projectile.velocityModified = true;

            // Set new owner to barrier caster (support both player and mob bosses)
            Entity barrierOwner = serverWorld.getEntity(barrier.getOwnerUuid());
            if (barrierOwner != null) {
                projectile.setOwner(barrierOwner);
            }

            // Sound and spark particles right at the projectile collision point
            serverWorld.playSound(null, projPos.x, projPos.y, projPos.z,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.4F, 1.8F);
            serverWorld.playSound(null, projPos.x, projPos.y, projPos.z,
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.4F);
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, projPos.x, projPos.y, projPos.z, 8, 0.15, 0.15, 0.15, 0.12);
            serverWorld.spawnParticles(ParticleTypes.CRIT, projPos.x, projPos.y, projPos.z, 6, 0.15, 0.15, 0.15, 0.1);
        });
    }

    // Checks if a specific player already owns an active Domain Expansion
    public static boolean hasActiveDomain(UUID playerUuid) {
        for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
            // If we find a barrier owned by this player that is a Domain Expansion
            if (barrier.getOwnerUuid().equals(playerUuid) && barrier.getDomainExpansion() != null && !barrier.isExpired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the active Domain Expansion barrier owned by the given entity UUID, if one exists and has not expired.
     */
    public static ActiveBarrier getActiveDomainBarrier(UUID playerUuid) {
        for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
            if (barrier.getOwnerUuid().equals(playerUuid) && barrier.getDomainExpansion() != null && !barrier.isExpired()) {
                return barrier;
            }
        }
        return null;
    }

    // Checks if an owner already has an active barrier covering a specific direction
    public static boolean hasActiveBarrierFor(UUID ownerUuid, Vec3d direction) {
        for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
            if (barrier.getOwnerUuid().equals(ownerUuid) && !barrier.isExpired()) {
                if (barrier.getDirection().equals(Vec3d.ZERO)) {
                    return true;
                }
                if (direction != null) {
                    double dot = barrier.getDirection().normalize().dotProduct(direction.normalize());
                    if (dot > 0.35) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
