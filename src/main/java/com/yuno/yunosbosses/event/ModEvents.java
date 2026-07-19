package com.yuno.yunosbosses.event;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.effect.ModEffects;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public class ModEvents {

    public static void registerEvents() {

        YunosBosses.LOGGER.info("Registering Mod Events for " + YunosBosses.MOD_ID);

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
                ItemStack stack = player.getStackInHand(hand);
                return TypedActionResult.fail(stack); // Cancels using items
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
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