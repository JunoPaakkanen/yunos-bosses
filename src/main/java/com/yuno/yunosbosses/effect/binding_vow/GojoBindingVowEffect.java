package com.yuno.yunosbosses.effect.binding_vow;

import com.yuno.yunosbosses.effect.MilkProof;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class GojoBindingVowEffect extends StatusEffect implements MilkProof {
    public GojoBindingVowEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    // Gets called every tick when a player has this effect
    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        return super.applyUpdateEffect(entity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

}
