package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBeam;
import com.yuno.yunosbosses.util.BeamManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class KillingMagicRenderer {

    private static final Identifier MAGIC_CIRCLE_TEXTURE =
            Identifier.of("yunosbosses", "textures/effect/magic_circle.png");

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            for (ActiveBeam beam : BeamManager.ACTIVE_BEAMS) {
                renderBeam(context, beam);
            }
        });
    }

    private static Entity getEntityByUuid(ClientWorld world, UUID uuid) {
        if (uuid == null || world == null) return null;
        for (Entity entity : world.getEntities()) {
            if (entity.getUuid().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    public static void renderBeam(WorldRenderContext context, ActiveBeam beam) {
        if (!(context.world() instanceof ClientWorld clientWorld)) return;

        Entity owner = getEntityByUuid(clientWorld, beam.getOwnerUuid());
        if (owner == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        Vec3d activeVisualStart;
        Vec3d activeLookDir;

        if (!beam.isCharging()) {
            if (beam.getLockedStart() == null) {
                // First frame of firing: Lock snapshot position and direction
                if (beam.getDirection() != null) {
                    activeLookDir = beam.getDirection();
                } else {
                    activeLookDir = owner.getRotationVector();
                }

                if (!beam.isUsingCustomStart()) {
                    activeVisualStart = owner.getEyePos().add(activeLookDir.multiply(1.0));
                } else {
                    activeVisualStart = beam.getStart();
                }
                beam.lock(activeVisualStart, activeLookDir);
            } else {
                activeVisualStart = beam.getLockedStart();
                activeLookDir = beam.getLockedDir();
            }
        } else {
            // Charging phase
            if (beam.getDirection() != null) {
                activeLookDir = beam.getDirection();
            } else {
                activeLookDir = owner.getRotationVector();
            }

            if (beam.isUsingCustomStart()) {
                activeVisualStart = beam.getStart();
            } else {
                activeVisualStart = owner.getEyePos().add(activeLookDir.multiply(1.0));
            }
        }

        Vec3d unitDir = activeLookDir.normalize();
        Vec3d tempUp = Math.abs(unitDir.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d perpU = unitDir.crossProduct(tempUp).normalize();
        Vec3d perpV = unitDir.crossProduct(perpU).normalize();

        float baseRadius = beam.getRadius() > 0 ? beam.getRadius() : 0.4F;
        float baseCircleRadius = Math.max(0.5F, baseRadius * 2.4F);

        matrices.push();
        matrices.translate(
                activeVisualStart.x - cameraPos.x,
                activeVisualStart.y - cameraPos.y,
                activeVisualStart.z - cameraPos.z
        );

        // --- 1. RENDER MAGIC CIRCLE ---
        int circleAlpha = 0;
        float circleScale = 1.0F;

        if (beam.isCharging()) {
            float chargeProgress = beam.getChargeProgress();
            // Snap open with cubic ease-out
            circleScale = (float) (1.0 - Math.pow(1.0 - chargeProgress, 3));
            circleAlpha = (int) (Math.min(1.0F, chargeProgress * 1.6F) * 235);
        } else {
            // Firing phase: Circle expands slightly into a shockwave and fades over the first 35%
            float firingProgress = beam.getFiringProgress();
            float fade = Math.max(0.0F, 1.0F - (firingProgress / 0.35F));
            circleAlpha = (int) (fade * 255);
            circleScale = 1.0F + (firingProgress * 0.35F);
        }

        if (circleAlpha > 0) {
            float circleRadius = baseCircleRadius * circleScale;
            Vec3d cPerp1 = perpU.multiply(circleRadius);
            Vec3d cPerp2 = perpV.multiply(circleRadius);

            VertexConsumer circleBuffer = context.consumers().getBuffer(
                    RenderLayer.getEntityTranslucent(MAGIC_CIRCLE_TEXTURE));

            Vector3f axis = new Vector3f((float) unitDir.x, (float) unitDir.y, (float) unitDir.z);

            // Outer primary circle (spins forward)
            matrices.push();
            float spin = (float) (System.currentTimeMillis() % 4000) / 4000.0F * 360.0F;
            matrices.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(spin), axis));
            drawDoubleSidedQuad(circleBuffer, matrices.peek().getPositionMatrix(),
                    cPerp1.add(cPerp2), cPerp1.subtract(cPerp2),
                    cPerp1.multiply(-1).subtract(cPerp2), cPerp1.multiply(-1).add(cPerp2),
                    170, 230, 255, circleAlpha);
            matrices.pop();

            // Inner counter-rotating circle for arcane intricacy
            matrices.push();
            float innerSpin = -(float) (System.currentTimeMillis() % 3000) / 3000.0F * 360.0F;
            float innerRadius = circleRadius * 0.65F;
            Vec3d inPerp1 = perpU.multiply(innerRadius);
            Vec3d inPerp2 = perpV.multiply(innerRadius);
            matrices.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(innerSpin), axis));
            drawDoubleSidedQuad(circleBuffer, matrices.peek().getPositionMatrix(),
                    inPerp1.add(inPerp2), inPerp1.subtract(inPerp2),
                    inPerp1.multiply(-1).subtract(inPerp2), inPerp1.multiply(-1).add(inPerp2),
                    210, 245, 255, (int) (circleAlpha * 0.8F));
            matrices.pop();
        }

        // --- 2. RENDER THE ZOLTRAAK BEAM ---
        if (!beam.isCharging()) {
            float firingProgress = beam.getFiringProgress();
            float alphaRatio = firingProgress < 0.15F
                    ? 1.0F
                    : Math.max(0.0F, 1.0F - ((firingProgress - 0.15F) / 0.85F));
            int beamAlpha = (int) (alphaRatio * 255);

            if (beamAlpha > 0) {
                float dynamicRadius = baseRadius * (1.0F - (firingProgress * 0.15F));

                Vec3d renderDir = activeLookDir.multiply(beam.getRange());
                Matrix4f posMatrix = matrices.peek().getPositionMatrix();
                VertexConsumer beamBuffer = context.consumers().getBuffer(RenderLayer.getLightning());

                // Layer 1: Intense, pure incandescent white core (4 angled fins along beam axis)
                float coreRadius = dynamicRadius * 0.40F;
                int coreAlpha = beamAlpha;
                for (int i = 0; i < 4; i++) {
                    double angle = Math.toRadians(i * 45.0);
                    drawBeamFin(beamBuffer, posMatrix, renderDir, perpU, perpV, angle, coreRadius,
                            255, 255, 255, coreAlpha);
                }

                // Layer 2: Radiant electric-cyan inner bloom (4 angled fins along beam axis, rotated 22.5 deg)
                float innerBloomRadius = dynamicRadius * 0.72F;
                int bloomAlpha = (int) (beamAlpha * 0.88F);
                for (int i = 0; i < 4; i++) {
                    double angle = Math.toRadians(22.5 + (i * 45.0));
                    drawBeamFin(beamBuffer, posMatrix, renderDir, perpU, perpV, angle, innerBloomRadius,
                            175, 235, 255, bloomAlpha);
                }

                // Layer 3: Outer ethereal azure energy shroud (6 angled fins along beam axis)
                float outerRadius = dynamicRadius;
                int shroudAlpha = (int) (beamAlpha * 0.60F);
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 30.0);
                    drawBeamFin(beamBuffer, posMatrix, renderDir, perpU, perpV, angle, outerRadius,
                            90, 195, 255, shroudAlpha);
                }
            }
        }

        matrices.pop();
    }

    private static void drawBeamFin(VertexConsumer buffer, Matrix4f posMatrix, Vec3d renderDir,
                                    Vec3d perpU, Vec3d perpV, double angleRad, float radius,
                                    int red, int green, int blue, int alpha) {
        if (alpha <= 0) return;
        Vec3d offset = perpU.multiply(Math.cos(angleRad) * radius)
                .add(perpV.multiply(Math.sin(angleRad) * radius));
        Vec3d p1 = offset.multiply(-1);
        Vec3d p2 = renderDir.add(p1);
        Vec3d p3 = renderDir.add(offset);
        Vec3d p4 = offset;
        drawDoubleSidedQuad(buffer, posMatrix, p1, p2, p3, p4, red, green, blue, alpha);
    }

    private static void drawDoubleSidedQuad(VertexConsumer buffer, Matrix4f posMatrix,
                                            Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
                                            int red, int green, int blue, int alpha) {
        if (alpha <= 0) return;
        // Front side
        drawVertex(buffer, posMatrix, (float) p1.x, (float) p1.y, (float) p1.z, 0, 0, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p2.x, (float) p2.y, (float) p2.z, 0, 1, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p3.x, (float) p3.y, (float) p3.z, 1, 1, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p4.x, (float) p4.y, (float) p4.z, 1, 0, red, green, blue, alpha);

        // Back side
        drawVertex(buffer, posMatrix, (float) p4.x, (float) p4.y, (float) p4.z, 1, 0, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p3.x, (float) p3.y, (float) p3.z, 1, 1, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p2.x, (float) p2.y, (float) p2.z, 0, 1, red, green, blue, alpha);
        drawVertex(buffer, posMatrix, (float) p1.x, (float) p1.y, (float) p1.z, 0, 0, red, green, blue, alpha);
    }

    private static void drawVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z,
                                   float u, float v, int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Max brightness
                .normal(0, 1, 0);
    }
}
