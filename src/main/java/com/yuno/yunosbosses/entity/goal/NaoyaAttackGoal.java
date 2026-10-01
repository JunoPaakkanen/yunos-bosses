package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.MeleeAttackAbility;

public class NaoyaAttackGoal extends AbstractBossAttackGoal {
    private final NaoyaEntity naoya;

    public NaoyaAttackGoal(NaoyaEntity naoya, double speed) {
        super(naoya, speed);
        this.naoya = naoya;

        // Basic Melee Strike
        this.registerAbility(new MeleeAttackAbility(3.0, 5, 10, 10.0F));
    }

    @Override
    protected void onAbilityStarted(BossAbility ability) {
        this.naoya.triggerAttackAnim();
    }
}
