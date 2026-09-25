package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.entity.goal.ability.MeleeAttackAbility;
import com.yuno.yunosbosses.entity.goal.ability.SpellCastAbility;
import com.yuno.yunosbosses.spell.ModSpells;

public class MethodeAttackGoal extends AbstractBossAttackGoal {
    private final MethodeEntity methode;

    public MethodeAttackGoal(MethodeEntity methode, double speed) {
        super(methode, speed);
        this.methode = methode;

        // Ideal distance: maintains 7.5 blocks spacing (tolerance 2.0 -> 5.5 to 9.5 blocks)
        this.setIdealDistance(7.5);

        // 1. Reactive Defensive Barrier (intercepts incoming projectiles within 12 blocks, 100-tick / 5-second cooldown)
        this.registerAbility(new DefensiveProjectileShieldAbility(12.0, 0, 100, () -> ModSpells.DEFENSIVE_MAGIC));

        // 2. Point Blank Melee Staff Strike (0.0 to 3.5 blocks) - 14-tick windup, 16-tick recovery (matches 1.5s animation)
        this.registerAbility(new MeleeAttackAbility(3.5, 14, 16, 9.0F));

        // 3. Medium Range Killing Magic Barrage (3.0 to 24.0 blocks) - 14-tick windup, 16-tick recovery
        this.registerAbility(new SpellCastAbility(3.0, 24.0, 14, 16, () -> ModSpells.KILLING_MAGIC_BARRAGE));

        // 4. Long Range Zoltraak Beam (16.0 to 38.0 blocks — snipes retreating targets) - 15-tick windup, 25-tick recovery
        this.registerAbility(new SpellCastAbility(16.0, 38.0, 15, 25, () -> ModSpells.KILLING_MAGIC));
    }

    @Override
    protected void onAbilityStarted(BossAbility ability) {
        this.methode.triggerAttackAnim();
    }
}
