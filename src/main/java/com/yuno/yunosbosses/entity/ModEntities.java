package com.yuno.yunosbosses.entity;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
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

    public static final EntityType<SlashProjectileEntity> SLASH_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("yunosbosses", "slash_projectile"),
            EntityType.Builder.<SlashProjectileEntity>create(SlashProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f)
                    .build()
    );

    public static final EntityType<SeveredTorsoEntity> SEVERED_TORSO = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("yunosbosses", "severed_torso"),
            EntityType.Builder.create(SeveredTorsoEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 1f)
                    .build()
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(UBEL, UbelEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(SEVERED_TORSO, SeveredTorsoEntity.setAttributes());
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(ModEntities.UBEL, UbelEntity.setAttributes());
    }
}
