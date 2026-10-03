package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.util.FrameFreezeRendererHelper;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

@Mixin(GeoEntityRenderer.class)
public abstract class GeoEntityRendererMixin<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void yunosbosses$updateGeoFrameFreezeState(T entity, R state, float tickProgress, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            boolean frozen = living.hasStatusEffect(ModEffects.FRAME_FREEZE);
            if (state instanceof FrameFreezeStateAccess access) {
                access.yunosbosses$setFrameFrozen(frozen);
            }
            if (frozen && state instanceof LivingEntityRenderState livingState) {
                livingState.relativeHeadYaw = 0.0F;
                livingState.pitch = 0.0F;
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void yunosbosses$flattenGeoModelAndDrawFrame(R state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.applyFrameFreeze(state, matrixStack, vertexConsumerProvider);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void yunosbosses$popGeoMatrix(R state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        FrameFreezeRendererHelper.popFrameFreeze(state, matrixStack);
    }
}
