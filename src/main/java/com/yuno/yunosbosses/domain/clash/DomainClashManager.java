package com.yuno.yunosbosses.domain.clash;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.network.DomainClashEndPayload;
import com.yuno.yunosbosses.network.DomainClashProgressPayload;
import com.yuno.yunosbosses.network.DomainClashStartPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for Jujutsu Kaisen-style Domain Clashes.
 * Coordinates pending domain expansion casts, detects barrier overlaps,
 * orchestrates cinematic interactive clashes, manages technique burnout,
 * and resolves tug-of-war barrier struggles.
 */
public class DomainClashManager {

    public static final int CLASH_DURATION_TICKS = 100; // 5.0 seconds
    public static final int BURNOUT_DURATION_TICKS = 300; // 15 seconds of Technique Burnout on loss
    public static final float MASH_SCORE_PER_TAP = 3.6f;

    // Pending casts ticking down to domain expansion
    private static final List<PendingDomainCast> PENDING_CASTS = new ArrayList<>();

    // Active domain clashes
    private static final List<ActiveDomainClash> ACTIVE_CLASHES = new ArrayList<>();

    // Technique burnout tracker: UUID -> remaining ticks
    private static final Map<UUID, Integer> BURNOUT_MAP = new ConcurrentHashMap<>();

    // --- BURNOUT MANAGEMENT ---

    public static boolean hasBurnout(UUID uuid) {
        if (uuid == null) return false;
        Integer ticks = BURNOUT_MAP.get(uuid);
        return ticks != null && ticks > 0;
    }

    public static int getBurnoutSeconds(UUID uuid) {
        if (uuid == null) return 0;
        Integer ticks = BURNOUT_MAP.get(uuid);
        return ticks == null ? 0 : Math.max(1, (ticks + 19) / 20);
    }

    public static void applyBurnout(UUID uuid, int ticks) {
        BURNOUT_MAP.put(uuid, ticks);
    }

    // --- PENDING CAST CLASS ---

    public static class PendingDomainCast {
        public final UUID casterUuid;
        public final LivingEntity caster;
        public final DomainExpansion domainExpansion;
        public final ItemStack staff;
        public final float radius;
        public final boolean isOpenBarrier;
        public final String domainName;
        public final int chargeLevel;
        public int remainingCastTicks;
        public final Runnable finishAction;
        public final Vec3d castPos;
        public final ServerWorld world;
        public boolean inClash = false;

        public PendingDomainCast(LivingEntity caster, DomainExpansion domainExpansion, ItemStack staff,
                                 float radius, boolean isOpenBarrier, String domainName, int chargeLevel,
                                 int remainingCastTicks, Runnable finishAction) {
            this.casterUuid = caster.getUuid();
            this.caster = caster;
            this.domainExpansion = domainExpansion;
            this.staff = staff != null ? staff.copy() : ItemStack.EMPTY;
            this.radius = radius;
            this.isOpenBarrier = isOpenBarrier;
            this.domainName = domainName;
            this.chargeLevel = chargeLevel;
            this.remainingCastTicks = remainingCastTicks;
            this.finishAction = finishAction;
            this.castPos = caster.getPos().add(0, 2, 0);
            this.world = (ServerWorld) caster.getWorld();
        }
    }

    // --- CLASH PARTICIPANT ---

    public static class ClashParticipant {
        public final UUID uuid;
        public final LivingEntity entity;
        public final String name;
        public final String domainName;
        public final boolean isOpenBarrier;
        public final float radius;
        public final DomainExpansion domainExpansion;
        public final ItemStack staff;
        public final int chargeLevel;
        public float score;
        public int mashCount = 0;
        public final Runnable finishAction;
        public final ActiveBarrier existingBarrier;

