package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.binding_vow.ModBindingVows;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class ModEntityComponents implements EntityComponentInitializer {

    public static final ComponentKey<SpellComponent> SPELL_DATA =
            ComponentRegistry.getOrCreate(Identifier.of(YunosBosses.MOD_ID, "spells"), SpellComponent.class);

    public static final ComponentKey<TransformationComponent> TRANSFORMATION_DATA =
            ComponentRegistry.getOrCreate(Identifier.of(YunosBosses.MOD_ID, "transformation"), TransformationComponent.class);

    public static final ComponentKey<ManaComponent> MANA =
            ComponentRegistry.getOrCreate(Identifier.of(YunosBosses.MOD_ID, "mana"), ManaComponent.class);

    public static final ComponentKey<BindingVowComponent> BINDING_VOWS =
            ComponentRegistry.getOrCreate(Identifier.of(YunosBosses.MOD_ID, "binding_vows"), BindingVowComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Register spells
        registry.registerForPlayers(SPELL_DATA, PlayerSpellComponent::new, (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
            RespawnCopyStrategy.ALWAYS_COPY.copyForRespawn(from, to, registryLookup, lossless, keepInventory, sameCharacter);
            if (!lossless) {
                to.resetCombatState();
            }
        });

        // Register transformations
        registry.registerForPlayers(TRANSFORMATION_DATA, PlayerTransformationComponent::new,
                RespawnCopyStrategy.LOSSLESS_ONLY);

        // Register mana
        registry.registerForPlayers(MANA, PlayerManaComponent::new, (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
            RespawnCopyStrategy.ALWAYS_COPY.copyForRespawn(from, to, registryLookup, lossless, keepInventory, sameCharacter);
            if (!lossless) {
                to.setManaRegen(0.25F);
            }
        });

        // Register binding vows
        registry.registerForPlayers(BINDING_VOWS, PlayerBindingVowComponent::new, (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
            RespawnCopyStrategy.ALWAYS_COPY.copyForRespawn(from, to, registryLookup, lossless, keepInventory, sameCharacter);
            if (!lossless) {
                to.removeVow(ModBindingVows.GOJO.getId());
            }
        });
    }
}
