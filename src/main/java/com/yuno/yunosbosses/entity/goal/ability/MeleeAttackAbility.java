package com.yuno.yunosbosses.entity.goal.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

public class MeleeAttackAbility implements BossAbility {
    private final double maxDistanceSq;
    private final int windupTicks;
    private final int recoveryTicks;
    private final float damage;

    public MeleeAttackAbility(double maxDistance, int windupTicks, int recoveryTicks, float damage) {
        this.maxDistanceSq = maxDistance * maxDistance;
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.damage = damage;
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        return distanceSq <= maxDistanceSq;
    }

    @Override
    public int getWindupTicks() {
        return windupTicks;
    }

    @Override
    public int getRecoveryTicks() {
        return recoveryTicks;
    }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        boss.setTarget(target);
        boss.setAttacking(true);

        // Apply damage if target is still within melee reach
        if (boss.squaredDistanceTo(target) <= maxDistanceSq * 1.5) {
            target.damage((ServerWorld) boss.getWorld(), boss.getWorld().getDamageSources().mobAttack(boss), damage);
            boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, boss.getSoundCategory(), 1.0F, 1.0F);
        }
    }
}
