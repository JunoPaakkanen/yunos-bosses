package com.yuno.yunosbosses.entity;

import net.minecraft.entity.damage.DamageSource;

/**
 * Duck interface injected into MobEntity via Mixin to support Mini-Boss mechanics
 * for any vanilla or modded hostile mob that is not a full YunosBossEntity.
 */
public interface YunosMiniBossEntity {
    boolean yunos$isMiniBoss();

    void yunos$setMiniBoss(boolean isMiniBoss);

    boolean yunos$isMiniBossEvaluated();

    void yunos$setMiniBossEvaluated(boolean evaluated);

    int yunos$getReinforcementBrokenTicks();

    void yunos$setReinforcementBrokenTicks(int ticks);

    default boolean yunos$isReinforcementBroken() {
        return yunos$getReinforcementBrokenTicks() > 0;
    }

    void yunos$breakReinforcement(int ticks, DamageSource source);
}
