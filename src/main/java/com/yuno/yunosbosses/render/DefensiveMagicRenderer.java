package com.yuno.yunosbosses.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
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
    private static final Identifier HEX_TEXTURE = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");
    private static final Identifier BARRIER_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier.png");
    private static final Identifier DOMAIN_EXTERIOR = Identifier.of("yunosbosses", "textures/domain_expansion/domain_exterior.png");

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            Vec3d cameraPos = context.camera().getPos();

            for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
                // If direction is ZERO, treat it as a Sphere
                if (barrier.getDirection().equals(Vec3d.ZERO)) {
                    renderSphere(matrices, consumers, cameraPos, barrier, 20.0f);
                } else {
                    // Otherwise, render the directional hex shield
                    renderBarrier(matrices, consumers, cameraPos, barrier, context);
                }
            }
        });
    }

    // Default Spherical Barrier
    private static void renderSphere(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, float radius) {
        if (barrier.getTexture() != null) {
            renderSphere(matrices, consumers, camera, barrier, radius, barrier.getTexture());
        } else {
            renderSphere(matrices, consumers, camera, barrier, radius, BARRIER_TEXTURE);
        }
    }

    // Custom Spherical Barrier with a different texture
    private static void renderSphere(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, float radius, Identifier texture) {
        matrices.push();

        Vec3d pos = barrier.getPosition();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);

        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();

        int segments = 48; // Quality of the sphere
        int alpha = 255;

        // --- PASS 1: OUTER SPHERE ---
        VertexConsumer outerBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(DOMAIN_EXTERIOR));

        for (int i = 0; i < segments; i++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) i / segments);
            float lat1 = (float) Math.PI * (-0.5f + (float) (i + 1) / segments);

            float y0 = (float) Math.sin(lat0);
            float yr0 = (float) Math.cos(lat0);
            float y1 = (float) Math.sin(lat1);
            float yr1 = (float) Math.cos(lat1);

            for (int j = 0; j < segments; j++) {
                float lng0 = (float) (2 * Math.PI * (float) j / segments);
                float lng1 = (float) (2 * Math.PI * (float) (j + 1) / segments);

                float x0 = (float) Math.cos(lng0);
                float z0 = (float) Math.sin(lng0);
                float x1 = (float) Math.cos(lng1);
                float z1 = (float) Math.sin(lng1);

                float u0 = (float) j / segments;
                float u1 = (float) (j + 1) / segments;
                float v0 = 1.0f - ((float) i / segments);
                float v1 = 1.0f - ((float) (i + 1) / segments);

                float px1 = x0 * yr0 * radius, py1 = y0 * radius, pz1 = z0 * yr0 * radius;
                float px2 = x1 * yr0 * radius, py2 = y0 * radius, pz2 = z1 * yr0 * radius;
                float px3 = x1 * yr1 * radius, py3 = y1 * radius, pz3 = z1 * yr1 * radius;
                float px4 = x0 * yr1 * radius, py4 = y1 * radius, pz4 = z0 * yr1 * radius;

                float nx1 = x0 * yr0, ny1 = y0, nz1 = z0 * yr0;
                float nx2 = x1 * yr0, ny2 = y0, nz2 = z1 * yr0;
                float nx3 = x1 * yr1, ny3 = y1, nz3 = z1 * yr1;
                float nx4 = x0 * yr1, ny4 = y1, nz4 = z0 * yr1;

                // FIXED: Flipped winding sequence (4 -> 3 -> 2 -> 1) forces the solid shell to face OUTWARD
                outerBuffer.vertex(modelMatrix, px4, py4, pz4).color(255, 255, 255, alpha).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(nx4, ny4, nz4);
                outerBuffer.vertex(modelMatrix, px3, py3, pz3).color(255, 255, 255, alpha).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(nx3, ny3, nz3);
                outerBuffer.vertex(modelMatrix, px2, py2, pz2).color(255, 255, 255, alpha).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(nx2, ny2, nz2);
                outerBuffer.vertex(modelMatrix, px1, py1, pz1).color(255, 255, 255, alpha).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(nx1, ny1, nz1);
            }
        }

        // --- PASS 2: INNER SPHERE ---
        float innerRadius = radius - 0.05f;
        VertexConsumer innerBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(texture));

        for (int i = 0; i < segments; i++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) i / segments);
            float lat1 = (float) Math.PI * (-0.5f + (float) (i + 1) / segments);

            float y0 = (float) Math.sin(lat0);
            float yr0 = (float) Math.cos(lat0);
            float y1 = (float) Math.sin(lat1);
            float yr1 = (float) Math.cos(lat1);

            for (int j = 0; j < segments; j++) {
                float lng0 = (float) (2 * Math.PI * (float) j / segments);
                float lng1 = (float) (2 * Math.PI * (float) (j + 1) / segments);

                float x0 = (float) Math.cos(lng0);
                float z0 = (float) Math.sin(lng0);
                float x1 = (float) Math.cos(lng1);
                float z1 = (float) Math.sin(lng1);

                // Split the UV map in half to render the texture twice
                float u0 = ((float) j / segments) * 2.0F;
                float u1 = ((float) (j + 1) / segments) * 2.0F;

                float v0 = 1.0f - ((float) i / segments);
                float v1 = 1.0f - ((float) (i + 1) / segments);

                float ix1 = x0 * yr0 * innerRadius, iy1 = y0 * innerRadius, iz1 = z0 * yr0 * innerRadius;
                float ix2 = x1 * yr0 * innerRadius, iy2 = y0 * innerRadius, iz2 = z1 * yr0 * innerRadius;
                float ix3 = x1 * yr1 * innerRadius, iy3 = y1 * innerRadius, iz3 = z1 * yr1 * innerRadius;
                float ix4 = x0 * yr1 * innerRadius, iy4 = y1 * innerRadius, iz4 = z0 * yr1 * innerRadius;


                // FIXED: Standard winding sequence (1 -> 2 -> 3 -> 4) forces the glowing skybox to face INWARD
                innerBuffer.vertex(modelMatrix, ix1, iy1, iz1).color(255, 255, 255, alpha).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                innerBuffer.vertex(modelMatrix, ix2, iy2, iz2).color(255, 255, 255, alpha).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                innerBuffer.vertex(modelMatrix, ix3, iy3, iz3).color(255, 255, 255, alpha).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                innerBuffer.vertex(modelMatrix, ix4, iy4, iz4).color(255, 255, 255, alpha).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            }
        }

        matrices.pop();
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
