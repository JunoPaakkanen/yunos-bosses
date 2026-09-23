package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.animation.ModAnimations;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DomainExpansionShrine extends DomainExpansion {

    public static final Identifier SHRINE_TEXTURE = Identifier.of("yunosbosses", "textures/domain_expansion/shrine.png");

    public DomainExpansionShrine(Identifier id, SpellRarity rarity) {
        super(id, castAnimation, rarity);
    }

    public static final Identifier castAnimation = ModAnimations.DOMAIN_EXPANSION_SHRINE_ANIM;

    @Override
    public boolean isOpenBarrier() {
        return true;
    }

    @Override
    public Identifier getBarrierTexture() {
        return SHRINE_TEXTURE;
    }

    public float getRadius(int chargeLevel, boolean isOpen) {
        if (isOpen) {
            return switch (chargeLevel) {
                case 2 -> 40.0F;
                case 3 -> 65.0F;
                default -> 25.0F;
            };
        } else {
            return switch (chargeLevel) {
                case 2 -> 24.0F;
                case 3 -> 32.0F;
                default -> 16.0F;
            };
        }
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        this.cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        if (component.hasAltCastWindow(this)) {
            altCast(world, caster, staff, chargeLevel);
        } else {
            defaultCast(world, caster, staff, chargeLevel);
        }
    }

    public void defaultCast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        final boolean isOpenBarrier = caster.isSneaking() && component.unlockedOpenDomain();

        if (chargeLevel == 3) {
            caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    ModSounds.DOMAIN_EXPANSION_SHRINE_2, SoundCategory.NEUTRAL, 1.2f, 1.0f);
        } else {
            caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    ModSounds.DOMAIN_EXPANSION_SHRINE_1, SoundCategory.NEUTRAL, 1.2f, 1.0f);
        }

        for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, caster.getPos(), 64)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 110, 1));
        }

        startDomainExpansionCast(world, caster, "Malevolent Shrine", isOpenBarrier);

        float dynamicRadius = getRadius(chargeLevel, isOpenBarrier);

        DelayedServerEffects.delay(80, () -> finishDomainExpansionCast(world, caster, staff, dynamicRadius, isOpenBarrier));
    }

    public void altCast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        // Locate the actual DomainShrineEntity within the domain
        DomainShrineEntity targetShrine = null;
        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            if (barrier.getOwnerUuid().equals(caster.getUuid()) && barrier.getDomainExpansion() == this) {
                Box searchBox = Box.from(barrier.getPosition()).expand(barrier.getRadius());
                List<DomainShrineEntity> shrines = world.getEntitiesByClass(
                        DomainShrineEntity.class,
                        searchBox,
                        Entity::isAlive
                );
                if (!shrines.isEmpty()) {
                    targetShrine = shrines.getFirst();
                    break;
                }
            }
        }

        // Teleport caster on top of the shrine
        if (targetShrine != null) {
            // Shrine entity height is 6.5 blocks; teleport centered right onto the roof
            caster.requestTeleport(targetShrine.getX(), targetShrine.getY() + 6.6, targetShrine.getZ());
            caster.fallDistance = 0.0F;
            caster.setVelocity(Vec3d.ZERO);
            caster.velocityModified = true;
            world.playSound(null, targetShrine.getX(), targetShrine.getY() + 6.6, targetShrine.getZ(),
                    SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.2F);
        }

        // End alt cast window
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        component.clearAltCastWindow(this);
    }

    public static void triggerKaminoFinisher(World world, LivingEntity caster, ItemStack staff, int chargeLevel, ActiveBarrier barrier) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.FUGA, SoundCategory.PLAYERS, 2.5f, 1.0f);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.8f, 0.75f);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_BLASTFURNACE_FIRE_CRACKLE, SoundCategory.PLAYERS, 2.0f, 0.85f);

        float potency = switch (chargeLevel) {
            case 2 -> 1.5F;
            case 3 -> 2.5F;
            default -> 1.0F;
        };
        if (staff.getItem() instanceof StaffItem staffItem) {
            potency *= staffItem.getPowerMultiplier();
        }
        final float finalPotency = potency;

        Identifier animId = ModAnimations.FLAME_ARROW_ANIM;
        if (caster instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PlayerAnimationPayload(caster.getUuid(), animId));
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, caster.getPos(), 96)) {
            if (player != caster) {
                ServerPlayNetworking.send(player, new PlayerAnimationPayload(caster.getUuid(), animId));
            }
        }

        int chargeDurationTicks = 30;
        float domainRadius = barrier.getRadius();

        for (int t = 1; t <= chargeDurationTicks; t++) {
            final int currentTick = t;
            DelayedServerEffects.delay(t, () -> {
                if (!caster.isAlive()) return;

                Vec3d eyePos = caster.getEyePos();
                Vec3d lookDir = caster.getRotationVec(1.0F);

                Vec3d globalUp = new Vec3d(0, 1, 0);
                Vec3d rightDir = Math.abs(lookDir.y) > 0.99 ? new Vec3d(1, 0, 0) : lookDir.crossProduct(globalUp).normalize();
                Vec3d upDir = rightDir.crossProduct(lookDir).normalize();

                double pushOut = Math.min(1.0, (double) currentTick / 22.0) * 0.14;
                Vec3d leftHand = eyePos.add(lookDir.multiply(0.88 + pushOut))
                        .add(rightDir.multiply(-0.28))
                        .add(upDir.multiply(-0.18));

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
                if (currentTick >= 23) {
                    double tremble = Math.sin(currentTick * 2.3) * 0.010;
                    rightHand = rightHand.add(upDir.multiply(tremble)).add(rightDir.multiply(tremble * 0.5));
                }

                Vec3d arrowDir = leftHand.subtract(rightHand).normalize();

                if (currentTick <= 5) {
                    serverWorld.spawnParticles(ParticleTypes.FLAME, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, startPos.x, startPos.y, startPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, leftHand.x, leftHand.y, leftHand.z, 0, 0, 0, 0, 0);
                } else {
                    int segments = Math.max(5, (int) (16 * drawProgress));
                    for (int s = 0; s <= segments; s++) {
                        double frac = (double) s / segments;
                        Vec3d pt = rightHand.lerp(leftHand, frac);
                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);
                        if (s % 2 == 0 || currentTick >= 20) {
                            serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, pt.x, pt.y, pt.z, 0, 0, 0, 0, 0);
                        }
                    }

                    Vec3d tipPos = leftHand.add(arrowDir.multiply(0.28));
                    Vec3d barbBase = leftHand.add(arrowDir.multiply(0.08));
                    Vec3d leftBarb = barbBase.add(rightDir.multiply(-0.08)).add(upDir.multiply(0.04));
                    Vec3d rightBarb = barbBase.add(rightDir.multiply(0.08)).add(upDir.multiply(-0.04));

                    serverWorld.spawnParticles(ParticleTypes.FLAME, tipPos.x, tipPos.y, tipPos.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, leftBarb.x, leftBarb.y, leftBarb.z, 0, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, rightBarb.x, rightBarb.y, rightBarb.z, 0, 0, 0, 0, 0);

                    if (currentTick >= 18) {
                        serverWorld.spawnParticles(ParticleTypes.CRIT, tipPos.x, tipPos.y, tipPos.z, 2, 0.03, 0.03, 0.03, 0.02);
                    }

                    if (drawProgress > 0.15) {
                        double limbExt = 0.26 + 0.24 * drawProgress;
                        Vec3d upperLimb = leftHand.add(upDir.multiply(limbExt)).add(rightDir.multiply(-0.06));
                        Vec3d lowerLimb = leftHand.add(upDir.multiply(-limbExt)).add(rightDir.multiply(-0.06));

                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, upperLimb.x, upperLimb.y, upperLimb.z, 0, 0, 0, 0, 0);
                        serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, lowerLimb.x, lowerLimb.y, lowerLimb.z, 0, 0, 0, 0, 0);

                        for (int i = 1; i < 6; i++) {
                            double f = (double) i / 6.0;
                            Vec3d pUpper = upperLimb.lerp(rightHand, f);
                            Vec3d pLower = lowerLimb.lerp(rightHand, f);
                            serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pUpper.x, pUpper.y, pUpper.z, 0, 0, 0, 0, 0);
                            serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pLower.x, pLower.y, pLower.z, 0, 0, 0, 0, 0);
                        }
                    }
                }

                Vec3d bowCenter = rightHand.lerp(leftHand, 0.5);
                for (int p = 0; p < 8; p++) {
                    double pAngle = caster.getRandom().nextDouble() * Math.PI * 2.0;
                    double pDist = 3.0 + caster.getRandom().nextDouble() * 10.0;
                    double pY = (caster.getRandom().nextDouble() - 0.5) * 4.0;
                    Vec3d fuelOrigin = bowCenter.add(Math.cos(pAngle) * pDist, pY, Math.sin(pAngle) * pDist);
                    Vec3d suckVelocity = bowCenter.subtract(fuelOrigin).normalize().multiply(0.45);

                    if (p % 2 == 0) {
                        serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE,
                                fuelOrigin.x, fuelOrigin.y, fuelOrigin.z, 0,
                                suckVelocity.x, suckVelocity.y, suckVelocity.z, 0.35);
                    } else {
                        serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                                fuelOrigin.x, fuelOrigin.y, fuelOrigin.z, 0,
                                suckVelocity.x, suckVelocity.y, suckVelocity.z, 0.25);
                    }
                }

                if (currentTick % 6 == 0) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 1.0f, 1.0f + (currentTick * 0.02f));
                }
                if (currentTick == 22) {
                    world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2f, 1.8f);
                }
            });
        }

        DelayedServerEffects.delay(chargeDurationTicks, () -> {
            if (!caster.isAlive()) return;

            Vec3d eyePos = caster.getEyePos();
            Vec3d lookDir = caster.getRotationVec(1.0F).normalize();

            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 3.0f, 1.4f);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 2.5f, 0.5f);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 3.0f, 1.0f);

            Vec3d muzzlePos = eyePos.add(lookDir.multiply(1.0));
            serverWorld.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 2, 0, 0, 0, 0);
            serverWorld.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 16, lookDir.x * 0.4, lookDir.y * 0.4, lookDir.z * 0.4, 0.2);

            Vec3d arrowSpawnPos = eyePos.add(lookDir.multiply(0.25));
            FlameArrowEntity arrow = new FlameArrowEntity(ModEntities.FLAME_ARROW, serverWorld, caster, finalPotency);
            arrow.setPosition(arrowSpawnPos.x, arrowSpawnPos.y, arrowSpawnPos.z);
            arrow.setVelocity(lookDir.multiply(2.3));
            arrow.setThermobaricFinisher(domainRadius);
            serverWorld.spawnEntity(arrow);
        });
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.domain_expansion_shrine");
    }

    @Override
    public boolean canBeCharged() {
        return true;
    }

    @Override
    public int getLifetimeTicks() {
        return 960;
    }

    @Override
    public float getRadius() {
        return 25.0F;
    }

    @Override
    public void onDomainEffect(Entity affectedEntity, ActiveBarrier barrier) {
        if (affectedEntity instanceof LivingEntity) {
            if (affectedEntity.age % 4 == 0) {
                ServerWorld serverWorld = (ServerWorld) affectedEntity.getWorld();
                Entity caster = serverWorld.getEntity(barrier.getOwnerUuid());

                Vec3d center = barrier.getPosition();
                float radius = barrier.getRadius();
                double distToCenter = affectedEntity.getPos().distanceTo(center);

                float damage;
                if (barrier.isOpenBarrier()) {
                    double distRatio = Math.max(0.0, Math.min(1.0, distToCenter / radius));
                    damage = (float) (3.4f - (distRatio * 2.2f));

                    if (distToCenter > 2.0 && distToCenter < radius) {
                        Vec3d inward = center.subtract(affectedEntity.getPos()).normalize().multiply(0.018);
                        affectedEntity.setVelocity(affectedEntity.getVelocity().add(inward.x, 0, inward.z));
                        affectedEntity.velocityModified = true;
                    }
                } else {
                    damage = 1.8f;
                }

                float pitch = 1.3f + (affectedEntity.getRandom().nextFloat() * 0.5f);
                affectedEntity.getWorld().playSound(null, affectedEntity.getX(), affectedEntity.getY(), affectedEntity.getZ(),
                        ModSounds.REELSEIDEN_HIT, SoundCategory.NEUTRAL, 1.0f, pitch);

                DamageSource source = ModDamageTypes.of(affectedEntity.getWorld(), ModDamageTypes.CUTTING_MAGIC_SHALLOW, caster);
                Vec3d originalVelocity = affectedEntity.getVelocity();

                affectedEntity.damage((ServerWorld) affectedEntity.getWorld(), source, damage);

                affectedEntity.setVelocity(originalVelocity);
                affectedEntity.velocityModified = true;

                Vec3d pos = affectedEntity.getBoundingBox().getCenter();
                serverWorld.spawnParticles(
                        ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                        pos.x, pos.y, pos.z,
                        2,
                        0.25, 0.25, 0.25,
                        0.0
                );

                SimpleParticleType[] particlePool = {
                        ModParticles.DISMANTLE_A_PARTICLE,
                        ModParticles.DISMANTLE_B_PARTICLE
                };
                int randomIndex = affectedEntity.getRandom().nextInt(particlePool.length);
                serverWorld.spawnParticles(
                        particlePool[randomIndex],
                        pos.x, pos.y, pos.z,
                        2,
                        0.35, 0.35, 0.35,
                        0.08
                );

                serverWorld.spawnParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        pos.x, pos.y, pos.z,
                        1,
                        0.1, 0.1, 0.1,
                        0.0
                );
                serverWorld.spawnParticles(
                        ParticleTypes.CRIT,
                        pos.x, pos.y, pos.z,
                        3,
                        0.3, 0.3, 0.3,
                        0.15
                );
            }
        }
    }

    @Override
    public void onDomainCreated(ServerWorld world, LivingEntity caster, Vec3d barrierCenter) {
        Vec3d forwardVec = Vec3d.fromPolar(0.0F, caster.getYaw()).normalize();
        double spawnX = caster.getX() - (forwardVec.x * 1.5);
        double spawnY = caster.getY();
        double spawnZ = caster.getZ() - (forwardVec.z * 1.5);

        BlockPos shrineCenter = BlockPos.ofFloored(spawnX, spawnY, spawnZ);

        int clearRadius = 4;
        int clearHeight = 10;

        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int y = 0; y < clearHeight; y++) {
                for (int z = -clearRadius; z <= clearRadius; z++) {
                    if ((x * x) + (z * z) <= (clearRadius * clearRadius)) {
                        BlockPos targetPos = shrineCenter.add(x, y, z);
                        BlockState state = world.getBlockState(targetPos);

                        if (!state.isAir() && state.getHardness(world, targetPos) >= 0.0F) {
                            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }

        DomainShrineEntity shrine = new DomainShrineEntity(ModEntities.DOMAIN_SHRINE, world);
        shrine.refreshPositionAndAngles(spawnX, spawnY, spawnZ, caster.getYaw(), 0.0F);
        world.spawnEntity(shrine);

        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            double px = spawnX + Math.cos(rad) * 4.5;
            double pz = spawnZ + Math.sin(rad) * 4.5;
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, spawnY + 0.2, pz, 1, 0.1, 0.05, 0.1, 0.02);
            world.spawnParticles(ParticleTypes.POOF, px, spawnY + 0.2, pz, 2, 0.2, 0.1, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.WHITE_ASH, px, spawnY + 0.5, pz, 3, 0.3, 0.3, 0.3, 0.04);
            world.spawnParticles(ParticleTypes.ASH, px, spawnY + 0.5, pz, 2, 0.3, 0.3, 0.3, 0.04);
        }

        // Activate alt cast window for teleporting on top of the shrine
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        component.startAltCastWindow(this, 100);
    }

    @Override
    public void onDomainRemoved(ServerWorld world, ActiveBarrier barrier) {
        Box searchBox = Box.from(barrier.getPosition()).expand(barrier.getRadius());

        List<DomainShrineEntity> shrines = world.getEntitiesByClass(
                DomainShrineEntity.class,
                searchBox,
                entity -> true
        );

        for (DomainShrineEntity shrine : shrines) {
            shrine.discard();
        }
    }

    private static final Map<Integer, int[][]> COLUMN_OFFSETS_CACHE = new ConcurrentHashMap<>();

    private static int[][] getOrCreateColumnOffsets(int radius) {
        return COLUMN_OFFSETS_CACHE.computeIfAbsent(radius, r -> {
            List<int[]> raw = new ArrayList<>();
            int rSq = r * r;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= rSq) {
                        raw.add(new int[]{x, z});
                    }
                }
            }
            int n = raw.size();
            int stride = 719;
            while (gcd(stride, n) != 1) {
                stride += 2;
            }

            int[][] permuted = new int[2][n];
            for (int i = 0; i < n; i++) {
                int[] coord = raw.get((i * stride) % n);
                permuted[0][i] = coord[0];
                permuted[1][i] = coord[1];
            }
            return permuted;
        });
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private void spawnBlockPulverizeParticles(ServerWorld serverWorld, BlockPos targetPos, BlockState state,
                                              SimpleParticleType[] particlePool, Random rand) {
        serverWorld.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                3, 0.2, 0.2, 0.2, 0.1
        );

        if (rand.nextInt(3) == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.POOF,
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.02
            );
        }

        if (rand.nextInt(3) == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.WHITE_ASH,
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    2, 0.15, 0.15, 0.15, 0.02
            );
        }

        if (rand.nextInt(2) == 0) {
            SimpleParticleType particleType = particlePool[rand.nextInt(particlePool.length)];
            serverWorld.spawnParticles(
                    particleType,
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    1, 0.2, 0.2, 0.2, 0.05
            );
        }
    }

    @Override
    public void onGlobalTick(World world, ActiveBarrier barrier) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;

        int remainingTicks = barrier.getMaxTicks() - barrier.getCurrentTicks();
        if (remainingTicks <= 160 && !barrier.isFurnaceCueSent()) {
            barrier.setFurnaceCueSent(true);
            if (serverWorld.getPlayerByUuid(barrier.getOwnerUuid()) instanceof ServerPlayerEntity player) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2f, 1.4f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BLASTFURNACE_FIRE_CRACKLE, SoundCategory.PLAYERS, 1.5f, 1.0f);
            }
        }

        Vec3d center = barrier.getPosition();
        float radius = barrier.getRadius();
        Random rand = world.getRandom();
        double floorY = center.getY() - 3.0;

        SimpleParticleType[] particlePool = {
                ModParticles.DISMANTLE_A_PARTICLE,
                ModParticles.DISMANTLE_B_PARTICLE
        };

        int spawnCount = Math.max(16, (int) (radius * 0.75f));
        for (int i = 0; i < spawnCount; i++) {
            double u = rand.nextDouble();
            double v = rand.nextDouble();

            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);

            double r = rand.nextDouble() * (radius - 1.0);

            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.sin(phi) * Math.sin(theta);
            double dz = r * Math.cos(phi);

            Vec3d spawnPos = center.add(dx, dy, dz);

            if (spawnPos.getY() >= floorY + 0.5) {
                int randomIndex = rand.nextInt(particlePool.length);
                SimpleParticleType particleType = particlePool[randomIndex];

                double vx = (rand.nextDouble() - 0.5) * 0.4;
                double vy = (rand.nextDouble() - 0.5) * 0.2;
                double vz = (rand.nextDouble() - 0.5) * 0.4;

                serverWorld.spawnParticles(
                        particleType,
                        spawnPos.x, spawnPos.y, spawnPos.z,
                        1,
                        vx, vy, vz,
                        0.15
                );
            }
        }

        int groundParticleCount = 5;
        DustParticleEffect fissureGlowDust = new DustParticleEffect(0xFF202E, 0.8f);
        for (int i = 0; i < groundParticleCount; i++) {
            double angle = rand.nextDouble() * 2.0 * Math.PI;
            double dist = 2.0 + rand.nextDouble() * (radius * 0.75 - 2.0);
            double gx = center.x + Math.cos(angle) * dist;
            double gz = center.z + Math.sin(angle) * dist;

            serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    gx, floorY + 0.25, gz,
                    1,
                    0.03, 0.05, 0.03,
                    0.01
            );

            if (rand.nextInt(2) == 0) {
                serverWorld.spawnParticles(
                        ModParticles.FLAME_EMBER_PARTICLE,
                        gx, floorY + 0.25, gz,
                        1,
                        0.03, 0.08, 0.03,
                        0.02
                );
            }

            serverWorld.spawnParticles(
                    fissureGlowDust,
                    gx, floorY + 0.2, gz,
                    1,
                    0.04, 0.04, 0.04,
                    0.0
            );
        }

        int radiusInt = Math.round(radius);
        int[][] offsets = getOrCreateColumnOffsets(radiusInt);
        int[] offsetsX = offsets[0];
        int[] offsetsZ = offsets[1];
        int totalColumns = offsetsX.length;

        int cursor = barrier.getDomainBlockCursor();
        int columnsToInspect = Math.min(totalColumns, Math.max(120, (int) (radius * 6.0f)));
        int maxBlocksThisTick = 400;

        int blocksBrokenThisTick = 0;
        int stepsProcessed = 0;

        for (int step = 0; step < columnsToInspect; step++) {
            if (blocksBrokenThisTick >= maxBlocksThisTick) {
                break;
            }
            stepsProcessed++;

            int idx = (cursor + step) % totalColumns;
            int colX = (int) Math.floor(center.x + offsetsX[idx]);
            int colZ = (int) Math.floor(center.z + offsetsZ[idx]);

            if (!serverWorld.isChunkLoaded(colX >> 4, colZ >> 4)) continue;

            BlockPos topPos = serverWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(colX, 0, colZ));
            int currentY = topPos.getY() - 1;

            if (currentY <= floorY) continue;

            int heightDiff = currentY - (int) floorY;
            int sliceDepth = Math.min(heightDiff, Math.max(8, heightDiff / 3));
            int blocksSlicedInColumn = 0;

            while (currentY > floorY && blocksSlicedInColumn < sliceDepth) {
                BlockPos targetPos = new BlockPos(colX, currentY, colZ);
                BlockState state = serverWorld.getBlockState(targetPos);

                if (!state.getFluidState().isEmpty()) {
                    serverWorld.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    currentY--;
                    blocksSlicedInColumn++;
                    blocksBrokenThisTick++;
                    if (blocksBrokenThisTick >= maxBlocksThisTick) break;
                    continue;
                }

                if (!state.isAir()) {
                    if (state.getHardness(serverWorld, targetPos) >= 0.0F) {
                        serverWorld.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        blocksBrokenThisTick++;
                        blocksSlicedInColumn++;

                        if (blocksSlicedInColumn == 1 || rand.nextInt(4) == 0) {
                            spawnBlockPulverizeParticles(serverWorld, targetPos, state, particlePool, rand);
                        }

                        if (blocksBrokenThisTick >= maxBlocksThisTick) break;
                    }
                    currentY--;
                } else {
                    currentY--;
                }
            }
        }

        barrier.setDomainBlockCursor((cursor + stepsProcessed) % totalColumns);

        if (blocksBrokenThisTick > 0 && barrier.getCurrentTicks() % 6 == 0) {
            serverWorld.playSound(null, center.x, floorY, center.z,
                    SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.4f, 0.75f + (rand.nextFloat() * 0.3f));
            serverWorld.playSound(null, center.x, floorY, center.z,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 0.85f + (rand.nextFloat() * 0.3f));
        }
    }
}
