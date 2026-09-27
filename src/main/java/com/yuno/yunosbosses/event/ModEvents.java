package com.yuno.yunosbosses.event;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public class ModEvents {

    public static void registerEvents() {

        YunosBosses.LOGGER.info("Registering Mod Events for " + YunosBosses.MOD_ID);

        // Fail-safe cleanup: when an entity dies on the server, destroy any owned active domains
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                BarrierManager.onOwnerDeath(entity, serverWorld);
            }
        });

        // Prevent left-clicking/attacking entirely when frozen
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
                return ActionResult.FAIL; // Cancels the attack block
            }
            return ActionResult.PASS;
        });

        // Prevent right-clicking items
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
                return ActionResult.FAIL; // Cancels using items
            }
            return ActionResult.PASS;
        });

        // Prevent right-clicking blocks
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
                return ActionResult.FAIL; // Cancels block interaction
            }
            return ActionResult.PASS;
        });
    }
}
