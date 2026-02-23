package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBeam;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class KillingMagicRenderer {

    public static void renderBeam(WorldRenderContext context, Vec3d start, Vec3d end, float lifeRatio) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        matrices.push();

        matrices.translate(
                start.x - cameraPos.x,
                start.y - cameraPos.y,
                start.z - cameraPos.z
        );

        // Calculate Alpha (255 is solid, 0 is invisible)
        // As lifeRatio goes from 0.0 to 1.0, alpha goes from 255 to 0
        int alpha = (int) ((1.0f - lifeRatio) * 255);

        // Vector math and VertexConsumer calls here
        Vec3d dir = end.subtract(start); // Direction vector (B - A)

        // Get the matrix for position calculations
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Define a perpendicular vector for thickness
        Vec3d up = new Vec3d(0, 1, 0);
        float beamRadius = 0.2f;
        // First perpendicular vector
        Vec3d perpendicular = dir.crossProduct(up).normalize().multiply(beamRadius);
        // Second perpendicular vector
        Vec3d perp2 = dir.crossProduct(perpendicular).normalize().multiply(beamRadius);

        // Draw the first quad
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.getLightning());
        drawVertex(buffer, posMatrix, (float)-perpendicular.x, (float)-perpendicular.y, (float)-perpendicular.z, 0, 0, alpha);
        drawVertex(buffer, posMatrix, (float)dir.x - (float)perpendicular.x, (float)dir.y - (float)perpendicular.y, (float)dir.z - (float)perpendicular.z, 0, 1, alpha);
        drawVertex(buffer, posMatrix, (float)dir.x + (float)perpendicular.x, (float)dir.y + (float)perpendicular.y, (float)dir.z + (float)perpendicular.z, 1, 1, alpha);
        drawVertex(buffer, posMatrix, (float)perpendicular.x, (float)perpendicular.y, (float)perpendicular.z, 1, 0, alpha);

        // Draw the second quad
        drawVertex(buffer, posMatrix, (float)-perp2.x, (float)-perp2.y, (float)-perp2.z, 0, 0, alpha);
        drawVertex(buffer, posMatrix, (float)dir.x - (float)perp2.x, (float)dir.y - (float)perp2.y, (float)dir.z - (float)perp2.z, 0, 1, alpha);
        drawVertex(buffer, posMatrix, (float)dir.x + (float)perp2.x, (float)dir.y + (float)perp2.y, (float)dir.z + (float)perp2.z, 1, 1, alpha);
        drawVertex(buffer, posMatrix, (float)perp2.x, (float)perp2.y, (float)perp2.z, 1, 0, alpha);

        matrices.pop();
    }

    public static void renderBeam(WorldRenderContext context, ActiveBeam beam) {
        // Calculate a life ratio (0.0 = new, 1.0 = about to disappear)
        float lifeRatio = (float) beam.getCurrentTicks() / beam.getMaxTicks();

        renderBeam(context, beam.getStart(), beam.getEnd(), lifeRatio);
    }

    private static void drawVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float u, float v, int alpha) {
        buffer.vertex(matrix, x, y, z)          // 1. Set Position
                .color(255, 255, 255, alpha)        // 2. Set Color
                .texture(u, v)                    // 3. Set UV Mapping
                .overlay(OverlayTexture.DEFAULT_UV) // 4. Set Overlay (Usually default)
                .light(15728880)                  // 5. Set Light (Full brightness/Glow)
                .normal(0, 1, 0);           // 6. Set Normal (Which way the face points)
    }
}
