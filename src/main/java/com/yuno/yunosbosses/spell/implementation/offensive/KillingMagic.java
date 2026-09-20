package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class KillingMagic extends Spell {

    public KillingMagic(Identifier id, SpellRarity rarity) {
        super(id, rarity);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        this.cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        float damageMultiplier = 1.0F;
        if (staff.getItem() instanceof StaffItem staffItem) {
            damageMultiplier = staffItem.getPowerMultiplier();
        }

        int maxRange;
        int delay;
        int durationTicks;
        float baseDamage;
        float beamRadius;
        float damageRadius;
        float tunnelRadius;
        float impactRadius;
        float soundPitch;
        float knockbackHoriz;
        float knockbackVert;

        switch (chargeLevel) {
            case 3 -> {
                maxRange = 70;
                delay = 8;
                durationTicks = 30;
                baseDamage = 100.0F;
                beamRadius = 1.15F;
                damageRadius = 2.2F;
                tunnelRadius = 2.2F;
                impactRadius = 4.0F;
                soundPitch = 0.80F;
                knockbackHoriz = 0.90F;
                knockbackVert = 0.20F;
            }
            case 2 -> {
                maxRange = 45;
                delay = 6;
                durationTicks = 22;
                baseDamage = 60.0F;
                beamRadius = 0.68F;
                damageRadius = 1.4F;
                tunnelRadius = 1.4F;
                impactRadius = 2.5F;
                soundPitch = 1.00F;
                knockbackHoriz = 0.55F;
                knockbackVert = 0.12F;
            }
            default -> {
                maxRange = 28;
                delay = 4;
                durationTicks = 16;
                baseDamage = 32.0F;
                beamRadius = 0.38F;
                damageRadius = 0.9F;
                tunnelRadius = 0.8F;
                impactRadius = 1.5F;
                soundPitch = 1.25F;
                knockbackHoriz = 0.35F;
                knockbackVert = 0.08F;
            }
        }

        float trueDamage = baseDamage * damageMultiplier;

        // Summon magic circle audio feedback
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.6F, 1.8F);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 1.5F);

        Vec3d look;
        boolean customDir = false;
        if (caster instanceof MobEntity mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            look = mob.getTarget().getEyePos().subtract(caster.getEyePos()).normalize();
            customDir = true;
        } else {
            look = caster.getRotationVector();
        }
        Vec3d start = caster.getEyePos().add(look.multiply(1.0));

        fireBeam(world, caster, start, maxRange, delay, durationTicks, beamRadius, damageRadius,
                trueDamage, tunnelRadius, impactRadius, soundPitch, knockbackHoriz, knockbackVert, customDir, customDir ? look : null);
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.killing_magic");
    }

    @Override
    public boolean canBeCharged() {
        return true;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 12.0F;
    }

    public void fireBeam(World world, LivingEntity caster, Vec3d start, int maxRange, int delay,
                         int durationTicks, float beamRadius, float damageRadius, float trueDamage,
                         float tunnelRadius, float impactRadius, float soundPitch,
                         float knockbackHoriz, float knockbackVert,
                         boolean useCustomStart, Vec3d customDirection) {

        BeamPayload payload = new BeamPayload(
                caster.getUuid(),
                start,
                maxRange,
                useCustomStart,
                customDirection,
                delay,
                durationTicks,
                beamRadius
        );

        if (caster instanceof ServerPlayerEntity player) {
            ServerPlayNetworking.send(player, payload);
        }

        for (ServerPlayerEntity player : PlayerLookup.tracking(caster)) {
            if (player != caster) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        Runnable fireAction = () -> {
            if (!caster.isAlive() && !(world instanceof ServerWorld)) return;
            ServerWorld serverWorld = (ServerWorld) world;

            Vec3d firingDir = customDirection != null ? customDirection : caster.getRotationVector();
            Vec3d firingOrigin = useCustomStart ? start : caster.getEyePos().add(firingDir.multiply(1.0));

            executeBeamEffect(serverWorld, caster, firingOrigin, firingDir, maxRange, damageRadius,
                    trueDamage, tunnelRadius, impactRadius, soundPitch, knockbackHoriz, knockbackVert);
        };

        if (delay <= 0) {
            fireAction.run();
        } else {
            DelayedServerEffects.delay(delay, fireAction);
        }
    }

    public void fireBeamTowardTarget(World world, LivingEntity caster, Vec3d start, Vec3d direction,
                                     int maxRange, int delay, int durationTicks, float beamRadius,
                                     float damageRadius, float trueDamage, float tunnelRadius,
                                     float impactRadius, float soundPitch,
                                     float knockbackHoriz, float knockbackVert) {
        fireBeam(world, caster, start, maxRange, delay, durationTicks, beamRadius, damageRadius,
                trueDamage, tunnelRadius, impactRadius, soundPitch, knockbackHoriz, knockbackVert, true, direction);
    }

    public static void executeBeamEffect(ServerWorld world, LivingEntity caster, Vec3d origin, Vec3d direction,
                                         int maxRange, float damageRadius, float trueDamage,
                                         float tunnelRadius, float impactRadius, float soundPitch,
                                         float knockbackHoriz, float knockbackVert) {

        // 1. Firing audio
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.5F, soundPitch);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.5F, soundPitch + 0.2F);
        if (soundPitch <= 0.9F) {
            world.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0F, 1.0F);
        }

        // 2. Step forward along ray
        Vec3d unitDir = direction.normalize();
        double stepDistance = 0.5;
        int totalSteps = (int) (maxRange / stepDistance);

        Set<Entity> hitEntities = new HashSet<>();
        Set<BlockPos> brokenBlocks = new HashSet<>();
        Vec3d currentPoint = origin;
        boolean hitObstacle = false;

        for (int s = 0; s < totalSteps; s++) {
            currentPoint = origin.add(unitDir.multiply(s * stepDistance));

            // Barrier collision check
            for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
                boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);
                if (!isSphere && barrier.getOwnerUuid().equals(caster.getUuid())) {
                    continue;
                }

                if (isSphere) {
                    double dist = currentPoint.distanceTo(barrier.getPosition());
                    if (dist <= barrier.getRadius() + 0.5 && dist >= barrier.getRadius() - 0.5) {
                        hitObstacle = true;
                        break;
                    }
                } else {
                    Box hexBox = Box.from(barrier.getPosition()).expand(0.9F);
                    if (hexBox.contains(currentPoint)) {
                        hitObstacle = true;
                        break;
                    }
                }
            }

            if (hitObstacle) {
                world.playSound(null, currentPoint.x, currentPoint.y, currentPoint.z,
                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 1.2F);
                world.playSound(null, currentPoint.x, currentPoint.y, currentPoint.z,
                        SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 1.4F);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, currentPoint.x, currentPoint.y, currentPoint.z, 20, 0.3, 0.3, 0.3, 0.2);
                break;
            }

            Box attackHitbox = new Box(
                    currentPoint.x - damageRadius, currentPoint.y - damageRadius, currentPoint.z - damageRadius,
                    currentPoint.x + damageRadius, currentPoint.y + damageRadius, currentPoint.z + damageRadius
            );

            world.getOtherEntities(caster, attackHitbox, Entity::isAlive).forEach(entity -> {
                if (hitEntities.contains(entity) || entity instanceof ItemEntity) return;
                hitEntities.add(entity);

                Vec3d preVel = entity.getVelocity();
                entity.damage(world, world.getDamageSources().indirectMagic(caster, caster), trueDamage);

                // Controlled kinetic displacement
                if (knockbackHoriz > 0.1F) {
                    entity.addVelocity(unitDir.x * knockbackHoriz, knockbackVert, unitDir.z * knockbackHoriz);
                } else {
                    // Suppressive pinning: dampens velocity and prevents vertical launch
                    double preservedY = Math.min(0.0, preVel.y);
                    entity.setVelocity(
                            preVel.x * 0.4 + unitDir.x * knockbackHoriz,
                            preservedY,
                            preVel.z * 0.4 + unitDir.z * knockbackHoriz
                    );
                }
                entity.velocityModified = true;

                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getBodyY(0.5), entity.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticles(ParticleTypes.CRIT, entity.getX(), entity.getBodyY(0.5), entity.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            });

            // Block vaporization tunnel
            int minX = (int) Math.floor(currentPoint.x - tunnelRadius);
            int maxX = (int) Math.floor(currentPoint.x + tunnelRadius);
            int minY = (int) Math.floor(currentPoint.y - tunnelRadius);
            int maxY = (int) Math.floor(currentPoint.y + tunnelRadius);
            int minZ = (int) Math.floor(currentPoint.z - tunnelRadius);
            int maxZ = (int) Math.floor(currentPoint.z + tunnelRadius);

            boolean hitBedrock = false;
            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        BlockPos bPos = new BlockPos(bx, by, bz);
                        if (brokenBlocks.contains(bPos)) continue;

                        BlockState state = world.getBlockState(bPos);
                        if (state.isAir()) continue;

                        if (state.getHardness(world, bPos) < 0.0F || state.isOf(Blocks.BEDROCK)) {
                            hitBedrock = true;
                            continue;
                        }

                        if (!state.getFluidState().isEmpty()) continue;

                        brokenBlocks.add(bPos.toImmutable());
                        world.setBlockState(bPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);

                        // Block fragments
                        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                                bPos.getX() + 0.5, bPos.getY() + 0.5, bPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.08);
                    }
                }
            }

            // Energy sparks along path
            if (s % 2 == 0) {
                world.spawnParticles(ParticleTypes.END_ROD, currentPoint.x, currentPoint.y, currentPoint.z, 2, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, currentPoint.x, currentPoint.y, currentPoint.z, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // 3. Detonate at terminus
        detonateImpact(world, currentPoint, impactRadius, soundPitch, brokenBlocks);
    }

    private static void detonateImpact(ServerWorld world, Vec3d pos, float impactRadius,
                                       float soundPitch, Set<BlockPos> brokenBlocks) {
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.2F, soundPitch + 0.1F);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5F, soundPitch + 0.3F);

        world.spawnParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 25, 0.5, 0.5, 0.5, 0.25);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 15, 0.4, 0.4, 0.4, 0.15);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 8, 0.4, 0.4, 0.4, 0.05);

        int craterRadius = (int) Math.ceil(impactRadius);
        BlockPos center = BlockPos.ofFloored(pos);
        for (BlockPos bPos : BlockPos.iterate(
                center.add(-craterRadius, -craterRadius, -craterRadius),
                center.add(craterRadius, craterRadius, craterRadius))) {

            if (brokenBlocks.contains(bPos)) continue;

            if (bPos.getSquaredDistance(pos) <= impactRadius * impactRadius) {
                BlockState state = world.getBlockState(bPos);
                if (!state.isAir() && state.getHardness(world, bPos) >= 0.0F
                        && !state.isOf(Blocks.BEDROCK) && state.getFluidState().isEmpty()) {

                    brokenBlocks.add(bPos.toImmutable());
                    world.setBlockState(bPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                            bPos.getX() + 0.5, bPos.getY() + 0.5, bPos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }
}
