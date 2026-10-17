package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.util.BossCombatHelper;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class NaoyaPalmStrikeAbility implements BossAbility {
    private int cooldown = 80;

    @Override
    public void tick(MobEntity boss, LivingEntity target) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        if (this.cooldown > 0 || target == null || !target.isAlive()) return false;
        return distanceSq <= 3.5 * 3.5 && !target.hasStatusEffect(ModEffects.FRAME_FREEZE);
    }

    @Override
    public void onStart(MobEntity boss, LivingEntity target) {
        if (boss instanceof NaoyaEntity naoya) {
            naoya.triggerAttackAnim();
        }
    }

    @Override
    public int getWindupTicks() {
        return 5;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        AbstractBossAttackGoal.snapLook(boss, target);
    }

    @Override
    public int getRecoveryTicks() {
        return 4; // Extremely brief recovery so Naoya immediately transitions into the Frame Shatter finisher combo
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw && target != null && target.isAlive()) {
            boolean inDomain = (boss instanceof NaoyaEntity naoya) && naoya.isInOwnDomain();
            BossCombatHelper.applySignatureDamage(sw, target, boss, 6.0F);

            // Inflict Frame Freeze
            target.addStatusEffect(new StatusEffectInstance(ModEffects.FRAME_FREEZE, 30, 0, false, false, true));

            if (inDomain) {
                // Inside Time Cell Moon Palace, even the freezing palm strike imparts a concussive knockback jolt!
                Vec3d push = boss.getRotationVec(1.0F).normalize().multiply(0.85).add(0, 0.15, 0);
                target.setVelocity(push);
                if (target instanceof WallSlamData wsd) {
                    wsd.yunos$setWallSlamTimer(35);
                }
            } else {
                target.setVelocity(Vec3d.ZERO);
            }
            target.velocityModified = true;

            sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.FRAME_FREEZE, SoundCategory.HOSTILE, 1.2F, 1.0F);

            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    16, 0.2, 0.2, 0.2, 0.5);
        }

        this.cooldown = 180;
    }
}
