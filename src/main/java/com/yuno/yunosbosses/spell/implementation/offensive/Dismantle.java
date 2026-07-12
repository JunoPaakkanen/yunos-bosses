package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
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

public class Dismantle extends Spell {

    public Dismantle(Identifier id) {
        super(id, true);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        fireDismantle(world, caster, staff, 1.0F);
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

        //  Randomly select 1 of 4 orientations (Horizontal, Vertical, Diagonal /, Diagonal \)
        int orientation = caster.getRandom().nextInt(4);
        Vec3d slashAxis = switch (orientation) {
            case 0 -> rightDir; // Horizontal (-)
            case 1 -> upDir;    // Vertical (|)
            case 2 -> rightDir.add(upDir).normalize(); // Diagonal (/)
            case 3 -> rightDir.subtract(upDir).normalize(); // Diagonal (\)
            default -> rightDir;
        };

        // Slash Configuration
        float slashWidth = 5.0f * potency;     // Total width of the slash in blocks
        int rayCount = 10;           // Shoots 10 parallel rays to form the "blade"
        float maxDistance = 25.0f;   // How far the slash travels
        float baseDamage = 20.0f;    // 100% Damage value
        float cooldown = 15.0F;      // Implement this later
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

                        target.damage(ModDamageTypes.of(world, ModDamageTypes.CUTTING_MAGIC, caster), finalDamage);

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
        return Text.translatable("yunosbosses.spell.dismantle");
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 50.0F;
    }
}
