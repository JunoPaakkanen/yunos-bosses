package com.yuno.yunosbosses.entity.goal.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

public interface BossAbility {
    boolean canUse(MobEntity boss, LivingEntity target, double distanceSq);
    int getWindupTicks(); // Time before the attack actually fires
    int getRecoveryTicks(); // Cooldown/recovery after the attack
    void execute(MobEntity boss, LivingEntity target);
}