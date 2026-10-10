package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaFlankDashAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaFrameShatterAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaJabAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaMachRamAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaPalmStrikeAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaPunchBarrageAbility;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.DuoModeCheckHelper;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NaoyaAttackGoal extends AbstractBossAttackGoal {
    private final NaoyaEntity naoya;
    private final NaoyaFlankDashAbility flankDashAbility;
    private int evasionCooldown = 0;

    public NaoyaAttackGoal(NaoyaEntity naoya, double speed) {
        super(naoya, speed);
        this.naoya = naoya;

        // 1. COMBO FINISHER: Frame Shatter (when target has FRAME_FREEZE)
        this.registerAbility(new NaoyaFrameShatterAbility());

        // 2. Mach Supersonic Ram (Charge attack at high speed or long range)
        this.registerAbility(new NaoyaMachRamAbility());

        // 3. Advancing Sprint Punch Barrage (Flurry rush scaling with speed stacks)
        this.registerAbility(new NaoyaPunchBarrageAbility());

        // 4. 24-FPS Flanking Dash (Curved afterimages & surprise back strike)
        this.flankDashAbility = new NaoyaFlankDashAbility();
        this.registerAbility(this.flankDashAbility);

        // 5. Palm Strike (Freeze target with the 24-FPS Rule)
        this.registerAbility(new NaoyaPalmStrikeAbility());

        // 6. Close Quarters Melee Jab
        this.registerAbility(new NaoyaJabAbility());
    }

    @Override
    protected double getMovementSpeed() {
        return this.speed + (this.naoya.getSpeedStacks() * 0.04);
    }

    @Override
    public void tick() {
        if (this.evasionCooldown > 0) {
            this.evasionCooldown--;
        }
        super.tick();
    }

    @Override
    public void stop() {
        super.stop();
        this.naoya.setDashing(false);
    }

    @Override
    protected void performTeleport(Vec3d safePos) {
        World world = this.naoya.getWorld();
        // Spawn afterimage at old location
        ProjectionSorcery.broadcastFrameImage(world, this.naoya, this.naoya.getPos(), 30);

        super.performTeleport(safePos);
        this.naoya.fallDistance = 0.0F;

        if (world instanceof ServerWorld sw) {
            sw.playSound(null, safePos.x, safePos.y, safePos.z,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0F, 1.5F);
            sw.spawnParticles(ParticleTypes.PORTAL, safePos.x, safePos.y + 1.0, safePos.z,
                    15, 0.3, 0.5, 0.3, 0.1);
        }

        this.globalCooldown = 6;
    }

    public void triggerFlankDash() {
        if (this.target != null && this.target.isAlive() && this.activeAbility == null) {
            this.startAbility(this.flankDashAbility, 3);
        }
    }

    /**
     * Standard lateral side-step evasion for frontal attacks.
     */
    public void tryEvasiveStep(DamageSource source) {
        if (this.evasionCooldown > 0 || this.activeAbility != null) return;
        World world = this.naoya.getWorld();
        if (!(world instanceof ServerWorld sw)) return;

        // Choose random side: left or right
        Vec3d look = this.naoya.getRotationVec(1.0F);
        Vec3d side = new Vec3d(-look.z, 0, look.x).normalize();
        if (this.naoya.getRandom().nextBoolean()) {
            side = side.multiply(-1.0);
        }

        // Spawn afterimage at current position to act as decoy
        ProjectionSorcery.broadcastFrameImage(world, this.naoya, this.naoya.getPos(), 30);

        // Teleport 3.5 blocks to side
        Vec3d dodgePos = this.naoya.getPos().add(side.multiply(3.5));
        BlockPos floor = BlockPos.ofFloored(dodgePos);
        if (world.getBlockState(floor).isSolidBlock(world, floor)) {
            dodgePos = new Vec3d(dodgePos.x, floor.getY() + 1.0, dodgePos.z);
        }

        this.naoya.refreshPositionAndAngles(dodgePos.x, dodgePos.y, dodgePos.z, this.naoya.getYaw(), this.naoya.getPitch());
        sw.playSound(null, dodgePos.x, dodgePos.y, dodgePos.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0F, 1.8F);
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, dodgePos.x, dodgePos.y + 1.0, dodgePos.z, 8, 0.2, 0.4, 0.2, 0.1);

        this.evasionCooldown = 70; // 3.5 seconds
    }

    /**
     * Punishes players attempting to flank or sandwich Naoya, especially during combos.
     * Naoya leaves a Projection afterimage decoy and instantly flashes behind the rear attacker.
     * If the rear attacker and another player are lined up, Naoya unleashes a penetrating Shockwave Punch.
     * Otherwise, he delivers an immediate evasive back strike.
     */
    public boolean tryFlankCounter(DamageSource source, LivingEntity rearAttacker) {
        if (this.evasionCooldown > 0) return false;
        World world = this.naoya.getWorld();
        if (!(world instanceof ServerWorld sw)) return false;

        // 1. Interrupt active combo/ability cleanly
        if (this.activeAbility != null) {
            this.activeAbility.stop(this.naoya);
            this.activeAbility = null;
            this.attackTimer = 0;
        }

        // 2. Spawn 24-FPS afterimage decoy at current location
        ProjectionSorcery.broadcastFrameImage(world, this.naoya, this.naoya.getPos(), 40);
        sw.playSound(null, this.naoya.getX(), this.naoya.getY(), this.naoya.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.4F, 1.8F);
        sw.playSound(null, this.naoya.getX(), this.naoya.getY(), this.naoya.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.0F, 1.8F);
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.naoya.getX(), this.naoya.getY() + 1.0, this.naoya.getZ(),
                16, 0.3, 0.4, 0.3, 0.1);

        // 3. Calculate position behind the rear attacker
        Vec3d attLook = rearAttacker.getRotationVec(1.0F);
        Vec3d behindDir = new Vec3d(-attLook.x, 0, -attLook.z);
        if (behindDir.lengthSquared() < 1e-4) {
            behindDir = new Vec3d(this.naoya.getX() - rearAttacker.getX(), 0, this.naoya.getZ() - rearAttacker.getZ());
        }
        if (behindDir.lengthSquared() > 1e-4) {
            behindDir = behindDir.normalize();
        } else {
            behindDir = this.naoya.getRotationVec(1.0F).multiply(-1.0).normalize();
        }

        Vec3d behindPos = rearAttacker.getPos().add(behindDir.multiply(1.8));
        BlockPos targetBlock = BlockPos.ofFloored(behindPos);
        if (sw.getBlockState(targetBlock).isSolidBlock(sw, targetBlock)) {
            behindPos = new Vec3d(behindPos.x, targetBlock.getY() + 1.0, behindPos.z);
        } else if (sw.getBlockState(targetBlock.down()).isAir()) {
            BlockPos down2 = targetBlock.down(2);
            if (sw.getBlockState(down2).isSolidBlock(sw, down2)) {
                behindPos = new Vec3d(behindPos.x, targetBlock.getY() - 1.0, behindPos.z);
            }
        }

        // Reposition Naoya behind the attacker
        this.naoya.refreshPositionAndAngles(behindPos.x, behindPos.y, behindPos.z, this.naoya.getYaw(), this.naoya.getPitch());
        this.naoya.fallDistance = 0.0F;
        snapLook(this.naoya, rearAttacker);
        this.naoya.setTarget(rearAttacker);
        this.target = rearAttacker;
        this.naoya.addSpeedStacks(2);

        sw.playSound(null, behindPos.x, behindPos.y, behindPos.z,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.2F, 1.6F);
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, behindPos.x, behindPos.y + 1.0, behindPos.z,
                12, 0.2, 0.4, 0.2, 0.1);

        // 4. Direction towards rearAttacker from new position
        Vec3d vRear = new Vec3d(rearAttacker.getX() - this.naoya.getX(), 0, rearAttacker.getZ() - this.naoya.getZ());
        if (vRear.lengthSquared() > 1e-4) {
            vRear = vRear.normalize();
        } else {
            vRear = this.naoya.getRotationVec(1.0F);
        }

        // 5. Check if another player is lined up in front of Naoya
        List<PlayerEntity> otherPlayers = sw.getEntitiesByClass(
                PlayerEntity.class,
                this.naoya.getBoundingBox().expand(24.0),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && !p.getUuid().equals(rearAttacker.getUuid())
        );

        PlayerEntity linedUpPlayer = null;
        for (PlayerEntity other : otherPlayers) {
            // Minimum distance check: the two players must be at least 2.5 blocks apart
            if (rearAttacker.distanceTo(other) < 2.5) {
                continue;
            }

            Vec3d vOther = new Vec3d(other.getX() - this.naoya.getX(), 0, other.getZ() - this.naoya.getZ());
            if (vOther.lengthSquared() > 1e-4) {
                vOther = vOther.normalize();
                if (vRear.dotProduct(vOther) > 0.82) { // Angular alignment < ~34 degrees
                    linedUpPlayer = other;
                    break;
                }
            }
        }

        if (linedUpPlayer != null) {
            // Both players are lined up along the same forward corridor: unleash penetrating Shockwave Punch!
            executeShockwavePunch(sw, rearAttacker, linedUpPlayer, vRear);
        } else {
            // Not lined up: execute rapid evasive strike on rear attacker
            executeRearEvasiveStrike(sw, rearAttacker, vRear);
        }

        return true;
    }

    private void executeShockwavePunch(ServerWorld sw, LivingEntity rearAttacker, PlayerEntity linedUpPlayer, Vec3d punchDir) {
        this.naoya.triggerAttackAnim();

        sw.playSound(null, this.naoya.getX(), this.naoya.getY(), this.naoya.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.8F, 1.3F);
        sw.playSound(null, this.naoya.getX(), this.naoya.getY(), this.naoya.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.6F, 1.4F);

        Set<UUID> hitUuids = new HashSet<>();
        float damage = 20.0F + (this.naoya.getSpeedStacks() * 1.5F);
        DamageSource attackSource = this.naoya.getDamageSources().mobAttack(this.naoya);

        // Shockwave travels up to 18 blocks forward along punchDir
        for (double d = 0.5; d <= 18.0; d += 0.6) {
            Vec3d pt = this.naoya.getEyePos().add(punchDir.multiply(d));
            sw.spawnParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 2, 0.15, 0.15, 0.15, 0.05);

            Box hitBox = new Box(pt.x - 1.2, pt.y - 1.0, pt.z - 1.2, pt.x + 1.2, pt.y + 1.0, pt.z + 1.2);
            List<LivingEntity> hitEntities = sw.getEntitiesByClass(
                    LivingEntity.class,
                    hitBox,
                    e -> e != this.naoya && e.isAlive() && !e.isTeammate(this.naoya) && !hitUuids.contains(e.getUuid())
            );

            for (LivingEntity hit : hitEntities) {
                hitUuids.add(hit.getUuid());

                if (hit.isBlocking() && hit instanceof PlayerEntity player) {
                    player.getItemCooldownManager().set(player.getActiveItem(), 60);
                    player.clearActiveItem();
                    sw.sendEntityStatus(player, (byte) 30);
                    sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.2F, 1.0F);
                }

                if (hit.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
                    ProjectionSorcery.shatterFrame(hit, attackSource, damage);
                }

                hit.damage(sw, attackSource, damage);

                if (hit instanceof WallSlamData wsd) {
                    wsd.yunos$setWallSlamTimer(45);
                }

                Vec3d launch = punchDir.multiply(2.0).add(0, 0.35, 0);
                hit.setVelocity(launch);
                hit.velocityModified = true;

                sw.spawnParticles(ParticleTypes.EXPLOSION, hit.getX(), hit.getY() + 1.0, hit.getZ(), 1, 0, 0, 0, 0);
            }
        }

        this.evasionCooldown = DuoModeCheckHelper.isDuoMode(sw) ? 60 : 80;
        this.globalCooldown = 12;
    }

    private void executeRearEvasiveStrike(ServerWorld sw, LivingEntity rearAttacker, Vec3d punchDir) {
        this.naoya.triggerAttackAnim();

        sw.playSound(null, rearAttacker.getX(), rearAttacker.getY(), rearAttacker.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.4F, 1.3F);
        sw.playSound(null, rearAttacker.getX(), rearAttacker.getY(), rearAttacker.getZ(),
                ModSounds.FRAME_FREEZE, SoundCategory.HOSTILE, 1.1F, 1.2F);

        DamageSource attackSource = this.naoya.getDamageSources().mobAttack(this.naoya);
        float strikeDamage = 12.0F + (this.naoya.getSpeedStacks() * 1.0F);

        if (rearAttacker.isBlocking() && rearAttacker instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(player.getActiveItem(), 60);
            player.clearActiveItem();
            sw.sendEntityStatus(player, (byte) 30);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.2F, 1.0F);
        }

        rearAttacker.damage(sw, attackSource, strikeDamage);

        // Inflict brief Frame Freeze and Wall Slam timer
        rearAttacker.addStatusEffect(new StatusEffectInstance(ModEffects.FRAME_FREEZE, 20, 0, false, false, true));
        if (rearAttacker instanceof WallSlamData wsd) {
            wsd.yunos$setWallSlamTimer(35);
        }

        Vec3d push = punchDir.multiply(1.4).add(0, 0.25, 0);
        rearAttacker.setVelocity(rearAttacker.getVelocity().add(push));
        rearAttacker.velocityModified = true;

        sw.spawnParticles(ParticleTypes.CRIT, rearAttacker.getX(), rearAttacker.getY() + 1.0, rearAttacker.getZ(),
                12, 0.2, 0.3, 0.2, 0.1);
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, rearAttacker.getX(), rearAttacker.getY() + 1.0, rearAttacker.getZ(),
                8, 0.2, 0.3, 0.2, 0.1);

        this.evasionCooldown = DuoModeCheckHelper.isDuoMode(sw) ? 50 : 65;
        this.globalCooldown = 8;
    }
}
