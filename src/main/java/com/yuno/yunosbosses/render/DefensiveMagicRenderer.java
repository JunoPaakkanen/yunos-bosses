package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class DefensiveMagicRenderer {
    // You'll need to create this hexagon texture in: assets/yunosbosses/textures/misc/hexagon.png
    private static final Identifier HEX_TEXTURE = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            Vec3d cameraPos = context.camera().getPos();

            // Prevent ConcurrentModificationException by creating a snapshot of the list
            List<ActiveBarrier> barrierSnapshot;
            synchronized (BarrierManager.ACTIVE_BARRIERS) {
                barrierSnapshot = new ArrayList<>(BarrierManager.ACTIVE_BARRIERS);
            }

            // Loop through all barriers stored in the Manager
            for (ActiveBarrier barrier : barrierSnapshot) {
                renderBarrier(matrices, consumers, cameraPos, barrier, context);
            }
        });
    }

    private static void renderBarrier(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, WorldRenderContext context) {
        matrices.push();

        // Move to the barrier's world position
        Vec3d pos = barrier.getPosition();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);

        Vec3d dir = barrier.getDirection().normalize();
        long time = context.world().getTime();
        int pulsingAlpha = (int) (150 + 60 * Math.sin(time * 0.3));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-context.camera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(context.camera().getPitch()));

        // Draw the Hexagonal Grid
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(HEX_TEXTURE, false));

        // Honeycomb Grid Math
        float size = 0.35f;
        float spacingX = 0.65f;
        float spacingY = 0.54f;

        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {

                if ((row == 1 && col == 1) || (row == -1 && col == 1)) {
                    continue;
                }

                float xOffset = col * spacingX + (row % 2 != 0 ? spacingX / 2f : 0);
                float yOffset = row * spacingY;

                drawHexQuad(matrices, buffer,
                        xOffset - size, yOffset - size,
                        xOffset + size, yOffset + size,
                        pulsingAlpha);
            }
        }

        matrices.pop();
    }

    private static void drawHexQuad(MatrixStack matrices, VertexConsumer buffer, float x1, float y1, float x2, float y2, int alpha) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Side A (Front)
        drawVertex(matrix, buffer, x1, y1, 0, 0, alpha);
        drawVertex(matrix, buffer, x1, y2, 0, 1, alpha);
        drawVertex(matrix, buffer, x2, y2, 1, 1, alpha);
        drawVertex(matrix, buffer, x2, y1, 1, 0, alpha);

        // Side B (Back)
        drawVertex(matrix, buffer, x2, y1, 1, 0, alpha);
        drawVertex(matrix, buffer, x2, y2, 1, 1, alpha);
        drawVertex(matrix, buffer, x1, y2, 0, 1, alpha);
        drawVertex(matrix, buffer, x1, y1, 0, 0, alpha);
    }

    private static void drawVertex(Matrix4f matrix, VertexConsumer buffer, float x, float y, float u, float v, int alpha) {
        buffer.vertex(matrix, x, y, 0.01f)
                .color(155, 220, 255, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(0, 0, 1);
    }
}
