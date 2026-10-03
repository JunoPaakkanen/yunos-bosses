package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.util.FrameFreezeRendererHelper;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void yunosboss$flattenModelAndDrawFrame(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.applyFrameFreeze(state, matrixStack, vertexConsumerProvider);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void yunosboss$popMatrix(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.popFrameFreeze(state, matrixStack);
    }
}
