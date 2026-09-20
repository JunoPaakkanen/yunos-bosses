package com.yuno.yunosbosses.entity.goal.ability;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.function.Supplier;

public class SpellCastAbility implements BossAbility {
    private final double minDistanceSq;
    private final double maxDistanceSq;
    private final int windupTicks;
    private final int recoveryTicks;
    private final Supplier<Spell> spellSupplier;

    public SpellCastAbility(double maxDistance, int windupTicks, int recoveryTicks, Supplier<Spell> spellSupplier) {
        this(0.0, maxDistance, windupTicks, recoveryTicks, spellSupplier);
    }

    public SpellCastAbility(double minDistance, double maxDistance, int windupTicks, int recoveryTicks, Supplier<Spell> spellSupplier) {
        this.minDistanceSq = minDistance * minDistance;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.spellSupplier = spellSupplier;
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        return distanceSq >= minDistanceSq && distanceSq <= maxDistanceSq;
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
        if (target != null && target.isAlive()) {
            boss.setTarget(target);
            boss.setAttacking(true);
            spellSupplier.get().cast(boss.getWorld(), boss, boss.getMainHandStack());
        }
    }
}
