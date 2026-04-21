package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.network.KickAttackPayload;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class PlayerAttackAnimationMixin {

    @Unique
    private int kickCooldown = 0;
    @Unique
    private boolean useSecondKick = false;
    @Unique
    private int impactTimer = 0;

    // Count the timer down every tick
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.kickCooldown > 0) {
            this.kickCooldown--;
        }

        if (this.impactTimer > 0) {
            this.impactTimer--;

            if (this.impactTimer == 0) {
                ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
                ClientPlayNetworking.send(new KickAttackPayload());
            }
        }
    }

    // Attack interceptor
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

        // If the player is transformed (Legs), play the kick animation
        if (transformData.isTransformed()) {

            // If the cooldown is not yet over, cancel the kick
            if (this.kickCooldown > 0) {
                ci.cancel();
                return;
            }

            var layer = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(player, ModAnimations.ANIM_SLOT);

            if (layer instanceof PlayerAnimationController controller) {
                // --- THE ATTACK ---
                if (!useSecondKick) {
                    controller.triggerAnimation(ModAnimations.KICK_ANIM);
                    this.kickCooldown = 12;
                    this.impactTimer = 8;
                    useSecondKick = true;
                } else {
                    controller.triggerAnimation(ModAnimations.KICK_ANIM_2);
                    this.kickCooldown = 16;
                    this.impactTimer = 12;
                    useSecondKick = false;
                }
                player.resetLastAttackedTicks();
            }
        }
    }
}