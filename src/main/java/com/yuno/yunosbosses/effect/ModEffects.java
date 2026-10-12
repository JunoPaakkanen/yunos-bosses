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

    public static final RegistryEntry<StatusEffect> FRAME_FREEZE = registerStatusEffect("frame_freeze",
            new FrameFreezeEffect());

    public static final RegistryEntry<StatusEffect> ANTI_TELEPORT = registerStatusEffect("anti_teleport",
            new AntiTeleportEffect(StatusEffectCategory.HARMFUL, 0x4A148C));

    public static final RegistryEntry<StatusEffect> ANTI_TOTEM = registerStatusEffect("anti_totem",
            new AntiTotemEffect(StatusEffectCategory.HARMFUL, 0x8B0000));

    public static final RegistryEntry<StatusEffect> ANTI_ESCAPE = registerStatusEffect("anti_escape",
            new AntiEscapeEffect(StatusEffectCategory.HARMFUL, 0x2B3A42));

    private static RegistryEntry<StatusEffect> registerStatusEffect(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(YunosBosses.MOD_ID, name), effect);
    }

    public static void registerEffects() {
        YunosBosses.LOGGER.info("Registering Mod Effects for " + YunosBosses.MOD_ID);
    }
}
