package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.entity.goal.ability.MeleeAttackAbility;
import com.yuno.yunosbosses.entity.goal.ability.SpellCastAbility;
import com.yuno.yunosbosses.spell.ModSpells;

public class MethodeAttackGoal extends AbstractBossAttackGoal {

    public MethodeAttackGoal(MethodeEntity methode, double speed) {
        super(methode, speed);

        // Set the ideal distance
        this.setIdealDistance(7);

        // Register Defensive Magic (Blocks incoming projectiles)
        this.registerAbility(new DefensiveProjectileShieldAbility(10.0, 0, 30, () -> ModSpells.DEFENSIVE_MAGIC));

        // Register Melee Attack (Range 0 - 4 blocks)
        this.registerAbility(new MeleeAttackAbility(4.0, 10, 15, 7.5F));

        // Register Killing Magic Barrage (Range 4 - 25 blocks)
        this.registerAbility(new SpellCastAbility(25.0, 10, 20, () -> ModSpells.KILLING_MAGIC_BARRAGE));

        // Register Long-Range Dismantle (Range 25 - 400 blocks)
        //this.registerAbility(new SpellCastAbility(400.0, 20, 30, () -> ModSpells.DISMANTLE));
    }
}