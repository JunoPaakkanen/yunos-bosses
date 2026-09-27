package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.render.gui.DomainClashClient;
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

/**
 * 3D world renderer for the physical collision seam and clashing energy field
 * at the boundary between two competing Domain Expansions.
 */
public class DomainClashRenderer {

    private static final Identifier BARRIER_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier.png");
    private static final Identifier SLASH_TEXTURE = Identifier.of("yunosbosses", "textures/entity/slash.png");

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!DomainClashClient.isActive()) return;

            Vec3d clashPos = DomainClashClient.getClashPos();
            if (clashPos == null || clashPos.equals(Vec3d.ZERO)) return;

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            Vec3d camera = context.camera().getPos();

            matrices.push();
            matrices.translate(clashPos.x - camera.x, clashPos.y - camera.y, clashPos.z - camera.z);

            long time = context.world().getTime();
            int elapsed = DomainClashClient.getElapsedTicks();

            // Face the collision boundary between casters
            Vec3d p1Pos = Vec3d.ZERO;
            Vec3d p2Pos = Vec3d.ZERO;
            if (context.world().getPlayerByUuid(DomainClashClient.getCaster1Uuid()) != null) {
                p1Pos = context.world().getPlayerByUuid(DomainClashClient.getCaster1Uuid()).getPos();
            }
            if (context.world().getPlayerByUuid(DomainClashClient.getCaster2Uuid()) != null) {
                p2Pos = context.world().getPlayerByUuid(DomainClashClient.getCaster2Uuid()).getPos();
            }

            float yaw = 0.0f;
            if (!p1Pos.equals(p2Pos)) {
                Vec3d diff = p2Pos.subtract(p1Pos);
                yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x));
            }
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw + 90.0f));

            Matrix4f modelMatrix = matrices.peek().getPositionMatrix();

            // 1. Vertical Clashing Energy Seam Disc
            VertexConsumer discBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER_TEXTURE));
            int segments = 48;
            float discRadius = 12.0f;
            int pulseAlpha = (int) (140 + 50 * Math.sin(elapsed * 0.45));

            float uScroll = (time % 80) / 80.0f;

            for (int i = 0; i < segments; i++) {
                float a0 = (float) (2 * Math.PI * (float) i / segments);
                float a1 = (float) (2 * Math.PI * (float) (i + 1) / segments);

                float x0 = (float) Math.cos(a0) * discRadius;
                float y0 = (float) Math.sin(a0) * discRadius;
                float x1 = (float) Math.cos(a1) * discRadius;
                float y1 = (float) Math.sin(a1) * discRadius;

                float u0 = ((float) i / segments) + uScroll;
                float u1 = ((float) (i + 1) / segments) + uScroll;

                // Crimson Side (facing Caster 1)
                discBuffer.vertex(modelMatrix, 0, 0, -0.05f).color(255, 30, 40, pulseAlpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, -1);
                discBuffer.vertex(modelMatrix, x0, y0, -0.05f).color(220, 20, 30, 0).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, -1);
                discBuffer.vertex(modelMatrix, x1, y1, -0.05f).color(220, 20, 30, 0).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, -1);
                discBuffer.vertex(modelMatrix, 0, 0, -0.05f).color(255, 30, 40, pulseAlpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, -1);

                // Azure Side (facing Caster 2)
                discBuffer.vertex(modelMatrix, 0, 0, 0.05f).color(30, 120, 255, pulseAlpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, x1, y1, 0.05f).color(20, 90, 230, 0).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, x0, y0, 0.05f).color(20, 90, 230, 0).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, 0, 0, 0.05f).color(30, 120, 255, pulseAlpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
            }

            // 2. White-Hot Energy Core Ring
            float coreRadius = 4.5f + (float) Math.sin(elapsed * 0.6) * 0.8f;
            for (int i = 0; i < segments; i++) {
                float a0 = (float) (2 * Math.PI * (float) i / segments);
                float a1 = (float) (2 * Math.PI * (float) (i + 1) / segments);

                float x0 = (float) Math.cos(a0) * coreRadius;
                float y0 = (float) Math.sin(a0) * coreRadius;
                float x1 = (float) Math.cos(a1) * coreRadius;
                float y1 = (float) Math.sin(a1) * coreRadius;

                discBuffer.vertex(modelMatrix, 0, 0, 0).color(255, 255, 255, 230).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, x0, y0, 0).color(255, 220, 120, 0).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, x1, y1, 0).color(255, 220, 120, 0).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
                discBuffer.vertex(modelMatrix, 0, 0, 0).color(255, 255, 255, 230).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 0, 1);
            }

            matrices.pop();
        });
    }
}
