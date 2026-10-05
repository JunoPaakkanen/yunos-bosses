package com.yuno.yunosbosses.world;

import com.yuno.yunosbosses.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.BiomeKeys;

public class ModEntitySpawns {

    public static void registerEntitySpawns() {
        // --- 1. SPAWN RESTRICTIONS ---
        // Physical rules for spawning entities (must be on ground surface and not peaceful)
        SpawnRestriction.register(
                ModEntities.UBEL,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySpawns::canBossSpawn
        );
        SpawnRestriction.register(
                ModEntities.METHODE,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySpawns::canBossSpawn
        );
        SpawnRestriction.register(
                ModEntities.NAOYA,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySpawns::canBossSpawn
        );

        // --- 2. NATURAL BIOME SPAWN INJECTIONS ---
        // Spawned as MONSTER group with group size 1 (solo boss)

        // ÜBEL: Dense, shadowed, untamed wilderness (Dark Forest & Old Growth Taigas)
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.DARK_FOREST,
                        BiomeKeys.OLD_GROWTH_PINE_TAIGA,
                        BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
                        BiomeKeys.OLD_GROWTH_BIRCH_FOREST
                ),
                SpawnGroup.MONSTER,
                ModEntities.UBEL,
                2, // Rare spawn weight (e.g. Wolf is weight ~8)
                1,
                1
        );

        // METHODE: Serene, spiritual, blooming, or mystical gardens (Flower Forest, Cherry Grove, Pale Garden)
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.FLOWER_FOREST,
                        BiomeKeys.CHERRY_GROVE,
                        BiomeKeys.MEADOW,
                        BiomeKeys.PALE_GARDEN
                ),
                SpawnGroup.MONSTER,
                ModEntities.METHODE,
                2, // Rare spawn weight
                1,
                1
        );

        // NAOYA: Rugged, elevated, open highland terrain for mach-speed dashes (Windswept Hills & Mountain Peaks)
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.WINDSWEPT_HILLS,
                        BiomeKeys.WINDSWEPT_GRAVELLY_HILLS,
                        BiomeKeys.WINDSWEPT_FOREST,
                        BiomeKeys.JAGGED_PEAKS,
                        BiomeKeys.STONY_PEAKS
                ),
                SpawnGroup.MONSTER,
                ModEntities.NAOYA,
                2, // Rare spawn weight
                1,
                1
        );
    }

    public static boolean canBossSpawn(EntityType<? extends PathAwareEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL && MobEntity.canMobSpawn(type, world, spawnReason, pos, random);
    }
}