        public ClashParticipant(LivingEntity entity, String domainName, boolean isOpenBarrier,
                                float radius, DomainExpansion domainExpansion, ItemStack staff,
                                int chargeLevel, Runnable finishAction, ActiveBarrier existingBarrier) {
            this.uuid = entity.getUuid();
            this.entity = entity;
            this.name = entity.getName().getString();
            this.domainName = domainName;
            this.isOpenBarrier = isOpenBarrier;
            this.radius = radius;
            this.domainExpansion = domainExpansion;
            this.staff = staff != null ? staff.copy() : ItemStack.EMPTY;
            this.chargeLevel = chargeLevel;
            this.finishAction = finishAction;
            this.existingBarrier = existingBarrier;
            this.score = calculateBaseScore();
        }

        private float calculateBaseScore() {
            float base = 100.0f;

            // Charge Level bonus
            switch (chargeLevel) {
                case 2 -> base += 25.0f;
                case 3 -> base += 55.0f;
                default -> {}
            }

            // Staff refinement multiplier
            if (staff.getItem() instanceof StaffItem staffItem) {
                base += (staffItem.getPowerMultiplier() - 1.0f) * 30.0f;
            }

            // Open Barrier Binding Vow refinement advantage (Sukuna's outside-attack advantage)
            if (isOpenBarrier) {
                base += 10.0f;
            }

            // Stamina / health factor
            float healthRatio = entity.getHealth() / Math.max(1.0f, entity.getMaxHealth());
            base += healthRatio * 20.0f;

            // Innate refinement for boss entities
            if (entity instanceof UbelEntity) {
                base += 10.0f;
            }

            return base;
        }
    }

    // --- ACTIVE CLASH SESSION ---

    public static class ActiveDomainClash {
        public final UUID clashId;
        public final ServerWorld world;
        public final Vec3d clashPos;
        public final ClashParticipant p1;
        public final ClashParticipant p2;
        public final int durationTicks;
        public int currentTicks = 0;
        public boolean resolved = false;

        public ActiveDomainClash(ServerWorld world, Vec3d clashPos, ClashParticipant p1, ClashParticipant p2, int durationTicks) {
            this.clashId = UUID.randomUUID();
            this.world = world;
            this.clashPos = clashPos;
            this.p1 = p1;
            this.p2 = p2;
            this.durationTicks = durationTicks;
        }

        public void tick() {
            this.currentTicks++;

            // Check if either entity is dead/removed
            if (!p1.entity.isAlive() && p2.entity.isAlive()) {
                resolve(p2, p1);
                return;
            }
            if (!p2.entity.isAlive() && p1.entity.isAlive()) {
                resolve(p1, p2);
                return;
            }

            // Apply focus stance to casters (slight slowness, resistance to prevent outside mob cheese)
            applyCasterFocus(p1.entity);
            applyCasterFocus(p2.entity);

            // AI mob clash pressure
            if (!(p1.entity instanceof PlayerEntity)) {
                applyMobPressure(p1);
            }
            if (!(p2.entity instanceof PlayerEntity)) {
                applyMobPressure(p2);
            }

            // In-world visual particles & sounds along clash boundary
            spawnClashVisuals();

            // Periodic sync of balance to clients (every 2 ticks)
            if (this.currentTicks % 2 == 0) {
                broadcastProgress();
            }

            // Climax resolution
            if (this.currentTicks >= this.durationTicks) {
                broadcastProgress();
                if (p1.score >= p2.score) {
                    resolve(p1, p2);
                } else {
                    resolve(p2, p1);
                }
            }
        }

