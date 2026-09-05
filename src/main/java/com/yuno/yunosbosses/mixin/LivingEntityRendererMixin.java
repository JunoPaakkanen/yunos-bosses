package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    // Texture to serve as the background frame
    private static final Identifier FRAME_TEXTURE = Identifier.of("minecraft", "textures/block/light_blue_stained_glass.png");

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void yunosbosses$updateFrameFreezeState(T entity, S state, float tickProgress, CallbackInfo ci) {
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
    private void yunosbosses$flattenModelAndDrawFrame(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (state instanceof FrameFreezeStateAccess access && access.yunosbosses$isFrameFrozen()) {
            matrixStack.push();

            // Tilt the frame
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(0.25F)); // Tilts slightly left

            // Flatten: Squish the Z-axis (depth) to nearly 0
            matrixStack.scale(1.0F, 1.0F, 0.02F);

            // Draw the frame behind them
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(FRAME_TEXTURE));
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f positionMatrix = entry.getPositionMatrix();

            // Draw a rectangular photo frame backing slightly behind the entity's back (On both sides)
            float width = Math.max(0.8F, (state.width + 0.4F) / 2.0F);
            float height = Math.max(2.0F, state.height + 0.2F);
            float offsetZ = -0.05F;
            float offsetZBack = -0.05F;

            int fullBright = 15728880;

            // Front quad
            buffer.vertex(positionMatrix, -width, 0.0F, offsetZ).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(0, 10).light(fullBright).normal(0, 0, 1);
            buffer.vertex(positionMatrix, width, 0.0F, offsetZ).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(0, 10).light(fullBright).normal(0, 0, 1);
            buffer.vertex(positionMatrix, width, height, offsetZ).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, 1);
            buffer.vertex(positionMatrix, -width, height, offsetZ).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, 1);

            // Back quad
            buffer.vertex(positionMatrix, width, 0.0F, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
            buffer.vertex(positionMatrix, -width, 0.0F, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
            buffer.vertex(positionMatrix, -width, height, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
            buffer.vertex(positionMatrix, width, height, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void yunosbosses$popMatrix(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (state instanceof FrameFreezeStateAccess access && access.yunosbosses$isFrameFrozen()) {
            matrixStack.pop(); // Restore rendering settings so other entities aren't squished
        }
    }
}
