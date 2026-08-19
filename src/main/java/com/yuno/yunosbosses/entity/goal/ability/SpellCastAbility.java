package com.yuno.yunosbosses.entity.goal.ability;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.function.Supplier;

public class SpellCastAbility implements BossAbility {
    private final double maxDistanceSq;
    private final int windupTicks;
    private final int recoveryTicks;
    private final Supplier<Spell> spellSupplier;

    public SpellCastAbility(double maxDistanceSq, int windupTicks, int recoveryTicks, Supplier<Spell> spellSupplier) {
        this.maxDistanceSq = maxDistanceSq;
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.spellSupplier = spellSupplier;
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
        Spell spell = spellSupplier.get();
        if (spell != null) {
            spell.cast(boss.getWorld(), boss, boss.getMainHandStack());
        }
    }
}