package com.yuno.yunosbosses.world;

import com.yuno.yunosbosses.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public class ModEntitySpawns {

    public static void registerEntitySpawns() {
        // --- SPAWN RESTRICTIONS ---
        // Physical rules for spawning entities
        SpawnRestriction.register(
                ModEntities.UBEL,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MobEntity::canMobSpawn
        );
        SpawnRestriction.register(
                ModEntities.METHODE,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MobEntity::canMobSpawn
        );

        // --- BIOME MODIFICATIONS ---
        // Add entities to the spawn pools of specific biomes
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE,
                ModEntities.UBEL,
                2, 1, 1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE,
                ModEntities.METHODE,
                2, 1, 1
        );
    }
}
