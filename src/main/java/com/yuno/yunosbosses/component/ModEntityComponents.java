package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.YunosBosses;
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

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Register spells
        registry.registerForPlayers(SPELL_DATA, player -> {
            PlayerSpellComponent component = new PlayerSpellComponent();
            return component;
        }, RespawnCopyStrategy.ALWAYS_COPY);

        // Register transformations
        registry.registerForPlayers(TRANSFORMATION_DATA, PlayerTransformationComponent::new,
                RespawnCopyStrategy.LOSSLESS_ONLY);

        // Register mana
        registry.registerForPlayers(MANA, PlayerManaComponent::new,
                RespawnCopyStrategy.ALWAYS_COPY);
    }
}
