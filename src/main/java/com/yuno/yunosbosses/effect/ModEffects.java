package com.yuno.yunosbosses.effect;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.effect.binding_vow.GojoBindingVowEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModEffects {

    public static final RegistryEntry<StatusEffect> GOJO_BINDING_VOW = registerStatusEffect("gojo",
            new GojoBindingVowEffect(StatusEffectCategory.NEUTRAL, 0x00b5ff));


    private static RegistryEntry<StatusEffect> registerStatusEffect(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(YunosBosses.MOD_ID, name), effect);
    }

    public static void registerEffects() {
        YunosBosses.LOGGER.info("Registering Mod Effects for " + YunosBosses.MOD_ID);
    }
}
