package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.MilkProof;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(MilkBucketItem.class)
public class MilkBucketMixin {
    @Redirect(
            method = "finishUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z")
    )
    private boolean preventClearing(LivingEntity entity) {
        List<RegistryEntry<StatusEffect>> toRemove = new ArrayList<>();

        // Iterate through all status effects on the entity
        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            RegistryEntry<StatusEffect> effectEntry = instance.getEffectType();

            // If the effect is not MilkProof, add it to the removal list
            if (!(effectEntry.value() instanceof MilkProof)) {
                toRemove.add(effectEntry);
            }
        }
        // Remove status effects from the entity
        toRemove.forEach(entity::removeStatusEffect);
        return !toRemove.isEmpty();
    }
}
