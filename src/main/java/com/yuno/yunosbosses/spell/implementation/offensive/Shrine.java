package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.entity.projectile.FlameArrowEntity;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
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
import java.util.List;
import java.util.Set;

public class Shrine extends Spell {

    public Shrine(Identifier id, SpellRarity rarity) {
        super(id, true, rarity, true);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (caster.isSneaking()) {
            shootFlameArrow(world, caster, staff, 1.0F);
        } else {
            fireDismantle(world, caster, staff, 1.0F);
        }
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        float potency = switch (chargeLevel) {
            case 2 -> 1.5F; // Charge level 2 = 150% potency
            case 3 -> 3.0F; // Charge level 3 = 300% potency
            default -> 1.0F; // Charge level 1 (Instant) = Base potency
        };

        if (caster.isSneaking()) {
            shootFlameArrow(world, caster, staff, potency);
        } else {
            fireDismantle(world, caster, staff, potency);
        }
    }

    public void shootFlameArrow(World world, LivingEntity caster, ItemStack staff) {
        shootFlameArrow(world, caster, staff, 1.0F);
    }

    public void shootFlameArrow(World world, LivingEntity caster, ItemStack staff, float potency) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        // Apply staff power multiplier if using a staff
        float finalPotency = potency;
        if (staff.getItem() instanceof StaffItem staffItem) {
            finalPotency *= staffItem.getPowerMultiplier();
        }
        final float effectivePotency = finalPotency;

