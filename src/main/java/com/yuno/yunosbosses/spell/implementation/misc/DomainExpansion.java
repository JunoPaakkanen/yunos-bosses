package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.block.ModBlocks;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.DomainCutscenePayload;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.spell.Spell;
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

    public DomainExpansion(Identifier id, Identifier castAnimation) {
        super(id, true);
        this.castAnimation = castAnimation;
    }

    public abstract Identifier getBarrierTexture();
    public abstract int getLifetimeTicks();
    public abstract float getRadius();
    public abstract void onDomainEffect(Entity affectedEntity, ActiveBarrier barrier);

    // Default mana cost
    public float manaCost = 100.0F;
    protected Identifier castAnimation;

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}

    public void startDomainExpansionCast(World world, LivingEntity caster, String domainName) {
        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            // 3D Player Animation
            ServerPlayNetworking.send(player, getDomainCastAnimation(caster, castAnimation));

            int durationTicks;
            // 2-second cast duration for players, 3-seconds for bosses
            if (caster.isPlayer()) {durationTicks = 40;}
            else {durationTicks = 60;}

            // 2D Cutscene
            ServerPlayNetworking.send(player, new DomainCutscenePayload(caster.getUuid(), domainName, durationTicks));
        }
    }

    public void finishDomainExpansionCast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            Vec3d pos = caster.getPos().add(0, 2, 0); // Center of the sphere

            double radius = getRadius();
            Identifier texture = getBarrierTexture();
            int lifetime = getLifetimeTicks();

            // Create the floor
            createDomainFloor(world, caster, radius);

            // Domain creation hook
            onDomainCreated((ServerWorld) world, caster, pos);

            // Create the barrier
            BarrierManager.ACTIVE_BARRIERS.add(
                    new ActiveBarrier(caster.getUuid(), pos, Vec3d.ZERO, lifetime, texture, this::onDomainEffect, this)
            );

            // Broadcast to ALL nearby players so they can see the barrier and animation
            for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, pos, 64)) {
                ServerPlayNetworking.send(player, new BarrierPayload(caster.getUuid(), pos, Vec3d.ZERO, lifetime, texture));
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
        // --- DOMAIN FLOOR ---
        // Establish the Center and the fixed Floor Height
        BlockPos centerPos = caster.getBlockPos();
        int floorY = centerPos.getY() - 1;

        double blockRadiusSq = (radius + 0.5) * (radius + 0.5);
        int loopRadius = (int) Math.ceil(radius + 1);

        // BlockState definitions
        var floorState = ModBlocks.DOMAIN_FLOOR.getDefaultState();
        var airState = Blocks.AIR.getDefaultState();

        // --- PASS 1: BLOCK MODIFICATION PIPELINE ---
        for (int x = -loopRadius; x <= loopRadius; x++) {
            for (int z = -loopRadius; z <= loopRadius; z++) {
                double distanceSq = (x * x) + (z * z);

                if (distanceSq <= blockRadiusSq) {
                    int targetX = centerPos.getX() + x;
                    int targetZ = centerPos.getZ() + z;
                    BlockPos floorPos = new BlockPos(targetX, floorY, targetZ);

                    world.setBlockState(floorPos, floorState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);

                    // Clear columns efficiently
                    for (int h = 1; h <= 8; h++) {
                        world.setBlockState(floorPos.up(h), airState, Block.NOTIFY_LISTENERS);
                    }
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

                entity.setVelocity(entity.getVelocity().x, 0, entity.getVelocity().z);
                entity.velocityModified = true;

                entity.teleport((ServerWorld) world, entity.getX(), targetSpawnY, entity.getZ(),
                        java.util.Collections.emptySet(), entity.getYaw(), entity.getPitch());

                System.out.println("Teleported " + entity.getName().getString() + " to " + targetSpawnY);
            }
        }
    }

    public void removeDomainFloor(World world, ActiveBarrier barrier) {
        // Reverse-engineer the floor height from the barrier position
        BlockPos centerPos = BlockPos.ofFloored(
                barrier.getPosition().x,
                barrier.getPosition().y -2,
                barrier.getPosition().z
        );
        int floorY = centerPos.getY() - 1;

        double radius = getRadius();
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

                    // ONLY remove it if it's a domain floor block
                    if (world.getBlockState(floorPos).isOf(ModBlocks.DOMAIN_FLOOR)) {
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
