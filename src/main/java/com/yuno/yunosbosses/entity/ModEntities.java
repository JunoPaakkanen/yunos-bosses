package com.yuno.yunosbosses.entity;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.entity.character.FernEntity;
import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.entity.character.modified.UselessChickenEntity;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.entity.other.DomainTcmpEyeEntity;
import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import com.yuno.yunosbosses.entity.projectile.FlameArrowEntity;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    // 1. Create the Registry Key
    public static final RegistryKey<EntityType<?>> UBEL_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "ubel")
    );

    // 2. Register the EntityType
    public static final EntityType<UbelEntity> UBEL = Registry.register(
            Registries.ENTITY_TYPE,
            UBEL_KEY,
            EntityType.Builder.create(UbelEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f) // Same size as a normal player
                    .build(UBEL_KEY)
    );

    public static final RegistryKey<EntityType<?>> METHODE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "methode")
    );

    public static final EntityType<MethodeEntity> METHODE = Registry.register(
            Registries.ENTITY_TYPE,
            METHODE_KEY,
            EntityType.Builder.create(MethodeEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 2.0f) // Slightly taller than a normal player
                    .build(METHODE_KEY)
    );

    public static final RegistryKey<EntityType<?>> NAOYA_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "naoya")
    );

    public static final EntityType<NaoyaEntity> NAOYA = Registry.register(
            Registries.ENTITY_TYPE,
            NAOYA_KEY,
            EntityType.Builder.create(NaoyaEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f)
                    .build(NAOYA_KEY)
    );

    public static final RegistryKey<EntityType<?>> FERN_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "fern")
    );

    public static final EntityType<FernEntity> FERN = Registry.register(
            Registries.ENTITY_TYPE,
            FERN_KEY,
            EntityType.Builder.create(FernEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f)
                    .build(FERN_KEY)
    );

    public static final RegistryKey<EntityType<?>> SLASH_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "slash_projectile")
    );

    public static final EntityType<SlashProjectileEntity> SLASH_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            SLASH_PROJECTILE_KEY,
            EntityType.Builder.<SlashProjectileEntity>create(SlashProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(2.5f, 0.5f) // Wide and flat, like a slash!
                    .maxTrackingRange(64)
                    .trackingTickInterval(1) // Tick every single frame for smooth movement
                    .build(SLASH_PROJECTILE_KEY)
    );

    public static final RegistryKey<EntityType<?>> FLAME_ARROW_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "flame_arrow")
    );

    public static final EntityType<FlameArrowEntity> FLAME_ARROW = Registry.register(
            Registries.ENTITY_TYPE,
            FLAME_ARROW_KEY,
            EntityType.Builder.<FlameArrowEntity>create(FlameArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0f, 1.0f)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(FLAME_ARROW_KEY)
    );

    public static final RegistryKey<EntityType<?>> SEVERED_TORSO_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "severed_torso")
    );

    public static final EntityType<SeveredTorsoEntity> SEVERED_TORSO = Registry.register(
            Registries.ENTITY_TYPE,
            SEVERED_TORSO_KEY,
            EntityType.Builder.<SeveredTorsoEntity>create(SeveredTorsoEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 0.9f) // Half the height of a normal player!
                    .build(SEVERED_TORSO_KEY)
    );

    public static final RegistryKey<EntityType<?>> USELESS_CHICKEN_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "useless_chicken")
    );

    public static final EntityType<UselessChickenEntity> USELESS_CHICKEN = Registry.register(
            Registries.ENTITY_TYPE,
            USELESS_CHICKEN_KEY,
            EntityType.Builder.create(UselessChickenEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.4f, 0.7f)
                    .build(USELESS_CHICKEN_KEY)
    );

    public static final RegistryKey<EntityType<?>> DOMAIN_SHRINE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "domain_shrine")
    );

    public static final EntityType<DomainShrineEntity> DOMAIN_SHRINE = Registry.register(
            Registries.ENTITY_TYPE,
            DOMAIN_SHRINE_KEY,
            EntityType.Builder.<DomainShrineEntity>create(DomainShrineEntity::new, SpawnGroup.MISC)
                    .dimensions(4.0f, 5.0f)
                    .maxTrackingRange(128)
                    .build(DOMAIN_SHRINE_KEY)
    );

    public static final RegistryKey<EntityType<?>> DOMAIN_TCMP_EYE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(YunosBosses.MOD_ID, "domain_tcmp_eye")
    );

    public static final EntityType<DomainTcmpEyeEntity> DOMAIN_TCMP_EYE = Registry.register(
            Registries.ENTITY_TYPE,
            DOMAIN_TCMP_EYE_KEY,
            EntityType.Builder.<DomainTcmpEyeEntity>create(DomainTcmpEyeEntity::new, SpawnGroup.MISC)
                    .dimensions(3.0f, 3.0f)
                    .maxTrackingRange(128)
                    .build(DOMAIN_TCMP_EYE_KEY)
    );

    // 3. Register the Attributes when the mod initializes
    public static void registerModEntities() {
        YunosBosses.LOGGER.info("Registering Mod Entities for " + YunosBosses.MOD_ID);
        FabricDefaultAttributeRegistry.register(UBEL, UbelEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(METHODE, MethodeEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(NAOYA, NaoyaEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(FERN, FernEntity.setAttributes());
        FabricDefaultAttributeRegistry.register(USELESS_CHICKEN, UselessChickenEntity.createChickenAttributes());
    }
}
