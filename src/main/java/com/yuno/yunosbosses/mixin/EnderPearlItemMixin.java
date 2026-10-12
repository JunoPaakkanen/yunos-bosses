package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.AntiTeleportEffect;
import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderPearlItem.class)
public class EnderPearlItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPearlUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (user.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(user);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
