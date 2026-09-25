package com.yuno.yunosbosses.world;

import com.yuno.yunosbosses.entity.ModEntities;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public class ModEntitySpawns {

    public static void registerEntitySpawns() {
        // --- SPAWN RESTRICTIONS ---
        // Physical rules for spawning entities (used by spawn eggs and summon commands)
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
    }
}
