package com.yuno.yunosbosses.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlackFlashRenderer {

    private static final List<StrikeInstance> ACTIVE_STRIKES = new ArrayList<>();

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(BlackFlashRenderer::render);
    }

    public static void tick() {
        Iterator<StrikeInstance> it = ACTIVE_STRIKES.iterator();
        while (it.hasNext()) {
            StrikeInstance strike = it.next();
            strike.ticksRemaining--;
            if (strike.ticksRemaining <= 0) {
                it.remove();
            }
        }
    }

    public static void spawnStrike(Vec3d pos, Vec3d lookDir, boolean isFinisher, int chainCount) {
        int maxTicks = isFinisher ? 12 : 8;
        int branchCount = isFinisher ? 16 : Math.min(14, 8 + chainCount * 2);
        double maxRadius = isFinisher ? 5.2 : 3.2;

        Random random = new Random();
        List<Branch> branches = new ArrayList<>();

        for (int b = 0; b < branchCount; b++) {
            // Distribute branches outward spherically
            double theta = random.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos((random.nextDouble() * 2.0) - 1.0);
            Vec3d dir = new Vec3d(
                    Math.sin(phi) * Math.cos(theta),
                    Math.sin(phi) * Math.sin(theta),
                    Math.cos(phi)
            ).normalize();

            // Slight forward momentum bias if lookDir is available
            if (lookDir != null && lookDir.lengthSquared() > 0.01) {
                dir = dir.add(lookDir.multiply(0.35)).normalize();
            }

            int numSegments = isFinisher ? 8 : 6;
            double segLength = maxRadius / numSegments;

            List<Vec3d> nodes = new ArrayList<>();
            nodes.add(Vec3d.ZERO);

            Vec3d current = Vec3d.ZERO;
            for (int s = 0; s < numSegments; s++) {
                Vec3d jitter = new Vec3d(
                        (random.nextDouble() - 0.5) * 0.45,
                        (random.nextDouble() - 0.5) * 0.45,
                        (random.nextDouble() - 0.5) * 0.45
                );
                current = current.add(dir.multiply(segLength)).add(jitter);
                nodes.add(current);
            }
            branches.add(new Branch(nodes));

            // Secondary sub-branch fork
            if (random.nextFloat() < 0.45f && nodes.size() > 3) {
                Vec3d forkStart = nodes.get(2);
                Vec3d forkDir = dir.add(
                        (random.nextDouble() - 0.5) * 1.2,
                        (random.nextDouble() - 0.5) * 1.2,
                        (random.nextDouble() - 0.5) * 1.2
                ).normalize();

                List<Vec3d> forkNodes = new ArrayList<>();
                forkNodes.add(forkStart);
                Vec3d fCurrent = forkStart;
                for (int fs = 0; fs < 3; fs++) {
                    fCurrent = fCurrent.add(forkDir.multiply(segLength * 0.7)).add(
                            (random.nextDouble() - 0.5) * 0.3,
                            (random.nextDouble() - 0.5) * 0.3,
                            (random.nextDouble() - 0.5) * 0.3
                    );
                    forkNodes.add(fCurrent);
                }
                branches.add(new Branch(forkNodes));
            }
        }

        ACTIVE_STRIKES.add(new StrikeInstance(pos, maxTicks, maxTicks, isFinisher, branches));
    }

    private static void render(WorldRenderContext context) {
        if (ACTIVE_STRIKES.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.getLightning());

        long time = System.currentTimeMillis();

        for (StrikeInstance strike : ACTIVE_STRIKES) {
            float alphaProgress = (float) strike.ticksRemaining / (float) strike.maxTicks;
            int alpha = (int) (MathHelper.clamp(alphaProgress * 1.25F, 0.0F, 1.0F) * 255);
            if (alpha <= 0) continue;

            matrices.push();
            matrices.translate(
                    strike.origin.x - cameraPos.x,
                    strike.origin.y - cameraPos.y,
                    strike.origin.z - cameraPos.z
            );
            Matrix4f posMatrix = matrices.peek().getPositionMatrix();

            float outerWidth = strike.isFinisher ? 0.22F : 0.14F;
            float innerWidth = strike.isFinisher ? 0.08F : 0.045F;

            // Outer layer: Dark blood-crimson aura
            int outerR = 190;
            int outerG = 8;
            int outerB = 25;
            int outerA = (int) (alpha * 0.85F);

            // Inner layer: Blinding scarlet-white core
            int innerR = 255;
            int innerG = 225;
            int innerB = 235;
            int innerA = alpha;

            for (Branch branch : strike.branches) {
                for (int i = 0; i < branch.nodes.size() - 1; i++) {
                    Vec3d p1 = branch.nodes.get(i);
                    Vec3d p2 = branch.nodes.get(i + 1);

                    // Micro-jitter for violently crackling electricity
                    double jitterSeed = (time * 0.035) + (i * 1.7) + strike.origin.hashCode();
                    Vec3d jitter = new Vec3d(
                            Math.sin(jitterSeed) * 0.035,
                            Math.cos(jitterSeed * 1.3) * 0.035,
                            Math.sin(jitterSeed * 0.7) * 0.035
                    );
                    Vec3d jp1 = (i == 0) ? p1 : p1.add(jitter);
                    Vec3d jp2 = p2.add(jitter);

                    // Billboarded segments facing the camera
                    drawBillboardSegment(buffer, posMatrix, jp1, jp2, strike.origin, cameraPos, outerWidth, outerR, outerG, outerB, outerA);
                    drawBillboardSegment(buffer, posMatrix, jp1, jp2, strike.origin, cameraPos, innerWidth, innerR, innerG, innerB, innerA);
                }
            }

            matrices.pop();
        }
    }

    private static void drawBillboardSegment(VertexConsumer buffer, Matrix4f posMatrix,
                                             Vec3d p1, Vec3d p2, Vec3d strikeOrigin, Vec3d cameraPos,
                                             float width, int r, int g, int b, int a) {
        if (a <= 0) return;
        Vec3d seg = p2.subtract(p1);
        if (seg.lengthSquared() < 1e-6) return;

        Vec3d midWorld = strikeOrigin.add(p1.add(p2).multiply(0.5));
        Vec3d toCamera = cameraPos.subtract(midWorld);

        Vec3d normal = seg.crossProduct(toCamera);
        if (normal.lengthSquared() < 1e-6) {
            normal = seg.crossProduct(new Vec3d(0, 1, 0));
        }
        normal = normal.normalize().multiply(width * 0.5);

        Vec3d q1 = p1.subtract(normal);
        Vec3d q2 = p2.subtract(normal);
        Vec3d q3 = p2.add(normal);
        Vec3d q4 = p1.add(normal);

        drawDoubleSidedQuad(buffer, posMatrix, q1, q2, q3, q4, r, g, b, a);
    }

    private static void drawDoubleSidedQuad(VertexConsumer buffer, Matrix4f posMatrix,
                                            Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
                                            int red, int green, int blue, int alpha) {
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

    private static class StrikeInstance {
        final Vec3d origin;
        final int maxTicks;
        int ticksRemaining;
        final boolean isFinisher;
        final List<Branch> branches;

        StrikeInstance(Vec3d origin, int maxTicks, int ticksRemaining, boolean isFinisher, List<Branch> branches) {
            this.origin = origin;
            this.maxTicks = maxTicks;
            this.ticksRemaining = ticksRemaining;
            this.isFinisher = isFinisher;
            this.branches = branches;
        }
    }

    private record Branch(List<Vec3d> nodes) {}
}
