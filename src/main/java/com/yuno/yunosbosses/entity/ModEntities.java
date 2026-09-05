package com.yuno.yunosbosses.entity;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.entity.character.modified.UselessChickenEntity;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<UbelEntity> UBEL = register(
            "ubel",
            EntityType.Builder.create(UbelEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f)
    );

    public static final EntityType<MethodeEntity> METHODE = register(
            "methode",
            EntityType.Builder.create(MethodeEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.8f, 2.4f)
    );

    public static final EntityType<SlashProjectileEntity> SLASH_PROJECTILE = register(
            "slash_projectile",
            EntityType.Builder.<SlashProjectileEntity>create(SlashProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f)
    );

    public static final EntityType<SeveredTorsoEntity> SEVERED_TORSO = register(
            "severed_torso",
            EntityType.Builder.create(SeveredTorsoEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 1f)
    );

    public static final EntityType<UselessChickenEntity> USELESS_CHICKEN = register(
            "useless_chicken",
            EntityType.Builder.create(UselessChickenEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 1f)
    );

    public static final EntityType<DomainShrineEntity> DOMAIN_SHRINE = register(
            "domain_shrine",
            EntityType.Builder.create(DomainShrineEntity::new, SpawnGroup.MISC)
                    .dimensions(7f, 6.5f)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("yunosbosses", name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(UBEL, UbelEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(METHODE, MethodeEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(SEVERED_TORSO, SeveredTorsoEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(USELESS_CHICKEN, UselessChickenEntity.setAttributes());
    }
}
