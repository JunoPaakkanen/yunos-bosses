package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.binding_vow.BindingVowManager;
import com.yuno.yunosbosses.binding_vow.ModBindingVows;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.entity.projectile.FlameArrowEntity;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.InnateHudData;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansionShrine;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
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
        this.cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        if (component.getShrineCooldown() > 0) {
            return;
        }

        float potency = switch (chargeLevel) {
            case 2 -> 1.5F; // Charge level 2 = 150% potency
            case 3 -> 3.0F; // Charge level 3 = 300% potency
            default -> 1.0F; // Charge level 1 (Instant) = Base potency
        };

        if (caster.isSneaking()) {
            // Flame Arrow ALWAYS requires 100% meter to unlock!
            if (component.getMeter(this) < 100) {
                if (caster instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("§cDivine Flame is sealed! Requires 100% Open meter.§r"), true);
                }
                // Refund mana since flame arrow could not be cast without 100% meter
                ModEntityComponents.MANA.get(caster).addMana(this.getManaCost(caster));
                return;
            }

            boolean hasFurnaceVow = BindingVowManager.hasVow(caster, ModBindingVows.FURNACE);

            ActiveBarrier activeBarrier = BarrierManager.getActiveDomainBarrier(caster.getUuid());
            boolean insideShrineDomain = activeBarrier != null &&
                    activeBarrier.getDomainExpansion() instanceof DomainExpansionShrine &&
                    activeBarrier.getPosition().distanceTo(caster.getPos()) <= activeBarrier.getRadius();

            int remainingTicks = activeBarrier != null ? (activeBarrier.getMaxTicks() - activeBarrier.getCurrentTicks()) : 0;
            boolean isFinisherWindow = insideShrineDomain && remainingTicks <= 160 && remainingTicks > 0;

            component.setMeter(this, 0);

            if (hasFurnaceVow && isFinisherWindow) {
                // Thermobaric finisher unlocked with Furnace Binding Vow during the domain finisher window
                component.setShrineCooldown(60);
                DomainExpansionShrine.triggerKaminoFinisher(world, caster, staff, chargeLevel, activeBarrier);
            } else {
                component.setShrineCooldown(40);
                // With Furnace Vow outside the domain finisher window, Flame Arrow loses all AOE (single-target direct hit only).
                // Without Furnace Vow, it maintains its standard high-damage AOE whether inside or outside the domain.
                boolean singleTargetOnly = hasFurnaceVow;
                shootFlameArrow(world, caster, staff, potency, singleTargetOnly);
            }
        } else {
            component.setShrineCooldown(10);
            component.addMeter(this, 10);
            fireDismantle(world, caster, staff, potency);
        }
    }

    public void shootFlameArrow(World world, LivingEntity caster, ItemStack staff) {
        shootFlameArrow(world, caster, staff, 1.0F, false);
    }

    public void shootFlameArrow(World world, LivingEntity caster, ItemStack staff, float potency) {
        shootFlameArrow(world, caster, staff, potency, false);
    }

    public void shootFlameArrow(World world, LivingEntity caster, ItemStack staff, float potency, boolean singleTargetOnly) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        // Fuga sound effect
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.FUGA, SoundCategory.NEUTRAL, 1.0f, 1.0f + (caster.getRandom().nextFloat() * 0.2f - 0.1f));

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
                double pushOut = Math.min(1.0, (double) currentTick / 22.0) * 0.14;
                Vec3d leftHand = eyePos.add(lookDir.multiply(0.88 + pushOut))
                        .add(rightDir.multiply(-0.28))
                        .add(upDir.multiply(-0.18));

                // Right hand: reaches forward at ticks 1-5, pulls back dramatically ticks 6-23
                Vec3d startPos = leftHand.add(lookDir.multiply(-0.10)).add(rightDir.multiply(0.06));
                Vec3d fullPullPos = eyePos.add(lookDir.multiply(0.38))
                        .add(rightDir.multiply(0.24))
                        .add(upDir.multiply(-0.18));

                double drawProgress;
                if (currentTick <= 5) {
                    drawProgress = 0.0;
                } else if (currentTick <= 23) {
                    double raw = (double) (currentTick - 5) / 18.0;
                    drawProgress = Math.sin(raw * Math.PI / 2.0);
                } else {
                    drawProgress = 1.0;
                }

                Vec3d rightHand = startPos.lerp(fullPullPos, drawProgress);

                // Trembling vibration when fully drawn
                if (currentTick >= 23) {
                    double tremble = Math.sin(currentTick * 2.3) * 0.008;
                    rightHand = rightHand.add(upDir.multiply(tremble)).add(rightDir.multiply(tremble * 0.5));
                }

                Vec3d arrowDir = leftHand.subtract(rightHand).normalize();

                // Formation particles
                if (currentTick <= 5) {
                    serverWorld.spawnParticles(ParticleTypes.FLAME, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, startPos.x, startPos.y, startPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                } else {
                    int segments = Math.max(4, (int) (14 * drawProgress));
                    for (int s = 0; s <= segments; s++) {
                        double frac = (double) s / segments;
                        Vec3d pt = rightHand.lerp(leftHand, frac);
                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);

                        if (s % 3 == 0 || currentTick >= 23) {
                            serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);
                        }
                    }

                    Vec3d tipPos = leftHand.add(arrowDir.multiply(0.24));
                    Vec3d barbBase = leftHand.add(arrowDir.multiply(0.07));
                    Vec3d leftBarb = barbBase.add(rightDir.multiply(-0.07)).add(upDir.multiply(0.035));
                    Vec3d rightBarb = barbBase.add(rightDir.multiply(0.07)).add(upDir.multiply(-0.035));

                    serverWorld.spawnParticles(ParticleTypes.FLAME, tipPos.x, tipPos.y, tipPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, leftBarb.x, leftBarb.y, leftBarb.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, rightBarb.x, rightBarb.y, rightBarb.z, 0, 0, 0, 0, 0);

                    if (currentTick >= 20) {
                        serverWorld.spawnParticles(ParticleTypes.CRIT, tipPos.x, tipPos.y, tipPos.z, 1, 0.02, 0.02, 0.02, 0.01);
                    }

                    if (drawProgress > 0.15) {
                        double limbExt = 0.22 + 0.20 * drawProgress;
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

                if (currentTick % 7 == 0) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 0.9f, 1.1f + (currentTick * 0.02f));
                }
                if (currentTick == 22) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.7f);
                }
                if (currentTick == 26) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 0.9f, 1.3f);
                }
            });
        }

        // 4. Launch at tick 30 (1.5s)!
        DelayedServerEffects.delay(chargeDurationTicks, () -> {
            if (!caster.isAlive()) return;
            launchFlameArrow(serverWorld, caster, effectivePotency, singleTargetOnly);
        });
    }

    public void launchFlameArrow(ServerWorld serverWorld, LivingEntity caster, float potency, boolean singleTargetOnly) {
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

        // 2. Custom fiery shockwave forward
        Vec3d muzzlePos = eyePos.add(lookDir.multiply(1.02)).add(rightDir.multiply(-0.28)).add(upDir.multiply(-0.18));
        serverWorld.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 1, 0, 0, 0, 0);
        serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 12, lookDir.x * 0.3, lookDir.y * 0.3, lookDir.z * 0.3, 0.15);
        serverWorld.spawnParticles(ParticleTypes.FLAME, muzzlePos.x, muzzlePos.y, muzzlePos.z, 10, lookDir.x * 0.4, lookDir.y * 0.4, lookDir.z * 0.4, 0.25);

        // 3. Spawn the Flame Arrow projectile directly along the sightline
        Vec3d arrowSpawnPos = eyePos.add(lookDir.multiply(0.2));
        FlameArrowEntity arrow = new FlameArrowEntity(ModEntities.FLAME_ARROW, serverWorld, caster, potency);
        arrow.setPosition(arrowSpawnPos.x, arrowSpawnPos.y, arrowSpawnPos.z);
        arrow.setVelocity(lookDir.multiply(2.1));
        arrow.setSingleTargetOnly(singleTargetOnly);
        serverWorld.spawnEntity(arrow);
    }

    /**
     * Sukuna's Kai Technique (Dismantle)
     * High-velocity razor-sharp flying crescent shockwave that cleaves through targets and the environment.
     */
    public void fireDismantle(World world, LivingEntity caster, ItemStack staff, float potency) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        caster.swingHand(Hand.MAIN_HAND, true);

        if (potency >= 2.5F) {
            int slashCount = 4;
            fireSingleDismantle(serverWorld, caster, staff, potency, 0, slashCount); // Horizontal (-)
            DelayedServerEffects.delay(2, () -> {
                if (caster.isAlive()) fireSingleDismantle(serverWorld, caster, staff, potency, 1, slashCount); // Vertical (|)
            });
            DelayedServerEffects.delay(4, () -> {
                if (caster.isAlive()) fireSingleDismantle(serverWorld, caster, staff, potency, 2, slashCount); // Diagonal (/)
            });
            DelayedServerEffects.delay(6, () -> {
                if (caster.isAlive()) fireSingleDismantle(serverWorld, caster, staff, potency, 3, slashCount); // Diagonal (\)
            });
        } else if (potency >= 1.4F) {
            int slashCount = 2;
            fireSingleDismantle(serverWorld, caster, staff, potency, 2, slashCount); // Diagonal (/)
            DelayedServerEffects.delay(2, () -> {
                if (caster.isAlive()) fireSingleDismantle(serverWorld, caster, staff, potency, 3, slashCount); // Diagonal (\)
            });
        } else {
            int orientation = caster.getRandom().nextInt(4);
            fireSingleDismantle(serverWorld, caster, staff, potency, orientation, 1);
        }
    }

    public void fireSingleDismantle(ServerWorld serverWorld, LivingEntity caster, ItemStack staff, float potency, int orientation, int slashCount) {
        Vec3d eyePos = caster.getEyePos();
        Vec3d lookDir = caster.getRotationVec(1.0F).normalize();

        Vec3d globalUp = new Vec3d(0, 1, 0);
        Vec3d rightDir = Math.abs(lookDir.y) > 0.99 ? new Vec3d(1, 0, 0) : lookDir.crossProduct(globalUp).normalize();
        Vec3d upDir = rightDir.crossProduct(lookDir).normalize();

        Vec3d slashAxis = switch (orientation) {
            case 0 -> rightDir; // Horizontal (-)
            case 1 -> upDir;    // Vertical (|)
            case 2 -> rightDir.add(upDir).normalize(); // Diagonal (/)
            case 3 -> rightDir.subtract(upDir).normalize(); // Diagonal (\)
            default -> rightDir;
        };

        float slashWidth = 5.5f * Math.min(potency, 2.2f);
        float maxDistance = 22.0f + (5.0f * potency);
        float baseDamage = 18.0f;
        float damageMultiplier = potency;
        if (staff.getItem() instanceof StaffItem staffItem) {
            damageMultiplier *= staffItem.getPowerMultiplier();
        }
        float trueDamage = (baseDamage * damageMultiplier) / slashCount;
        int penetrationDepth = Math.max(2, (int) (4 * potency));

        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.8f, 1.3f + (caster.getRandom().nextFloat() * 0.2f));
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.4f, 1.85f);
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.2f, 1.45f);

        Vec3d muzzlePos = eyePos.add(lookDir.multiply(0.8));
        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, muzzlePos.x, muzzlePos.y, muzzlePos.z, 1, 0, 0, 0, 0);
        serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, muzzlePos.x, muzzlePos.y, muzzlePos.z, 4, 0.1, 0.1, 0.1, 0.05);

        double stepDistance = 4.0;
        int totalSteps = (int) Math.ceil(maxDistance / stepDistance);

        Set<Entity> hitEntities = new HashSet<>();
        Set<BlockPos> brokenBlocks = new HashSet<>();
        boolean[] barrierBlocked = new boolean[]{false};

        for (int step = 0; step < totalSteps; step++) {
            final int currentStep = step;
            double startD = currentStep * stepDistance;
            double endD = Math.min(maxDistance, (currentStep + 1) * stepDistance);

            if (currentStep == 0) {
                executeSlashStep(serverWorld, caster, eyePos, lookDir, slashAxis, slashWidth, startD, endD,
                        trueDamage, penetrationDepth, hitEntities, brokenBlocks, barrierBlocked);
            } else {
                DelayedServerEffects.delay(currentStep, () -> {
                    if (!caster.isAlive() || barrierBlocked[0]) return;
                    executeSlashStep(serverWorld, caster, eyePos, lookDir, slashAxis, slashWidth, startD, endD,
                            trueDamage, penetrationDepth, hitEntities, brokenBlocks, barrierBlocked);
                });
            }
        }
    }

    private void executeSlashStep(ServerWorld serverWorld, LivingEntity caster, Vec3d eyePos, Vec3d lookDir,
                                  Vec3d slashAxis, float slashWidth, double startD, double endD,
                                  float trueDamage, int penetrationDepth,
                                  Set<Entity> hitEntities, Set<BlockPos> brokenBlocks, boolean[] barrierBlocked) {
        if (barrierBlocked[0]) return;

        double midD = (startD + endD) * 0.5;
        Vec3d centerPos = eyePos.add(lookDir.multiply(midD));

        serverWorld.playSound(null, centerPos.x, centerPos.y, centerPos.z,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.4f + (float) (midD * 0.015));

        if (serverWorld.getRandom().nextFloat() < 0.55f) {
            serverWorld.playSound(null, centerPos.x, centerPos.y, centerPos.z,
                    SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.7f, 1.95f);
        }

        int sampleCount = Math.max(16, (int) (slashWidth * 3.5));
        double curveDepth = 1.35;

        for (int i = 0; i <= sampleCount; i++) {
            double t = ((double) i / sampleCount - 0.5) * slashWidth;
            double normT = (2.0 * t) / slashWidth;
            double forwardCurve = (1.0 - (normT * normT)) * curveDepth;

            Vec3d bladePoint = centerPos.add(slashAxis.multiply(t)).add(lookDir.multiply(forwardCurve));

            serverWorld.spawnParticles(ParticleTypes.CRIT, bladePoint.x, bladePoint.y, bladePoint.z, 1, 0, 0, 0, 0);

            if (i % 2 == 0) {
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, bladePoint.x, bladePoint.y, bladePoint.z, 1, 0.02, 0.02, 0.02, 0.01);
            }

            if (i % 4 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, bladePoint.x, bladePoint.y, bladePoint.z, 1, 0, 0, 0, 0);
            }

            if (i % 3 == 0) {
                Vec3d wakePos = bladePoint.subtract(lookDir.multiply(0.45));
                serverWorld.spawnParticles(ParticleTypes.WHITE_SMOKE, wakePos.x, wakePos.y, wakePos.z, 1,
                        -lookDir.x * 0.06, -lookDir.y * 0.06, -lookDir.z * 0.06, 0.02);
            }
        }

        int rayCount = Math.max(12, (int) (slashWidth * 3.0));
        int blocksBrokenThisStep = 0;

        for (int r = 0; r <= rayCount; r++) {
            if (barrierBlocked[0]) break;

            double offset = ((double) r / rayCount - 0.5) * slashWidth;
            double normR = (2.0 * offset) / slashWidth;
            double forwardCurve = (1.0 - (normR * normR)) * curveDepth;

            Vec3d rayStart = eyePos.add(slashAxis.multiply(offset)).add(lookDir.multiply(forwardCurve));
            int hitObjects = 0;

            for (double d = startD; d <= endD; d += 0.35) {
                Vec3d currentPos = rayStart.add(lookDir.multiply(d));

                boolean hitBarrier = false;
                for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
                    boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);
                    if (!isSphere && barrier.getOwnerUuid().equals(caster.getUuid())) {
                        continue;
                    }

                    if (isSphere) {
                        double dist = currentPos.distanceTo(barrier.getPosition());
                        if (dist <= barrier.getRadius() + 0.5 && dist >= barrier.getRadius() - 0.5) {
                            hitBarrier = true;
                            break;
                        }
                    } else {
                        Box hexBox = Box.from(barrier.getPosition()).expand(0.8F);
                        if (hexBox.contains(currentPos)) {
                            hitBarrier = true;
                            break;
                        }
                    }
                }

                if (hitBarrier) {
                    serverWorld.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.2F, 1.5F);
                    serverWorld.spawnParticles(ParticleTypes.CRIT, currentPos.x, currentPos.y, currentPos.z, 4, 0.1, 0.1, 0.1, 0.1);
                    serverWorld.spawnParticles(ModParticles.DISMANTLE_B_PARTICLE, currentPos.x, currentPos.y, currentPos.z, 1, 0, 0, 0, 0);
                    barrierBlocked[0] = true;
                    break;
                }

                Box checkBox = new Box(currentPos.x - 0.45, currentPos.y - 0.45, currentPos.z - 0.45,
                        currentPos.x + 0.45, currentPos.y + 0.45, currentPos.z + 0.45);
                List<Entity> entitiesNear = serverWorld.getOtherEntities(caster, checkBox);

                for (Entity entity : entitiesNear) {
                    if (entity instanceof DomainShrineEntity) continue;

                    if (entity instanceof LivingEntity target && !hitEntities.contains(target)) {
                        hitEntities.add(target);

                        float multiplier = (hitObjects == 0) ? 1.0f : ((hitObjects == 1) ? 0.7f : 0.5f);
                        float finalDamage = trueDamage * multiplier;

                        target.damage(serverWorld, ModDamageTypes.of(serverWorld, ModDamageTypes.CUTTING_MAGIC_SHALLOW, caster), finalDamage);

                        Vec3d targetCenter = target.getBoundingBox().getCenter();
                        serverWorld.spawnParticles(ModParticles.DISMANTLE_A_PARTICLE, targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.2, 0.2, 0.2, 0);
                        serverWorld.spawnParticles(ModParticles.DISMANTLE_B_PARTICLE, targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.2, 0.2, 0.2, 0);
                        serverWorld.spawnParticles(ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE, targetCenter.x, targetCenter.y, targetCenter.z, 1, 0, 0, 0, 0);
                        serverWorld.spawnParticles(ParticleTypes.CRIT, targetCenter.x, targetCenter.y, targetCenter.z, 12, 0.25, 0.25, 0.25, 0.15);
                        serverWorld.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, targetCenter.x, targetCenter.y, targetCenter.z, 5, 0.2, 0.2, 0.2, 0.1);

                        serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                                ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.4f, 1.15f + caster.getRandom().nextFloat() * 0.25f);
                        serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 1.2f);

                        hitObjects++;
                    }
                }

                if (hitObjects >= penetrationDepth) break;

                BlockPos bPos = BlockPos.ofFloored(currentPos);
                if (!brokenBlocks.contains(bPos)) {
                    BlockState state = serverWorld.getBlockState(bPos);
                    if (!state.isAir() && state.getFluidState().isEmpty() && state.getHardness(serverWorld, bPos) >= 0.0F) {
                        brokenBlocks.add(bPos);
                        // Do NOT destroy blocks if MobGriefing is disabled
                        if (serverWorld.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                            serverWorld.setBlockState(bPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }

                        serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                                currentPos.x, currentPos.y, currentPos.z, 10, 0.25, 0.25, 0.25, 0.12);
                        serverWorld.spawnParticles(ParticleTypes.POOF, currentPos.x, currentPos.y, currentPos.z, 2, 0.2, 0.2, 0.2, 0.04);
                        serverWorld.spawnParticles(ParticleTypes.CRIT, currentPos.x, currentPos.y, currentPos.z, 2, 0.15, 0.15, 0.15, 0.05);

                        blocksBrokenThisStep++;
                        hitObjects++;
                    }
                }

                if (hitObjects >= penetrationDepth) break;
            }
        }

        if (blocksBrokenThisStep > 0) {
            serverWorld.playSound(null, centerPos.x, centerPos.y, centerPos.z,
                    SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.1f, 1.25f);
        }
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
        return 80.0F;
    }

    @Override
    public InnateHudData getRightInnateHudData(PlayerEntity player, SpellComponent component) {
        boolean hasFurnaceVow = BindingVowManager.hasVow(player, ModBindingVows.FURNACE);
        int meter = component.getMeter(this);

        if (hasFurnaceVow) {
            ActiveBarrier activeBarrier = BarrierManager.getActiveDomainBarrier(player.getUuid());
            if (activeBarrier != null && activeBarrier.getDomainExpansion() instanceof DomainExpansionShrine) {
                int remaining = activeBarrier.getMaxTicks() - activeBarrier.getCurrentTicks();
                if (remaining <= 160 && remaining > 0) {
                    if (remaining > 135) {
                        return new InnateHudData("Furnace: Open", 0xFFFF2200);
                    } else {
                        int secondsLeft = (remaining + 19) / 20;
                        int color = meter >= 100 ? 0xFFFF2200 : 0xFFFF8800;
                        return new InnateHudData("Furnace: " + meter + "% (" + secondsLeft + "s)", color);
                    }
                }
            }
            int color = meter >= 100 ? 0xFFFF5500 : 0xFFFF8800;
            return new InnateHudData("Furnace: " + meter + "%", color);
        }

        int color = meter >= 100 ? 0xFFFF5500 : 0xFFFF8800;
        return new InnateHudData("Open: " + meter + "%", color);
    }
}
