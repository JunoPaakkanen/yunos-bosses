package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerAttackAnimationMixin {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"))
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        if ((Object) this instanceof AbstractClientPlayerEntity player) {
            var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

            // If the player is transformed (Legs), play the kick animation
            if (transformData.isTransformed()) {
                var layer = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(player, ModAnimations.ANIM_SLOT);

                if (layer instanceof PlayerAnimationController controller) {
                    controller.triggerAnimation(ModAnimations.KICK_ANIM);
                }
            }
        }
    }
}