        private void applyCasterFocus(LivingEntity entity) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 4, false, false, false));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 1, false, false, false));
            entity.fallDistance = 0.0f;
        }

        private void applyMobPressure(ClashParticipant participant) {
            // Boss mob pushes cursed energy into the barrier
            float pressure = 0.8f + participant.entity.getRandom().nextFloat() * 0.8f;
            if (participant.entity.getHealth() > participant.entity.getMaxHealth() * 0.5f) {
                pressure += 0.4f;
            }
            participant.score += pressure;
        }

        private void spawnClashVisuals() {
            Vec3d pos1 = p1.entity.getPos().add(0, 1.2, 0);
            Vec3d pos2 = p2.entity.getPos().add(0, 1.2, 0);
            Vec3d clashNormal = pos2.subtract(pos1);
            double dist = clashNormal.length();
            if (dist > 0.001) {
                clashNormal = clashNormal.normalize();
            } else {
                clashNormal = new Vec3d(0, 0, 1);
            }

            // Orthogonal vectors for the clash boundary disc
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = Math.abs(clashNormal.y) > 0.95 ? new Vec3d(1, 0, 0) : clashNormal.crossProduct(up).normalize();
            Vec3d perpUp = right.crossProduct(clashNormal).normalize();

            Random rand = world.getRandom();

            // Midpoint seam particles
            int sparkCount = 6;
            for (int i = 0; i < sparkCount; i++) {
                double rad = (rand.nextDouble() - 0.5) * 8.0;
                double radY = (rand.nextDouble() - 0.5) * 6.0;
                Vec3d pt = clashPos.add(right.multiply(rad)).add(perpUp.multiply(radY));

                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0.08);
                if (rand.nextBoolean()) {
                    world.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, pt.x, pt.y, pt.z, 1, 0.02, 0.04, 0.02, 0.03);
                }
                if (rand.nextInt(3) == 0) {
                    world.spawnParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0.04);
                }
                if (rand.nextInt(4) == 0) {
                    world.spawnParticles(ParticleTypes.WHITE_ASH, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.02);
                }
            }

            // Cutting blade streaks on the boundary
            if (this.currentTicks % 2 == 0) {
                SimpleParticleType[] cutPool = {
                        ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                        ModParticles.DISMANTLE_A_PARTICLE,
                        ModParticles.DISMANTLE_B_PARTICLE
                };
                SimpleParticleType cut = cutPool[rand.nextInt(cutPool.length)];
                double rDist = (rand.nextDouble() - 0.5) * 5.0;
                double rY = (rand.nextDouble() - 0.5) * 4.0;
                Vec3d cutPt = clashPos.add(right.multiply(rDist)).add(perpUp.multiply(rY));
                world.spawnParticles(cut, cutPt.x, cutPt.y, cutPt.z, 1, 0.1, 0.1, 0.1, 0.02);
            }

            // Pulsing shockwaves every 12 ticks
            if (this.currentTicks % 12 == 0) {
                world.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, clashPos.x, clashPos.y, clashPos.z, 1, 0, 0, 0, 0);
                world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.4f, 1.1f + rand.nextFloat() * 0.4f);
            }

            // Rhythmic barrier rumble sound
            if (this.currentTicks % 8 == 0) {
                world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                        SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.6f + rand.nextFloat() * 0.3f);
            }
        }

        public void broadcastProgress() {
            float total = p1.score + p2.score;
            float balance = total <= 0.01f ? 0.5f : (p1.score / total);
            balance = Math.max(0.05f, Math.min(0.95f, balance));

            DomainClashProgressPayload payload = new DomainClashProgressPayload(clashId, balance, p1.score, p2.score);
            for (ServerPlayerEntity player : PlayerLookup.around(world, clashPos, 96)) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        public void handleInput(UUID playerUuid) {
            ClashParticipant p = null;
            if (p1.uuid.equals(playerUuid)) p = p1;
            else if (p2.uuid.equals(playerUuid)) p = p2;

            if (p != null) {
                // Tap bonus
                p.score += MASH_SCORE_PER_TAP;
                p.mashCount++;

                // Visual spark feedback on caster
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        p.entity.getX(), p.entity.getY() + 1.2, p.entity.getZ(),
                        3, 0.2, 0.2, 0.2, 0.05);

                // Immediately sync balance change to players
                broadcastProgress();
            }
        }

        public void resolve(ClashParticipant winner, ClashParticipant loser) {
            if (resolved) return;
            resolved = true;

            // --- RESOLVE LOSER ---
            BlockPos loserPos = BlockPos.ofFloored(loser.entity.getPos());

            // 1. Violent barrier shatter sounds
            world.playSound(null, loserPos.getX(), loserPos.getY(), loserPos.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.5f, 0.8f);
            world.playSound(null, loserPos.getX(), loserPos.getY(), loserPos.getZ(),
                    ModSounds.FRAME_SHATTER, SoundCategory.PLAYERS, 2.0f, 1.0f);
            world.playSound(null, loserPos.getX(), loserPos.getY(), loserPos.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.8f, 1.3f);

            // 2. Glass shatter world event (2001) & particles
            world.syncWorldEvent(2001, loserPos, Block.getRawIdFromState(Blocks.TINTED_GLASS.getDefaultState()));
            world.spawnParticles(ModParticles.FRAME_SHATTER_PARTICLE,
                    loser.entity.getX(), loser.entity.getY() + 1.2, loser.entity.getZ(),
                    25, 0.6, 0.6, 0.6, 0.25);
            world.spawnParticles(ParticleTypes.CRIT,
                    loser.entity.getX(), loser.entity.getY() + 1.2, loser.entity.getZ(),
                    20, 0.5, 0.5, 0.5, 0.15);
            world.spawnParticles(ParticleTypes.EXPLOSION,
                    loser.entity.getX(), loser.entity.getY() + 1.2, loser.entity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.0);

            // 3. Recoil knockback away from clash midpoint
            Vec3d recoil = loser.entity.getPos().subtract(clashPos);
            if (recoil.lengthSquared() > 0.01) {
                recoil = recoil.normalize().multiply(1.8).add(0, 0.35, 0);
            } else {
                recoil = new Vec3d(0, 0.5, 1.5);
            }
            loser.entity.setVelocity(recoil);
            loser.entity.velocityModified = true;

            // 4. Technique Burnout & debuffs
            applyBurnout(loser.uuid, BURNOUT_DURATION_TICKS);
            loser.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1));
            loser.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));

            // Loser ActiveBarrier expired if any
            if (loser.existingBarrier != null) {
                loser.existingBarrier.expire();
            }

            // Loser player feedback
            if (loser.entity instanceof ServerPlayerEntity loserPlayer) {
                loserPlayer.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§c§lDOMAIN SHATTERED!§r")));
                loserPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§7Cursed Technique Burnout (15s)")));
                loserPlayer.sendMessage(Text.literal("§cYour domain barrier was violently overpowered and shattered!§r"), true);
            }

            // Loser mob boss stagger
            if (loser.entity instanceof UbelEntity ubel) {
                // Stagger boss AI
                ubel.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
            }

            // --- RESOLVE WINNER ---
            world.playSound(null, winner.entity.getX(), winner.entity.getY(), winner.entity.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 2.0f, 1.0f);
            world.playSound(null, winner.entity.getX(), winner.entity.getY(), winner.entity.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.1f);

            // Winner buffs: Absorption & Speed
            winner.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
            winner.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));

            // Winner player feedback
            if (winner.entity instanceof ServerPlayerEntity winnerPlayer) {
                winnerPlayer.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6§lDOMAIN EXPANDED!§r")));
                winnerPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§eOverpowered Opponent's Barrier!")));
                winnerPlayer.sendMessage(Text.literal("§6Your domain prevailed in the clash!§r"), true);
            }

            // If winner was a pending cast, complete domain expansion now!
            if (winner.finishAction != null) {
                winner.finishAction.run();
            }

            // If winner had an existing barrier, resume sure-hit
            if (winner.existingBarrier != null) {
                winner.existingBarrier.setClashing(false);
            }

            // Broadcast end payload to all players within 96 blocks
            DomainClashEndPayload endPayload = new DomainClashEndPayload(clashId, winner.uuid, loser.uuid, clashPos);
            for (ServerPlayerEntity player : PlayerLookup.around(world, clashPos, 96)) {
                ServerPlayNetworking.send(player, endPayload);
            }
        }
    }

    // --- REGISTRATION & LIFECYCLE ---

    /**
     * Registers a domain cast attempt.
     * Evaluates whether an overlapping pending cast or young active barrier exists.
     * If overlap found -> initiates Domain Clash!
     * If no overlap -> proceeds with normal solo cast!
     */
    public static boolean registerDomainCast(ServerWorld world, LivingEntity caster, DomainExpansion domainExpansion,
                                             ItemStack staff, float radius, boolean isOpenBarrier,
                                             String domainName, int chargeLevel, int castDelayTicks,
                                             Runnable finishAction) {
        UUID casterUuid = caster.getUuid();

        // 1. Burnout check
        if (hasBurnout(casterUuid)) {
            int remainingSec = getBurnoutSeconds(casterUuid);
            if (caster instanceof PlayerEntity player) {
                player.sendMessage(Text.literal("§cCannot expand domain! Technique Burnout (" + remainingSec + "s remaining)§r"), true);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 0.8f);
            }
            return false;
        }

        // 2. Check if already clashing
        if (isEntityClashing(casterUuid)) {
            return false;
        }

        Vec3d newCenter = caster.getPos().add(0, 2, 0);

        // 3. Search for overlapping pending domain cast (simultaneous cast)
        PendingDomainCast clashPartnerPending = null;
        for (PendingDomainCast pending : PENDING_CASTS) {
            if (pending.world.equals(world) && !pending.casterUuid.equals(casterUuid) && pending.caster.isAlive() && !pending.inClash) {
                double dist = newCenter.distanceTo(pending.castPos);
                if (dist <= (radius + pending.radius + 6.0)) {
                    clashPartnerPending = pending;
                    break;
                }
            }
        }

        if (clashPartnerPending != null) {
            // Clash between two pending casts!
            clashPartnerPending.inClash = true;

            ClashParticipant p1 = new ClashParticipant(
                    clashPartnerPending.caster,
                    clashPartnerPending.domainName,
                    clashPartnerPending.isOpenBarrier,
                    clashPartnerPending.radius,
                    clashPartnerPending.domainExpansion,
                    clashPartnerPending.staff,
                    clashPartnerPending.chargeLevel,
                    clashPartnerPending.finishAction,
                    null
            );

            ClashParticipant p2 = new ClashParticipant(
                    caster,
                    domainName,
                    isOpenBarrier,
                    radius,
                    domainExpansion,
                    staff,
                    chargeLevel,
                    finishAction,
                    null
            );

            startClash(world, p1, p2, clashPartnerPending.castPos.lerp(newCenter, 0.5));
            return true;
        }

        // 4. Search for overlapping ACTIVE barrier in the clash reaction window
        ActiveBarrier overlappingBarrier = null;
        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            if (!barrier.getOwnerUuid().equals(casterUuid) && barrier.getDomainExpansion() != null && !barrier.isExpired()) {
                double dist = newCenter.distanceTo(barrier.getPosition());
                if (dist <= (radius + barrier.getRadius() + 6.0)) {
                    overlappingBarrier = barrier;
                    break;
                }
            }
        }

        if (overlappingBarrier != null) {
            Entity owner = world.getEntity(overlappingBarrier.getOwnerUuid());
            if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive() && !isEntityClashing(livingOwner.getUuid())) {
                overlappingBarrier.setClashing(true);

                ItemStack ownerStaff = ItemStack.EMPTY;
                if (livingOwner.getMainHandStack().getItem() instanceof StaffItem) {
                    ownerStaff = livingOwner.getMainHandStack();
                } else if (livingOwner.getOffHandStack().getItem() instanceof StaffItem) {
                    ownerStaff = livingOwner.getOffHandStack();
                }

                ClashParticipant p1 = new ClashParticipant(
                        livingOwner,
                        overlappingBarrier.getDomainExpansion().getName().getString(),
                        overlappingBarrier.isOpenBarrier(),
                        overlappingBarrier.getRadius(),
                        overlappingBarrier.getDomainExpansion(),
                        ownerStaff,
                        2, // default charge level for existing
                        null,
                        overlappingBarrier
                );

                ClashParticipant p2 = new ClashParticipant(
                        caster,
                        domainName,
                        isOpenBarrier,
                        radius,
                        domainExpansion,
                        staff,
                        chargeLevel,
                        finishAction,
                        null
                );

                startClash(world, p1, p2, overlappingBarrier.getPosition().lerp(newCenter, 0.5));
                return true;
            }
        }

        // 5. No clash detected -> normal isolated cast!
        PendingDomainCast soloCast = new PendingDomainCast(
                caster, domainExpansion, staff, radius, isOpenBarrier,
                domainName, chargeLevel, castDelayTicks, finishAction
        );
        PENDING_CASTS.add(soloCast);
        return true;
    }

    private static void startClash(ServerWorld world, ClashParticipant p1, ClashParticipant p2, Vec3d clashPos) {
        ActiveDomainClash clash = new ActiveDomainClash(world, clashPos, p1, p2, CLASH_DURATION_TICKS);
        ACTIVE_CLASHES.add(clash);

        // Sound of barriers colliding
        world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 2.5f, 0.65f);
        world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 2.0f, 0.6f);
        world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 2.0f, 1.1f);
        world.playSound(null, clashPos.x, clashPos.y, clashPos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.8f, 1.3f);

        // Broadcast Clash Start Payload
        DomainClashStartPayload payload = new DomainClashStartPayload(
                clash.clashId,
                p1.uuid,
                p1.name,
                p1.domainName,
                p1.isOpenBarrier,
                p2.uuid,
                p2.name,
                p2.domainName,
                p2.isOpenBarrier,
                clashPos,
                CLASH_DURATION_TICKS
        );

        for (ServerPlayerEntity player : PlayerLookup.around(world, clashPos, 96)) {
            ServerPlayNetworking.send(player, payload);
        }

        // Broadcast initial progress immediately so client has true starting scores/balance
        clash.broadcastProgress();
    }

    public static boolean isEntityClashing(UUID uuid) {
        if (uuid == null) return false;
        for (ActiveDomainClash clash : ACTIVE_CLASHES) {
            if (!clash.resolved && (clash.p1.uuid.equals(uuid) || clash.p2.uuid.equals(uuid))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEntityCastingDomainNear(LivingEntity entity, double maxDist) {
        if (entity == null) return false;
        Vec3d pos = entity.getPos();
        double maxDistSq = maxDist * maxDist;

        for (PendingDomainCast pending : PENDING_CASTS) {
            if (!pending.casterUuid.equals(entity.getUuid()) && pending.caster.isAlive()) {
                if (pending.castPos.squaredDistanceTo(pos) <= maxDistSq) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void handlePlayerInput(ServerPlayerEntity player, UUID clashId) {
        for (ActiveDomainClash clash : ACTIVE_CLASHES) {
            if (clash.clashId.equals(clashId) && !clash.resolved) {
                clash.handleInput(player.getUuid());
                break;
            }
        }
    }

    // --- SERVER TICK LOOP ---

    public static void serverTick(MinecraftServer server) {
        // 1. Tick burnout map
        for (Iterator<Map.Entry<UUID, Integer>> it = BURNOUT_MAP.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        // 2. Tick active clashes
        for (int i = ACTIVE_CLASHES.size() - 1; i >= 0; i--) {
            ActiveDomainClash clash = ACTIVE_CLASHES.get(i);
            clash.tick();
            if (clash.resolved) {
                ACTIVE_CLASHES.remove(i);
            }
        }

        // 3. Tick pending solo casts
        for (int i = PENDING_CASTS.size() - 1; i >= 0; i--) {
            PendingDomainCast pending = PENDING_CASTS.get(i);

            // Clean up if caster died or disconnected
            if (!pending.caster.isAlive() || pending.caster.isRemoved()) {
                PENDING_CASTS.remove(i);
                continue;
            }

            // If swept into a clash, remove from pending casts
            if (pending.inClash) {
                PENDING_CASTS.remove(i);
                continue;
            }

            pending.remainingCastTicks--;
            if (pending.remainingCastTicks <= 0) {
                // Completed isolated cast!
                PENDING_CASTS.remove(i);
                pending.finishAction.run();
            }
        }
    }
}
