package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.ModSpells;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class ModEntityComponents implements EntityComponentInitializer {
    public static final ComponentKey<SpellComponent> SPELL_DATA = ComponentRegistry.getOrCreate(Identifier.of("yunosbosses", "spells"), SpellComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(SPELL_DATA, player -> {
            PlayerSpellComponent component = new PlayerSpellComponent();
            return component;
        }, RespawnCopyStrategy.ALWAYS_COPY);
    }
}
