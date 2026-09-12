package com.yuno.yunosbosses.mixin;

import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerFirstPersonRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void alignBodyYawInFirstPerson(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (FirstPersonMode.isFirstPersonPass() && state instanceof IPlayerAnimationState animState && animState.playerAnimLib$isCameraEntity()) {
            state.bodyYaw = player.getYaw(tickDelta);
            state.relativeHeadYaw = 0.0f;
        }
    }

    @Inject(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", at = @At("TAIL"))
    private void applyFirstPersonPitch(PlayerEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseScale, CallbackInfo ci) {
        if (FirstPersonMode.isFirstPersonPass() && state instanceof IPlayerAnimationState animState && animState.playerAnimLib$isCameraEntity()) {
            float eyeHeight = 1.62f;
            matrices.translate(0.0f, eyeHeight, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
            matrices.translate(0.0f, -eyeHeight, 0.0f);
        }
    }
}
