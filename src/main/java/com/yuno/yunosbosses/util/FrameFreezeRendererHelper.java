package com.yuno.yunosbosses.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class FrameFreezeRendererHelper {
    public static final Identifier FRAME_TEXTURE = Identifier.of("minecraft", "textures/block/light_blue_stained_glass.png");

    public static void applyFrameFreeze(EntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
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

    public static void popFrameFreeze(EntityRenderState state, MatrixStack matrixStack) {
        if (state instanceof FrameFreezeStateAccess access && access.yunosbosses$isFrameFrozen()) {
            matrixStack.pop(); // Restore rendering settings so other entities aren't squished
        }
    }
}
