package com.yuno.yunosbosses.entity.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModDamageTypes {
    public static final RegistryKey<DamageType> CUTTING_MAGIC =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("yunosbosses", "cutting_magic"));
    public static final RegistryKey<DamageType> CUTTING_MAGIC_SHALLOW =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("yunosbosses", "cutting_magic_shallow"));
    public static final RegistryKey<DamageType> FIRE_MAGIC =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("yunosbosses", "fire_magic"));

    public static DamageSource of(World world, RegistryKey<DamageType> key, Entity attacker) {
        return new DamageSource(world.getRegistryManager()
                .getOrThrow(RegistryKeys.DAMAGE_TYPE)
                .getOrThrow(key), attacker);
    }
}