package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.block.ModBlocks;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.DomainCutscenePayload;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public abstract class DomainExpansion extends Spell {

    public DomainExpansion(Identifier id, Identifier castAnimation, SpellRarity rarity) {
        super(id, true, rarity);
        this.castAnimation = castAnimation;
    }

    public abstract Identifier getBarrierTexture();

    /**
     * Barrier texture to use when casting in closed/contained mode.
     */
    public Identifier getClosedBarrierTexture() {
        return getBarrierTexture();
    }

    public abstract int getLifetimeTicks();
    public abstract float getRadius();
    public abstract void onDomainEffect(Entity affectedEntity, ActiveBarrier barrier);

    /**
     * Whether this domain defaults to an Open Barrier domain (e.g. Malevolent Shrine).
     * Open barrier domains do not confine entities with physical wall collision physics,
     * allowing entities to attempt an escape while enforcing binding vows (such as extended radius/lethality).
     */
    public boolean isOpenBarrier() {
        return false;
    }

    /**
     * The floor block type to construct within this domain.
     */
    public Block getDomainFloorBlock() {
        return ModBlocks.DOMAIN_FLOOR;
    }

    /**
     * Whether this domain expansion should instantly clear/destroy all blocks within its interior
     * upon activation to eliminate visual clutter.
     */
    public boolean shouldClearBlocksInside() {
        return false;
    }

    // Default mana cost
    public float manaCost = 250.0F;
    protected Identifier castAnimation;

    @Override
    public float getManaCost(LivingEntity caster) {
        return manaCost;
    }

    public float getRadius(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> 30.0F; // Charge Level 2
            case 3 -> 50.0F; // Charge Level 3
            default -> 20.0F; // Charge Level 1 (Instant / Base)
        };
    }

    public void startDomainExpansionCast(World world, LivingEntity caster, String domainName, boolean isOpenBarrier) {
        int durationTicks = caster.isPlayer() ? 40 : 60;
        startDomainExpansionCast(world, caster, domainName, isOpenBarrier, durationTicks);
    }

    public void startDomainExpansionCast(World world, LivingEntity caster, String domainName, boolean isOpenBarrier, int durationTicks) {
        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            // 3D Player Animation
            ServerPlayNetworking.send(player, getDomainCastAnimation(caster, castAnimation));

            // 2D Cutscene
            ServerPlayNetworking.send(player, new DomainCutscenePayload(caster.getUuid(), domainName, durationTicks, isOpenBarrier));
        }
    }

    public void startDomainExpansionCast(World world, LivingEntity caster, String domainName) {
        startDomainExpansionCast(world, caster, domainName, isOpenBarrier());
    }

    public void finishDomainExpansionCast(World world, LivingEntity caster, ItemStack staff, float radius) {
        finishDomainExpansionCast(world, caster, staff, radius, isOpenBarrier());
    }

    public void finishDomainExpansionCast(World world, LivingEntity caster, ItemStack staff, float radius, boolean isOpenBarrier) {
        if (!world.isClient) {
            Vec3d pos = caster.getPos().add(0, 2, 0); // Center of the sphere

            Identifier texture = isOpenBarrier ? getBarrierTexture() : getClosedBarrierTexture();
            int lifetime = getLifetimeTicks();

            // Create the floor
            createDomainFloor(world, caster, radius);

            // Domain creation hook
            onDomainCreated((ServerWorld) world, caster, pos);

            // Create the barrier
            ActiveBarrier activeBarrier = new ActiveBarrier(caster.getUuid(), pos, Vec3d.ZERO, lifetime, radius, texture, this::onDomainEffect, this, isOpenBarrier);
            if (!isOpenBarrier) {
                // Record all entities currently inside the domain as captured
                Box box = new Box(pos.subtract(radius + 2.0, radius + 2.0, radius + 2.0), pos.add(radius + 2.0, radius + 2.0, radius + 2.0));
                for (Entity e : ((ServerWorld) world).getOtherEntities(caster, box)) {
                    if (e.getPos().distanceTo(pos) <= (radius + 1.5)) {
                        activeBarrier.capturePlayer(e.getUuid());
                    }
                }
            }
            BarrierManager.ACTIVE_BARRIERS.add(activeBarrier);

            // Broadcast to ALL nearby players so they can see the barrier and animation
            for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, pos, 64)) {
                ServerPlayNetworking.send(player, new BarrierPayload(caster.getUuid(), pos, Vec3d.ZERO, lifetime, texture, radius));
            }
        }
    }

    // Hooks used for custom domain logic upon creation and removal
    public void onDomainCreated(ServerWorld world, LivingEntity caster, Vec3d barrierCenter) {}

    public void onDomainRemoved(ServerWorld world, ActiveBarrier barrier) {}

    public void onGlobalTick(World world, ActiveBarrier barrier) {
        // Implement this method to perform any global tick logic, for entity-specific logic use onDomainEffect() instead
    }

    public void createDomainFloor(World world, LivingEntity caster, double radius) {
        // Establish the Center and the fixed Floor Height
        BlockPos centerPos = caster.getBlockPos();
        int floorY = centerPos.getY() - 1;

        double blockRadiusSq = (radius + 0.5) * (radius + 0.5);
        int loopRadius = (int) Math.ceil(radius + 1);

        // BlockState definitions
        var floorState = getDomainFloorBlock().getDefaultState();
        var airState = Blocks.AIR.getDefaultState();

        // --- OPTIONAL: INSTANT INTERIOR BLOCK CLEARING (Eliminates visual clutter) ---
        // ONLY clear blocks strictly ABOVE the floor (y >= floorY + 1). Never touch or destroy blocks below the floor.
        if (shouldClearBlocksInside()) {
            Vec3d barrierCenter = caster.getPos().add(0, 2, 0);
            double sphereRadiusSq = (radius + 0.5) * (radius + 0.5);
            int minX = (int) Math.floor(barrierCenter.x - radius - 1.0);
            int maxX = (int) Math.ceil(barrierCenter.x + radius + 1.0);
            int minY = floorY + 1; // Never destroy blocks below the floor
            int maxY = (int) Math.ceil(barrierCenter.y + radius + 1.0);
            int minZ = (int) Math.floor(barrierCenter.z - radius - 1.0);
            int maxZ = (int) Math.ceil(barrierCenter.z + radius + 1.0);

            BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double dx = (x + 0.5) - barrierCenter.x;
                        double dy = (y + 0.5) - barrierCenter.y;
                        double dz = (z + 0.5) - barrierCenter.z;
                        if ((dx * dx) + (dy * dy) + (dz * dz) <= sphereRadiusSq) {
                            mutablePos.set(x, y, z);
                            var state = world.getBlockState(mutablePos);
                            if (!state.isAir() && state.getHardness(world, mutablePos) >= 0.0f) {
                                world.setBlockState(mutablePos, airState, Block.NOTIFY_LISTENERS);
                            }
                        }
                    }
                }
            }
        }

        // --- PASS 1: FLOOR BLOCK PLACEMENT ---
        for (int x = -loopRadius; x <= loopRadius; x++) {
            for (int z = -loopRadius; z <= loopRadius; z++) {
                double distanceSq = (x * x) + (z * z);

                if (distanceSq <= blockRadiusSq) {
                    int targetX = centerPos.getX() + x;
                    int targetZ = centerPos.getZ() + z;
                    BlockPos floorPos = new BlockPos(targetX, floorY, targetZ);

                    world.setBlockState(floorPos, floorState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                }
            }
        }

        // --- PASS 2: ENTITY HANDLING ---
        // Expand the box slightly to catch entities transitioning near the border
        Box domainBounds = new Box(centerPos).expand(radius + 1.0).withMaxY(floorY + 5.0);
        List<Entity> entitiesInDomain = world.getOtherEntities(null, domainBounds);

        for (Entity entity : entitiesInDomain) {
            double dx = entity.getX() - centerPos.getX();
            double dz = entity.getZ() - centerPos.getZ();

            // Check if the entity is genuinely inside the smooth horizontal sphere
            if ((dx * dx) + (dz * dz) <= (radius * radius)) {
                double targetSpawnY = floorY + 1.0;

                // Clear a tiny 2-block safety bubble for the entity, so they don't suffocate
                BlockPos entityPos = BlockPos.ofFloored(entity.getX(), targetSpawnY, entity.getZ());
                world.setBlockState(entityPos, airState, Block.NOTIFY_LISTENERS);
                world.setBlockState(entityPos.up(), airState, Block.NOTIFY_LISTENERS);

                entity.setVelocity(entity.getVelocity().x, 0, entity.getVelocity().z);
                entity.velocityModified = true;

                entity.teleport((ServerWorld) world, entity.getX(), targetSpawnY, entity.getZ(),
                        java.util.Collections.emptySet(), entity.getYaw(), entity.getPitch(), false);

                System.out.println("Teleported " + entity.getName().getString() + " to " + targetSpawnY);
            }
        }
    }

    public void removeDomainFloor(World world, ActiveBarrier barrier) {
        // Reverse-engineer the floor height from the barrier position
        BlockPos centerPos = BlockPos.ofFloored(
                barrier.getPosition().x,
                barrier.getPosition().y - 2,
                barrier.getPosition().z
        );
        int floorY = centerPos.getY() - 1;

        double radius = barrier.getRadius();
        double blockRadiusSq = (radius + 0.5) * (radius + 0.5);
        int loopRadius = (int) Math.ceil(radius + 1);

        var airState = Blocks.AIR.getDefaultState();

        for (int x = -loopRadius; x <= loopRadius; x++) {
            for (int z = -loopRadius; z <= loopRadius; z++) {
                double distanceSq = (x * x) + (z * z);

                if (distanceSq <= blockRadiusSq) {
                    int targetX = centerPos.getX() + x;
                    int targetZ = centerPos.getZ() + z;
                    BlockPos floorPos = new BlockPos(targetX, floorY, targetZ);

                    // Remove if it's either standard domain floor or void domain floor
                    var state = world.getBlockState(floorPos);
                    if (state.isOf(ModBlocks.DOMAIN_FLOOR) || state.isOf(ModBlocks.DOMAIN_FLOOR_VOID)) {
                        world.setBlockState(floorPos, airState, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    public PlayerAnimationPayload getDomainCastAnimation(LivingEntity caster, Identifier castAnimation) {
        return new PlayerAnimationPayload(
                caster.getUuid(),
                castAnimation
        );
    }

}
