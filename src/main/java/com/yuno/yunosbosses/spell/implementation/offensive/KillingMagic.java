package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
                baseDamage = 60.0F;
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
                baseDamage = 30.0F;
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
                baseDamage = 15.0F;
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
        return 60.0F;
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

        Vec3d firingDir = customDirection != null ? customDirection : caster.getRotationVector();
        Vec3d firingOrigin = useCustomStart ? start : caster.getEyePos().add(firingDir.multiply(1.0));

        // Alert reactive defenders (like Methode) in the firing line so they can raise shields in response
        notifyDefenders(world, caster, firingOrigin, firingDir, maxRange);

        Runnable fireAction = () -> {
            if (!caster.isAlive() && !(world instanceof ServerWorld)) return;
            ServerWorld serverWorld = (ServerWorld) world;

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

    /**
     * Alerts potential defenders (such as Methode) in the line of fire so they can react and raise shields.
     */
    public static void notifyDefenders(World world, LivingEntity caster, Vec3d origin, Vec3d direction, int maxRange) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        Vec3d unitDir = direction.normalize();
        Vec3d endPoint = origin.add(unitDir.multiply(maxRange));
        Box searchBox = new Box(origin, endPoint).expand(4.0);

        List<LivingEntity> potentialDefenders = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e.isAlive() && !e.getUuid().equals(caster.getUuid())
        );

        for (LivingEntity defender : potentialDefenders) {
            Vec3d toDefender = defender.getBoundingBox().getCenter().subtract(origin);
            double projAlongBeam = toDefender.dotProduct(unitDir);

            // Must be in front of the beam and within range
            if (projAlongBeam > 0 && projAlongBeam <= maxRange) {
                // Perpendicular distance to the beam ray
                Vec3d closestPointOnBeam = origin.add(unitDir.multiply(projAlongBeam));
                double perpDist = defender.getBoundingBox().getCenter().distanceTo(closestPointOnBeam);

                if (perpDist <= (defender.getWidth() / 2.0) + 2.0) {
                    if (defender instanceof MethodeEntity methode) {
                        methode.tryDefendAgainst(origin);
                    }
                }
            }
        }
    }

    /**
     * Checks if a point along the beam ray collides with an active barrier.
     * Spherical barriers block beams from inside and outside, even for the owner.
     * Hex barriers allow the owner to fire through them, but block everyone else.
     */
    public static ActiveBarrier findCollidingBarrier(Vec3d currentPoint, Vec3d unitDir, Vec3d origin, UUID casterUuid) {
        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            if (barrier.isExpired()) continue;

            boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);

            if (isSphere) {
                // Open barriers (e.g. Malevolent Shrine) do not physically stop beams
                if (barrier.isOpenBarrier()) continue;

                double sphereRadius = barrier.getRadius();
                Vec3d center = barrier.getPosition();
                boolean casterOutside = origin.distanceTo(center) > sphereRadius;

                if (casterOutside) {
                    // Attack fired from outside: collides with exterior barrier shell
                    double dist = currentPoint.distanceTo(center);
                    if (dist <= sphereRadius + 0.4) {
                        return barrier;
                    }
                } else {
                    // Attack fired from inside: spherical barrier blocks beams from inside AND outside,
                    // even if the one firing is the owner!
                    double dist = currentPoint.distanceTo(center);
                    if (dist >= sphereRadius - 0.4) {
                        return barrier;
                    }
                }
            } else {
                // Directional Hex Shield (DefensiveMagic)
                // The hex barrier allows the owner to fire through it, but no one else!
                if (barrier.getOwnerUuid().equals(casterUuid)) {
                    continue; // Owner can fire through their own hex barrier
                }

                Vec3d shieldPos = barrier.getPosition();
                Vec3d shieldDir = barrier.getDirection().normalize();
                float shieldRadius = barrier.getRadius() > 0 ? barrier.getRadius() : 1.6F;

                // Check distance to the shield disk plane and radial distance from shield center
                double planeDist = currentPoint.subtract(shieldPos).dotProduct(shieldDir);
                Vec3d projOnPlane = currentPoint.subtract(shieldDir.multiply(planeDist));
                double radialDist = projOnPlane.distanceTo(shieldPos);

                if (radialDist <= shieldRadius + 0.3 && Math.abs(planeDist) <= 0.6) {
                    return barrier;
                }

                // Fallback check against shield position center
                if (currentPoint.distanceTo(shieldPos) <= shieldRadius + 0.3) {
                    return barrier;
                }
            }
        }
        return null;
    }

    /**
     * Determines whether an entity is currently protected by an active barrier.
     * Spherical barriers block beams from crossing inside/outside boundaries for everyone.
     * Hex barriers allow only the owner to fire through them.
     */
    public static boolean isEntityProtectedByBarrier(Entity entity, Vec3d origin, Vec3d unitDir, UUID casterUuid) {
        UUID entityUuid = entity.getUuid();

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            if (barrier.isExpired()) continue;

            // 1. Hex Barrier (DefensiveMagic)
            if (!barrier.getDirection().equals(Vec3d.ZERO)) {
                // The hex barrier allows the owner to fire through it but no one else
                if (barrier.getOwnerUuid().equals(casterUuid)) {
                    continue;
                }

                // For non-owner casters, check if the hex barrier is positioned between caster origin and the entity
                Vec3d shieldPos = barrier.getPosition();
                Vec3d toShield = shieldPos.subtract(origin);
                Vec3d toEntity = entity.getEyePos().subtract(origin);

                double shieldProj = toShield.dotProduct(unitDir);
                double entityProj = toEntity.dotProduct(unitDir);

                // Shield is between caster and entity along the line of fire
                if (shieldProj > 0 && shieldProj <= entityProj + 0.5) {
                    Vec3d beamAtShield = origin.add(unitDir.multiply(shieldProj));
                    if (beamAtShield.distanceTo(shieldPos) <= barrier.getRadius() + 0.4) {
                        return true;
                    }
                }
            }

            // 2. Spherical Barrier
            // Blocks beams between inside and outside for everyone, even if the one firing is the owner.
            if (barrier.getDirection().equals(Vec3d.ZERO)) {
                if (barrier.isOpenBarrier()) continue;

                Vec3d center = barrier.getPosition();
                double radius = barrier.getRadius();
                boolean casterInside = origin.distanceTo(center) <= radius;
                boolean entityInside = entity.getPos().distanceTo(center) <= radius + 0.5;

                // If one is inside and the other is outside, the spherical barrier blocks the beam!
                if (casterInside != entityInside) {
                    return true;
                }
            }
        }

        return false;
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

        // Re-notify any reactive defenders at execution time in case positions shifted
        notifyDefenders(world, caster, origin, direction, maxRange);

        // 2. Step forward along ray
        Vec3d unitDir = direction.normalize();
        double stepDistance = 0.5;
        int totalSteps = (int) (maxRange / stepDistance);

        Set<Entity> hitEntities = new HashSet<>();
        Set<BlockPos> brokenBlocks = new HashSet<>();
        Vec3d currentPoint = origin;
        boolean hitBarrierObstacle = false;

        for (int s = 0; s < totalSteps; s++) {
            currentPoint = origin.add(unitDir.multiply(s * stepDistance));
            double currentDist = s * stepDistance;

            // 1. Barrier collision check FIRST
            ActiveBarrier barrierHit = findCollidingBarrier(currentPoint, unitDir, origin, caster.getUuid());
            if (barrierHit != null) {
                hitBarrierObstacle = true;
                break;
            }

            // 2. Check bedrock / unbreakable block
            BlockPos checkPos = BlockPos.ofFloored(currentPoint);
            BlockState checkState = world.getBlockState(checkPos);
            if (checkState.isOf(Blocks.BEDROCK) || checkState.getHardness(world, checkPos) < 0) {
                break;
            }

            // 3. Attack entities
            Box attackHitbox = new Box(
                    currentPoint.x - damageRadius, currentPoint.y - damageRadius, currentPoint.z - damageRadius,
                    currentPoint.x + damageRadius, currentPoint.y + damageRadius, currentPoint.z + damageRadius
            );

            world.getOtherEntities(caster, attackHitbox, Entity::isAlive).forEach(entity -> {
                if (hitEntities.contains(entity) || entity instanceof ItemEntity) return;

                // Ensure the beam has actually reached this entity along the ray
                Vec3d toEntity = entity.getBoundingBox().getCenter().subtract(origin);
                double projDist = toEntity.dotProduct(unitDir);
                if (projDist > currentDist + 0.6) {
                    return; // Beam tip has not reached entity yet
                }

                // Ensure entity is not shielded by an active barrier
                if (isEntityProtectedByBarrier(entity, origin, unitDir, caster.getUuid())) {
                    return; // Protected by barrier!
                }

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

            // 4. Block vaporization tunnel
            int minX = (int) Math.floor(currentPoint.x - tunnelRadius);
            int maxX = (int) Math.floor(currentPoint.x + tunnelRadius);
            int minY = (int) Math.floor(currentPoint.y - tunnelRadius);
            int maxY = (int) Math.floor(currentPoint.y + tunnelRadius);
            int minZ = (int) Math.floor(currentPoint.z - tunnelRadius);
            int maxZ = (int) Math.floor(currentPoint.z + tunnelRadius);

            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        BlockPos bPos = new BlockPos(bx, by, bz);
                        if (brokenBlocks.contains(bPos)) continue;

                        BlockState state = world.getBlockState(bPos);
                        if (state.isAir()) continue;

                        if (state.getHardness(world, bPos) < 0.0F || state.isOf(Blocks.BEDROCK)) {
                            continue;
                        }

                        if (!state.getFluidState().isEmpty()) continue;

                        brokenBlocks.add(bPos.toImmutable());
                        if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                            world.setBlockState(bPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }

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

        if (hitBarrierObstacle) {
            world.playSound(null, currentPoint.x, currentPoint.y, currentPoint.z,
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0F, 1.2F);
            world.playSound(null, currentPoint.x, currentPoint.y, currentPoint.z,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.4F, 1.4F);
            world.playSound(null, currentPoint.x, currentPoint.y, currentPoint.z,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 2.0F, 1.6F);

            world.spawnParticles(ParticleTypes.FLASH, currentPoint.x, currentPoint.y, currentPoint.z, 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, currentPoint.x, currentPoint.y, currentPoint.z, 30, 0.4, 0.4, 0.4, 0.25);
            world.spawnParticles(ParticleTypes.CRIT, currentPoint.x, currentPoint.y, currentPoint.z, 15, 0.3, 0.3, 0.3, 0.15);
            world.spawnParticles(ParticleTypes.END_ROD, currentPoint.x, currentPoint.y, currentPoint.z, 10, 0.3, 0.3, 0.3, 0.1);
            return;
        }

        // 3. Detonate at terminus (only when hitting blocks/ground or max range, not on barrier!)
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
                    // Do NOT destroy blocks if MobGriefing is disabled
                    if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                        world.setBlockState(bPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                            bPos.getX() + 0.5, bPos.getY() + 0.5, bPos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }
}
