package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaFlankDashAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaFrameShatterAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaJabAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaMachRamAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaPalmStrikeAbility;
import com.yuno.yunosbosses.entity.goal.ability.naoya.NaoyaPunchBarrageAbility;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
}
