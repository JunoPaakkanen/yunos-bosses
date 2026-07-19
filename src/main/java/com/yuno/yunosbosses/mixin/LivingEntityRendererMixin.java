package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
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
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {

    // Texture to serve as the background frame
    private static final Identifier FRAME_TEXTURE = Identifier.of("minecraft", "textures/block/light_blue_stained_glass.png");

    // Temporary storage to lock head rotations
    private float yunosbosses$savedHeadYaw;
    private float yunosbosses$savedPrevHeadYaw;
    private float yunosbosses$savedPitch;
    private float yunosbosses$savedPrevPitch;

    @Inject(method = "render*", at = @At("HEAD"))
    private void yunosbosses$flattenModelAndDrawFrame(T entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Save current yaw/pitch
            this.yunosbosses$savedHeadYaw = entity.headYaw;
            this.yunosbosses$savedPrevHeadYaw = entity.prevHeadYaw;
            this.yunosbosses$savedPitch = entity.getPitch();
            this.yunosbosses$savedPrevPitch = entity.prevPitch;

            // Lock head angle
            entity.headYaw = entity.bodyYaw;
            entity.prevHeadYaw = entity.prevBodyYaw;
            entity.setPitch(0.0F);
            entity.prevPitch = 0.0F;

            matrixStack.push();

            // Tilt the frame
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(0.25F)); // Tilts slightly left

            // Flatten: Squish the Z-axis (depth) to nearly 0
            matrixStack.scale(1.0F, 1.0F, 0.02F);

            // Draw the frame behind them
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(FRAME_TEXTURE));
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f positionMatrix = entry.getPositionMatrix();

            // Draw a rectangular photo frame backing slightly behind the player's back (On both sides)
            float width = 0.8F;
            float height = 2.0F;
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
            buffer.vertex(positionMatrix, -width, height, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
            buffer.vertex(positionMatrix, width, height, offsetZBack - 0.005F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(0, 10).light(fullBright).normal(0, 0, -1);
        }
    }

    @Inject(method = "render*", at = @At("RETURN"))
    private void yunosbosses$popMatrix(T entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Restore the original player head yaw/pitch so camera looking is unaffected
            entity.headYaw = this.yunosbosses$savedHeadYaw;
            entity.prevHeadYaw = this.yunosbosses$savedPrevHeadYaw;
            entity.setPitch(this.yunosbosses$savedPitch);
            entity.prevPitch = this.yunosbosses$savedPrevPitch;

            matrixStack.pop(); // Restore rendering settings so other entities aren't squished
        }
    }
}