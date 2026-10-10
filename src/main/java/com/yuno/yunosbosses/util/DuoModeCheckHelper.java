package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.world.ModGameRules;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class DuoModeCheckHelper {

    public static boolean isDuoMode(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return serverWorld.getGameRules().getBoolean(ModGameRules.DUO_BOSS_DIFFICULTY);
        }
        return false;
    }
}