        // 1. Trigger player drawing animation across nearby clients and the caster
        Identifier animId = ModAnimations.FLAME_ARROW_ANIM;
        if (caster instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PlayerAnimationPayload(caster.getUuid(), animId));
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, caster.getPos(), 64)) {
            if (player != caster) {
                ServerPlayNetworking.send(player, new PlayerAnimationPayload(caster.getUuid(), animId));
            }
        }

        // 2. Play initial fire ignition audio
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.3f, 0.7f);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_BLASTFURNACE_FIRE_CRACKLE, SoundCategory.PLAYERS, 1.5f, 1.1f);

        // 3. Extended Drawing sequence: 30 ticks
        int chargeDurationTicks = 30;
        for (int t = 1; t <= chargeDurationTicks; t++) {
            final int currentTick = t;
            DelayedServerEffects.delay(t, () -> {
                if (!caster.isAlive()) return;

                Vec3d eyePos = caster.getEyePos();
                Vec3d lookDir = caster.getRotationVec(1.0F);

                Vec3d globalUp = new Vec3d(0, 1, 0);
                Vec3d rightDir = Math.abs(lookDir.y) > 0.99 ? new Vec3d(1, 0, 0) : lookDir.crossProduct(globalUp).normalize();
                Vec3d upDir = rightDir.crossProduct(lookDir).normalize();

                // Left hand: outstretched holding the front of the bow / arrow rest
                // Drives firmly forward from 0.88m to 1.02m
                double pushOut = Math.min(1.0, (double) currentTick / 22.0) * 0.14;
                Vec3d leftHand = eyePos.add(lookDir.multiply(0.88 + pushOut))
                        .add(rightDir.multiply(-0.28))
                        .add(upDir.multiply(-0.18));

                // Right hand: reaches forward at ticks 1-5 (gathering flame at left hand),
                // pulls back dramatically across ticks 6-23, and holds under full tension ticks 23-30
                Vec3d startPos = leftHand.add(lookDir.multiply(-0.10)).add(rightDir.multiply(0.06));
                Vec3d fullPullPos = eyePos.add(lookDir.multiply(0.38))
                        .add(rightDir.multiply(0.24))
                        .add(upDir.multiply(-0.18));

                double drawProgress;
                if (currentTick <= 5) {
                    drawProgress = 0.0;
                } else if (currentTick <= 23) {
                    // Dramatic ease-out pulling motion
                    double raw = (double) (currentTick - 5) / 18.0;
                    drawProgress = Math.sin(raw * Math.PI / 2.0);
                } else {
                    drawProgress = 1.0;
                }

                Vec3d rightHand = startPos.lerp(fullPullPos, drawProgress);

                // Trembling vibration when fully drawn (ticks 23-30)
                if (currentTick >= 23) {
                    double tremble = Math.sin(currentTick * 2.3) * 0.008;
                    rightHand = rightHand.add(upDir.multiply(tremble)).add(rightDir.multiply(tremble * 0.5));
                }

                Vec3d arrowDir = leftHand.subtract(rightHand).normalize();

                // A. Early Formation Ignition (Ticks 1-5)
                if (currentTick <= 5) {
                    // Concentrated spark / flame seed where hands gather
                    serverWorld.spawnParticles(ParticleTypes.FLAME, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, startPos.x, startPos.y, startPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                } else {
                    // B. Sharp, Radiant Fiery Arrow Shaft (stretching smoothly between rightHand and leftHand)
                    int segments = Math.max(4, (int) (14 * drawProgress));
                    for (int s = 0; s <= segments; s++) {
                        double frac = (double) s / segments;
                        Vec3d pt = rightHand.lerp(leftHand, frac);
                        // Stationary SMALL_FLAME for zero-drift crisp line
                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);

                        // Subtle inner glowing embers along the shaft
                        if (s % 3 == 0 || currentTick >= 23) {
                            serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);
                        }
                    }

                    // C. Exaggerated Arrowhead with swept-back fiery barbs extending beyond left hand
                    Vec3d tipPos = leftHand.add(arrowDir.multiply(0.24));
                    Vec3d barbBase = leftHand.add(arrowDir.multiply(0.07));
                    Vec3d leftBarb = barbBase.add(rightDir.multiply(-0.07)).add(upDir.multiply(0.035));
                    Vec3d rightBarb = barbBase.add(rightDir.multiply(0.07)).add(upDir.multiply(-0.035));

                    serverWorld.spawnParticles(ParticleTypes.FLAME, tipPos.x, tipPos.y, tipPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, leftBarb.x, leftBarb.y, leftBarb.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, rightBarb.x, rightBarb.y, rightBarb.z, 0, 0, 0, 0, 0);

                    // Gleaming tip sparks at full charge
                    if (currentTick >= 20) {
                        serverWorld.spawnParticles(ParticleTypes.CRIT, tipPos.x, tipPos.y, tipPos.z, 1, 0.02, 0.02, 0.02, 0.01);
                    }

                    // D. Exaggerated Fiery Bow Limbs & Dynamic Bowstring
                    if (drawProgress > 0.15) {
                        double limbExt = 0.22 + 0.20 * drawProgress; // Expanding fiery bow limbs
                        Vec3d upperLimb = leftHand.add(upDir.multiply(limbExt)).add(rightDir.multiply(-0.05));
                        Vec3d lowerLimb = leftHand.add(upDir.multiply(-limbExt)).add(rightDir.multiply(-0.05));

                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, upperLimb.x, upperLimb.y, upperLimb.z, 0, 0, 0, 0, 0);
                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, lowerLimb.x, lowerLimb.y, lowerLimb.z, 0, 0, 0, 0, 0);

                        int stringSteps = 5;
                        for (int i = 1; i < stringSteps; i++) {
                            double f = (double) i / stringSteps;
                            Vec3d pUpper = upperLimb.lerp(rightHand, f);
                            Vec3d pLower = lowerLimb.lerp(rightHand, f);
                            serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pUpper.x, pUpper.y, pUpper.z, 0, 0, 0, 0, 0);
                            serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pLower.x, pLower.y, pLower.z, 0, 0, 0, 0, 0);
                        }
                    }

                    // E. Inward Swirling Delicate Sparks
                    Vec3d arrowCenter = rightHand.lerp(leftHand, 0.5);
                    for (int i = 0; i < 2; i++) {
                        double angle = caster.getRandom().nextDouble() * Math.PI * 2;
                        double radius = 0.30 + caster.getRandom().nextDouble() * 0.25;
                        Vec3d spawnPos = arrowCenter.add(rightDir.multiply(Math.cos(angle) * radius))
                                .add(upDir.multiply(Math.sin(angle) * radius));
                        Vec3d inwardVel = arrowCenter.subtract(spawnPos).multiply(0.25);
                        serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE,
                                spawnPos.x, spawnPos.y, spawnPos.z, 0,
                                inwardVel.x, inwardVel.y, inwardVel.z, 0.25);
                    }
                }

                // Audio feedback during draw and tension build
                if (currentTick % 7 == 0) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 0.9f, 1.1f + (currentTick * 0.02f));
                }
                if (currentTick == 22) {
                    // Full draw tension reached sound
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.7f);
                }
                if (currentTick == 26) {
                    // High-energy friction sizzle
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 0.9f, 1.3f);
                }
            });
        }

        // 4. Launch at tick 30 (1.5s)!
        DelayedServerEffects.delay(chargeDurationTicks, () -> {
            if (!caster.isAlive()) return;
            launchFlameArrow(serverWorld, caster, effectivePotency);
        });
    }

    public void launchFlameArrow(ServerWorld serverWorld, LivingEntity caster, float potency) {
        Vec3d eyePos = caster.getEyePos();
        Vec3d lookDir = caster.getRotationVec(1.0F);
        Vec3d globalUp = new Vec3d(0, 1, 0);
        Vec3d rightDir = Math.abs(lookDir.y) > 0.99 ? new Vec3d(1, 0, 0) : lookDir.crossProduct(globalUp).normalize();
        Vec3d upDir = rightDir.crossProduct(lookDir).normalize();

        // 1. Powerful launch sounds
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 2.2f, 1.5f);
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.5f);
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 2.5f, 1.1f);

        // 2. Custom fiery shockwave forward (positioned cleanly at the bow rest in front of caster)
        Vec3d muzzlePos = eyePos.add(lookDir.multiply(1.02)).add(rightDir.multiply(-0.28)).add(upDir.multiply(-0.18));
        serverWorld.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 1, 0, 0, 0, 0);
        serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 12, lookDir.x * 0.3, lookDir.y * 0.3, lookDir.z * 0.3, 0.15);
        serverWorld.spawnParticles(ParticleTypes.FLAME, muzzlePos.x, muzzlePos.y, muzzlePos.z, 10, lookDir.x * 0.4, lookDir.y * 0.4, lookDir.z * 0.4, 0.25);

        // 3. Spawn the Flame Arrow projectile directly along the sightline (close to caster so point blank shots register)
        Vec3d arrowSpawnPos = eyePos.add(lookDir.multiply(0.2));
        FlameArrowEntity arrow = new FlameArrowEntity(ModEntities.FLAME_ARROW, serverWorld, caster, potency);
        arrow.setPosition(arrowSpawnPos.x, arrowSpawnPos.y, arrowSpawnPos.z);
        arrow.setVelocity(lookDir.multiply(2.8));
        serverWorld.spawnEntity(arrow);
    }

    public void fireDismantle(World world, LivingEntity caster, ItemStack staff, float potency) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        // Calculate the Local Camera Vectors
        Vec3d eyePos = caster.getEyePos();
        Vec3d lookDir = caster.getRotationVec(1.0F);

        // Find the "Right" and "Up" directions relative to where the caster is looking
        Vec3d globalUp = new Vec3d(0, 1, 0);
        Vec3d rightDir;

        // Prevent math errors if looking perfectly straight up or down
        if (Math.abs(lookDir.y) > 0.99) {
            rightDir = new Vec3d(1, 0, 0);
        } else {
            rightDir = lookDir.crossProduct(globalUp).normalize();
        }
        Vec3d upDir = rightDir.crossProduct(lookDir).normalize();

        //  Randomly select 1 of 4 orientations (Horizontal, Vertical, Diagonal /, Diagonal \\)
        int orientation = caster.getRandom().nextInt(4);
        Vec3d slashAxis = switch (orientation) {
            case 0 -> rightDir; // Horizontal (-)
            case 1 -> upDir;    // Vertical (|)
            case 2 -> rightDir.add(upDir).normalize(); // Diagonal (/)
            case 3 -> rightDir.subtract(upDir).normalize(); // Diagonal (\\\\)
            default -> rightDir;
        };

        // Slash Configuration
        float slashWidth = 5.0f * potency;     // Total width of the slash in blocks
        int rayCount = Math.max(10, (int) (slashWidth * 4));    // Shoots parallel rays to form the "blade"
        float maxDistance = 20.0f + (5.0f * potency);   // How far the slash travels
        float baseDamage = 20.0f;    // 100% Damage value
        float cooldown = 15.0F;      // TODO: Implement this later
        float damageMultiplier = potency;  // Damage multiplier granted by Staff item
        int penetrationDepth = Math.max(1, (int) (4 * potency)); // How many blocks/entities the slash penetrates

        // Apply damage multipliers
        if (staff.getItem() instanceof StaffItem staffItem) {
            damageMultiplier *= staffItem.getPowerMultiplier();
        }
        float trueDamage = baseDamage * damageMultiplier;

        // Trackers to prevent double-hitting the same entity or block across parallel rays
        Set<Entity> hitEntitiesThisCast = new HashSet<>();
        Set<BlockPos> brokenBlocksThisCast = new HashSet<>();

        // 4. Fire the Parallel Rays
        for (int i = 0; i <= rayCount; i++) {
            // Offset this specific ray from the center point to build the width of the blade
            double offset = ((double) i / rayCount - 0.5) * slashWidth;
            Vec3d rayStart = eyePos.add(slashAxis.multiply(offset));

            // hitObjects acts as our "Penetration Layer" tracker (0 = 100%, 1 = 70%, 2+ = 50%)
            int hitObjects = 0;

            // Step forward along the look direction
            for (double d = 0; d < maxDistance; d += 0.25) {
                Vec3d currentPos = rayStart.add(lookDir.multiply(d));

                // --- BARRIER INTERCEPT CHECK ---
                boolean hitBarrier = false;
                for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
                    boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);

                    // Allow the caster to safely shoot through their own directional Hex Shields
                    if (!isSphere && barrier.getOwnerUuid().equals(caster.getUuid())) {
                        continue;
                    }

                    if (isSphere) {
                        // SPHERICAL/DOMAIN BARRIER CHECK
                        double dist = currentPos.distanceTo(barrier.getPosition());
                        // Check if the ray has hit the 1-block thick skin of the domain
                        if (dist <= barrier.getRadius() + 0.5 && dist >= barrier.getRadius() - 0.5) {
                            hitBarrier = true;
                            break;
                        }
                    } else {
                        // HEX SHIELD CHECK
                        // Uses the exact same hitbox math found in BarrierManager
                        Box hexBox = Box.from(barrier.getPosition()).expand(0.8F);
                        if (hexBox.contains(currentPos)) {
                            hitBarrier = true;
                            break;
                        }
                    }
                }

                if (hitBarrier) {
                    // Shield hit sound
                    world.playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.5F);
                    // Spawn a spark particle
                    serverWorld.spawnParticles(ParticleTypes.CRIT, currentPos.x, currentPos.y, currentPos.z, 2, 0.1, 0.1, 0.1, 0.1);
                    // Spawn Dismantle particle
                    serverWorld.spawnParticles(ModParticles.DISMANTLE_B_PARTICLE, currentPos.x, currentPos.y, currentPos.z, 1, 0, 0, 0, 0);
                    // Terminate this specific ray, moving on to the next parallel ray
                    break;
                }

                // --- A. ENTITY CHECK ---
                Box checkBox = new Box(currentPos.x - 0.3, currentPos.y - 0.3, currentPos.z - 0.3,
                        currentPos.x + 0.3, currentPos.y + 0.3, currentPos.z + 0.3);

                List<Entity> entitiesNear = serverWorld.getOtherEntities(caster, checkBox);

                for (Entity entity : entitiesNear) {
                    // Ignore non-living entities and ensure we haven't already hit them
                    if (entity instanceof DomainShrineEntity) continue; // Explicitly ignore Shrine entity

                    if (entity instanceof LivingEntity target && !hitEntitiesThisCast.contains(target)) {
                        hitEntitiesThisCast.add(target);

                        // Calculate falloff damage based on what layer this is
                        float multiplier = (hitObjects == 0) ? 1.0f : ((hitObjects == 1) ? 0.7f : 0.5f);
                        float finalDamage = trueDamage * multiplier;

                        target.damage((ServerWorld) world, ModDamageTypes.of(world, ModDamageTypes.CUTTING_MAGIC, caster), finalDamage);

                        // Spawn custom Dismantle slash particle on hit entities
                        serverWorld.spawnParticles(ModParticles.DISMANTLE_A_PARTICLE,
                                currentPos.x, currentPos.y, currentPos.z,
                                1, 0, 0, 0, 0);

                        // Hitting a mob counts as penetrating a layer!
                        hitObjects++;
                    }
                }

                // If this specific ray has penetrated 4 objects/blocks, stop it and move to the next ray
                if (hitObjects >= penetrationDepth) break;

                // --- B. BLOCK CHECK ---
                BlockPos bPos = BlockPos.ofFloored(currentPos);
                if (!brokenBlocksThisCast.contains(bPos)) {
                    BlockState state = world.getBlockState(bPos);

                    // Only interact with solid, breakable blocks (ignores air and water)
                    if (!state.isAir() && state.getFluidState().isEmpty() && state.getHardness(world, bPos) >= 0.0F) {
                        brokenBlocksThisCast.add(bPos);

                        // Silently shred the block for maximum performance
                        world.setBlockState(bPos, net.minecraft.block.Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);

                        // Spawn dust particles
                        serverWorld.spawnParticles(ParticleTypes.POOF, currentPos.x, currentPos.y, currentPos.z, 1, 0.2, 0.2, 0.2, 0.05);

                        // Hitting a solid block counts as penetrating a layer!
                        hitObjects++;
                    }
                }

                if (hitObjects >= penetrationDepth) break;
            }
        }

        // Play an aggressive swoosh sound at the caster's location
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5f, 0.8f);
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.shrine");
    }

    @Override
    public boolean canBeCharged() {
        return true;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 50.0F;
    }
}
