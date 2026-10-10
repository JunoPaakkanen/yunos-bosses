package com.yuno.yunosbosses.world;

import com.yuno.yunosbosses.YunosBosses;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanRule> DUO_BOSS_DIFFICULTY =
            GameRuleRegistry.register(
                    "yunosBossDuoDifficulty",
                    GameRules.Category.MOBS,
                    GameRuleFactory.createBooleanRule(false) // default is solo (false)
            );

    public static void register() {
        YunosBosses.LOGGER.info("Registering Mod Game Rules for " + YunosBosses.MOD_ID);
    }
}
