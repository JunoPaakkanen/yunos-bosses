package com.yuno.yunosbosses.entity.goal.ability.naoya;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.AbstractBossAttackGoal;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class NaoyaJabAbility implements BossAbility {
    private int cooldown = 0;

    @Override
    public void tick(MobEntity boss, LivingEntity target) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        if (this.cooldown > 0 || target == null || !target.isAlive()) return false;
        return distanceSq <= 3.2 * 3.2;
    }

    @Override
    public void onStart(MobEntity boss, LivingEntity target) {
        // Start the punch animation at initiation so the windup plays first
        if (boss instanceof NaoyaEntity naoya) {
            naoya.triggerAttackAnim();
        }
    }

    @Override
    public int getWindupTicks() {
        // 5 ticks (0.25s) matches the exact moment the fist extends forward in animation.naoya.attack
        return 5;
    }

    @Override
    public void tickWindup(MobEntity boss, LivingEntity target, int remainingWindup) {
        AbstractBossAttackGoal.snapLook(boss, target);
    }

    @Override
    public int getRecoveryTicks() {
        // 5 ticks for the fist retraction phase
        return 6;
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        World world = boss.getWorld();
        if (world instanceof ServerWorld sw && target != null && target.isAlive()) {
            DamageSource source = boss.getDamageSources().mobAttack(boss);
            int speedStacks = (boss instanceof NaoyaEntity naoya) ? naoya.getSpeedStacks() : 0;
            float damage = 8.0F + (speedStacks * 0.4F);

            if (target.damage(sw, source, damage)) {
                target.takeKnockback(0.4F, boss.getX() - target.getX(), boss.getZ() - target.getZ());
            }

            // Punch impact audio-visual feedback at moment of impact
            sw.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 0.9F, 1.2F);
            sw.spawnParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.1);
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY() + 0.9, target.getZ(),
                    1, 0, 0, 0, 0);
        }

        this.cooldown = 14;
    }
}
