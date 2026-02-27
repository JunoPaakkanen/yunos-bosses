package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBeam;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KillingMagicRenderer {

    public static void renderBeam(WorldRenderContext context, Vec3d start, int range, ActiveBeam beam) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        PlayerEntity owner = context.world().getPlayerByUuid(beam.getOwnerUuid());
        if (owner == null) return;

        // Get current player orientation
        Vec3d activeVisualStart;
        Vec3d activeLookDir;
        Vec3d activeCurrentStart;

        if (!beam.isCharging()) {
            if (beam.getLockedStart() == null) {
                // First frame of firing: Capture the snapshot
                activeLookDir = owner.getRotationVector();
                activeCurrentStart = owner.getEyePos();
                activeVisualStart = activeCurrentStart.add(activeLookDir.multiply(1.0));
                beam.lock(activeVisualStart, activeLookDir);
            } else {
                // Subsequent frames: Use the LOCK
                activeVisualStart = beam.getLockedStart();
                activeLookDir = beam.getLockedDir();
                activeCurrentStart = activeVisualStart.subtract(activeLookDir.multiply(1.0));
            }
        } else {
            activeLookDir = owner.getRotationVector();
            activeCurrentStart = owner.getEyePos();
            activeVisualStart = activeCurrentStart.add(activeLookDir.multiply(1.0));
        }
        matrices.push();

        matrices.translate(
                activeVisualStart.x - cameraPos.x,
                activeVisualStart.y - cameraPos.y,
                activeVisualStart.z - cameraPos.z
        );

        // Calculate Alpha (255 is solid, 0 is invisible)
        // As lifeRatio goes from 0.0 to 1.0, alpha goes from 255 to 0
        int circleAlpha = beam.isCharging() ? 155 : (int) ((1.0f - beam.getFiringProgress()) * 255);

        Vec3d circleRenderDir = activeLookDir;

        // Get the matrix for position calculations
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Define a perpendicular vector for thickness
        Vec3d up = new Vec3d(0, 1, 0);
        float beamRadius = 0.2f;
        float circleRadius = 1.0f;

        // Perpendicular vectors for the circle
        Vec3d circlePerp1 = circleRenderDir.crossProduct(up).normalize().multiply(circleRadius);
        Vec3d circlePerp2 = circleRenderDir.crossProduct(circlePerp1).normalize().multiply(circleRadius);

        // Draw the beam
        if (!beam.isCharging()) {
            float beamLifeRatio = (float) (beam.getCurrentTicks() - 20) / (beam.getMaxTicks() - 20);
            int beamAlpha = (int) ((1.0f - beamLifeRatio) * 255);

            // Calculate the beam path using the latest positions
            Vec3d beamEnd = activeCurrentStart.add(activeLookDir.multiply(range));
            Vec3d renderDir = beamEnd.subtract(activeVisualStart); // Direction vector (B - A)

            // Perpendicular vectors for the beam
            Vec3d perp1 = renderDir.crossProduct(up).normalize().multiply(beamRadius);
            Vec3d perp2 = renderDir.crossProduct(perp1).normalize().multiply(beamRadius);

            VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.getLightning());
            // Draw the first quad
            drawDoubleSidedQuad(buffer, posMatrix, perp1.multiply(-1), renderDir.add(perp1.multiply(-1)), renderDir.add(perp1), perp1, beamAlpha);
            // Draw the second quad
            drawDoubleSidedQuad(buffer, posMatrix, perp2.multiply(-1), renderDir.add(perp2.multiply(-1)), renderDir.add(perp2), perp2, beamAlpha);
        }

        matrices.push(); // Create a nested sandbox for the spin
        // Calculate rotation based on time (adjust the divisor to change speed)
        float spin = (float)(System.currentTimeMillis() % 2000) / 4000.0f * 360.0f;

        Vector3f axis = new Vector3f((float) activeLookDir.x, (float) activeLookDir.y, (float) activeLookDir.z).normalize();
        matrices.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(spin), axis));

        VertexConsumer circleBuffer = context.consumers().getBuffer(RenderLayer.getEntityTranslucent(
                Identifier.of("yunosbosses", "textures/effect/magic_circle.png")));

        // Draw the magic circle
        drawDoubleSidedQuad(circleBuffer, matrices.peek().getPositionMatrix(),
                circlePerp1.add(circlePerp2), circlePerp1.subtract(circlePerp2),
                circlePerp1.multiply(-1).subtract(circlePerp2), circlePerp1.multiply(-1).add(circlePerp2), circleAlpha);

        matrices.pop();
        matrices.pop();
    }

    public static void renderBeam(WorldRenderContext context, ActiveBeam beam) {
        // Look for the player who owns this beam
        PlayerEntity owner = context.world().getPlayerByUuid(beam.getOwnerUuid());

        Vec3d currentStart;
        if (owner != null) {
            currentStart = owner.getEyePos();
        } else {
            currentStart = beam.getStart();
        }

        renderBeam(context, currentStart, beam.getRange(), beam);
    }

    private static void drawDoubleSidedQuad(VertexConsumer buffer, Matrix4f posMatrix, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int alpha) {
        // Front side
        drawVertex(buffer, posMatrix, (float)p1.x, (float)p1.y, (float)p1.z, 0, 0, alpha);
        drawVertex(buffer, posMatrix, (float)p2.x, (float)p2.y, (float)p2.z, 0, 1, alpha);
        drawVertex(buffer, posMatrix, (float)p3.x, (float)p3.y, (float)p3.z, 1, 1, alpha);
        drawVertex(buffer, posMatrix, (float)p4.x, (float)p4.y, (float)p4.z, 1, 0, alpha);

        // Back side (Reverse vertex order)
        drawVertex(buffer, posMatrix, (float)p4.x, (float)p4.y, (float)p4.z, 1, 0, alpha);
        drawVertex(buffer, posMatrix, (float)p3.x, (float)p3.y, (float)p3.z, 1, 1, alpha);
        drawVertex(buffer, posMatrix, (float)p2.x, (float)p2.y, (float)p2.z, 0, 1, alpha);
        drawVertex(buffer, posMatrix, (float)p1.x, (float)p1.y, (float)p1.z, 0, 0, alpha);
    }

    private static void drawVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float u, float v, int alpha) {
        buffer.vertex(matrix, x, y, z)          // 1. Set Position
                .color(214, 227, 255, alpha)        // 2. Set Color
                .texture(u, v)                    // 3. Set UV Mapping
                .overlay(OverlayTexture.DEFAULT_UV) // 4. Set Overlay (Usually default)
                .light(15728880)                  // 5. Set Light (Full brightness/Glow)
                .normal(0, 1, 0);           // 6. Set Normal (Which way the face points)
    }
}
