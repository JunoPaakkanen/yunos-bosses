package com.yuno.yunosbosses.entity;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<UbelEntity> UBEL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("yunosbosses", "ubel"),
            EntityType.Builder.create(UbelEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f)
                    .build()
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(UBEL, UbelEntity.setAttributes());
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(ModEntities.UBEL, UbelEntity.setAttributes());
    }
}
