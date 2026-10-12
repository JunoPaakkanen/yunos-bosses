package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.AntiEscapeEffect;
import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$preventRiptideUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (user.hasStatusEffect(ModEffects.ANTI_ESCAPE)) {
            ItemStack stack = user.getStackInHand(hand);
            if (EnchantmentHelper.getTridentSpinAttackStrength(stack, user) > 0) {
                AntiEscapeEffect.onEscapeAttemptBlocked(user, "Riptide");
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$preventRiptideRelease(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user.hasStatusEffect(ModEffects.ANTI_ESCAPE)) {
            if (EnchantmentHelper.getTridentSpinAttackStrength(stack, user) > 0) {
                AntiEscapeEffect.onEscapeAttemptBlocked(user, "Riptide");
                cir.setReturnValue(false);
            }
        }
    }
}
