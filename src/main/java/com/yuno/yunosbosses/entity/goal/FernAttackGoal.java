package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.FernEntity;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.entity.goal.ability.MeleeAttackAbility;
import com.yuno.yunosbosses.entity.goal.ability.SpellCastAbility;
import com.yuno.yunosbosses.spell.ModSpells;

public class FernAttackGoal extends AbstractBossAttackGoal {
    private final FernEntity fern;

    public FernAttackGoal(FernEntity fern, double speed) {
        super(fern, speed);
        this.fern = fern;

        // Ideal distance: maintains 7.5 blocks spacing
        this.setIdealDistance(7.5);

        // 1. Reactive Defensive Barrier
        this.registerAbility(new DefensiveProjectileShieldAbility(12.0, 0, 100, () -> ModSpells.DEFENSIVE_MAGIC));

        // 2. Point Blank Melee Strike
        this.registerAbility(new MeleeAttackAbility(3.5, 10, 15, 8.0F));

        // 3. Medium Range Killing Magic Barrage
        this.registerAbility(new SpellCastAbility(3.0, 24.0, 14, 16, () -> ModSpells.KILLING_MAGIC_BARRAGE));

        // 4. Long Range Zoltraak Beam
        this.registerAbility(new SpellCastAbility(16.0, 38.0, 15, 25, () -> ModSpells.KILLING_MAGIC));
    }

    @Override
    protected void onAbilityStarted(BossAbility ability) {
        this.fern.triggerAttackAnim();
    }
}
