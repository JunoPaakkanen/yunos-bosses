package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.util.FrameFreezeRendererHelper;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void yunosboss$updateFrameFreezeState(T entity, S state, float tickProgress, CallbackInfo ci) {
        boolean frozen = entity.hasStatusEffect(ModEffects.FRAME_FREEZE);
        if (state instanceof FrameFreezeStateAccess access) {
            access.yunosbosses$setFrameFrozen(frozen);
        }
        if (frozen) {
            state.relativeHeadYaw = 0.0F;
            state.pitch = 0.0F;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;scale(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V")
    )
    private void yunosboss$applyCapturedPlayerBodyTransform(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState && state instanceof FrameFreezeStateAccess access) {
            Map<String, PlayerAnimBone> capturedBones = access.yunosbosses$getCapturedPlayerAnimBones();
            if (capturedBones != null) {
                PlayerAnimBone capturedBody = capturedBones.get("body");
                if (capturedBody != null) {
                    PlayerAnimBone body = new PlayerAnimBone("body").copyOtherBone(capturedBody);
                    matrixStack.scale(-1.0F, -1.0F, 1.0F);
                    matrixStack.translate(-body.getPosX() / 16.0F, body.getPosY() / 16.0F + 0.75F, body.getPosZ() / 16.0F);
                    body.rotX *= -1.0F;
                    body.rotY *= -1.0F;
                    RenderUtil.rotateMatrixAroundBone(matrixStack, body);
                    matrixStack.scale(body.getScaleX(), body.getScaleY(), body.getScaleZ());
                    matrixStack.translate(0.0F, -0.75F, 0.0F);
                    matrixStack.scale(-1.0F, -1.0F, 1.0F);
                }
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void yunosboss$flattenModelAndDrawFrame(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.applyFrameFreeze(state, matrixStack, vertexConsumerProvider);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void yunosboss$popMatrix(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.popFrameFreeze(state, matrixStack);
    }
}
