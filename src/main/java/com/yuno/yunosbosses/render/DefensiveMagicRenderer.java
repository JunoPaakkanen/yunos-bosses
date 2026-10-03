package com.yuno.yunosbosses.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuno.yunosbosses.spell.implementation.defensive.SphereBarrierSpell;
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
import java.util.Random;

public class DefensiveMagicRenderer {
    private static final Identifier HEX_TEXTURE = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");
    private static final Identifier BARRIER_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier.png");
    private static final Identifier MAGIC_CIRCLE_TEXTURE = Identifier.of("yunosbosses", "textures/effect/magic_circle.png");
    private static final Identifier DOMAIN_EXTERIOR = Identifier.of("yunosbosses", "textures/domain_expansion/domain_exterior.png");
    private static final Identifier SLASH_TEXTURE = Identifier.of("yunosbosses", "textures/entity/slash.png");

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            Vec3d cameraPos = context.camera().getPos();

            for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
                // If direction is ZERO, treat it as a Sphere or Open Domain
                if (barrier.getDirection().equals(Vec3d.ZERO)) {
                    boolean isShrine = barrier.getTexture() != null && barrier.getTexture().getPath().contains("shrine");

                    if (isShrine) {
                        // Both Open and Closed Malevolent Shrine share identical visuals:
                        // red perimeter boundary cylinder, luminous ground fissures, and 3D slashes
                        renderOpenDomainShrine(matrices, consumers, cameraPos, barrier, barrier.getRadius(), context);
                    } else if (barrier.getDomainExpansion() != null) {
                        // Closed Domain Expansion (opaque black exterior shell, inner skybox)
                        renderDomainSphere(matrices, consumers, cameraPos, barrier, barrier.getRadius());
                    } else {
                        // Magical Spherical Defensive Barrier
                        renderMagicalSphereBarrier(matrices, consumers, cameraPos, barrier, barrier.getRadius(), context);
                    }
                } else {
                    // Otherwise, render the directional hex shield
                    renderBarrier(matrices, consumers, cameraPos, barrier, context);
                }
            }
        });
    }

    // --- MALEVOLENT SHRINE RENDERER (SHARED FOR BOTH OPEN AND CLOSED BARRIERS) ---
    // Open to the world sky, marked by a blood-red perimeter boundary cylinder, 3D slashes, and luminous fissures
    private static void renderOpenDomainShrine(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, float radius, WorldRenderContext context) {
        matrices.push();

        Vec3d pos = barrier.getPosition();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);

        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        long time = context.world().getTime();

        int segments = 64;
        float floorY = -2.0f; // Domain floor relative to barrier center
        float wallHeight = 6.0f;
        int pulsingAlpha = (int) (120 + 40 * Math.sin(time * 0.15));

        // --- 1. PERIMETER BOUNDARY CYLINDER (Demarcation Line of Cleave & Dismantle) ---
        VertexConsumer boundaryBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER_TEXTURE));
        float uOffset = (time % 100) / 100.0f;

        for (int j = 0; j < segments; j++) {
            float angle0 = (float) (2 * Math.PI * (float) j / segments);
            float angle1 = (float) (2 * Math.PI * (float) (j + 1) / segments);

            float x0 = (float) Math.cos(angle0) * radius;
            float z0 = (float) Math.sin(angle0) * radius;
            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;

            float u0 = ((float) j / segments) * 4.0f + uOffset;
            float u1 = ((float) (j + 1) / segments) * 4.0f + uOffset;

            // Outer face
            boundaryBuffer.vertex(modelMatrix, x0, floorY, z0).color(230, 20, 30, pulsingAlpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x1, floorY, z1).color(230, 20, 30, pulsingAlpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x1, floorY + wallHeight, z1).color(180, 10, 20, 0).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x0, floorY + wallHeight, z0).color(180, 10, 20, 0).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

            // Inner face (reverse winding)
            boundaryBuffer.vertex(modelMatrix, x0, floorY + wallHeight, z0).color(180, 10, 20, 0).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x1, floorY + wallHeight, z1).color(180, 10, 20, 0).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x1, floorY, z1).color(230, 20, 30, pulsingAlpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, x0, floorY, z0).color(230, 20, 30, pulsingAlpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        }

        // --- 2. GROUND BOUNDARY SEAL RING ---
        float innerRingR = radius - 0.6f;
        float outerRingR = radius + 0.6f;
        float ringY = floorY + 0.05f;

        for (int j = 0; j < segments; j++) {
            float angle0 = (float) (2 * Math.PI * (float) j / segments);
            float angle1 = (float) (2 * Math.PI * (float) (j + 1) / segments);

            float inX0 = (float) Math.cos(angle0) * innerRingR;
            float inZ0 = (float) Math.sin(angle0) * innerRingR;
            float outX0 = (float) Math.cos(angle0) * outerRingR;
            float outZ0 = (float) Math.sin(angle0) * outerRingR;

            float inX1 = (float) Math.cos(angle1) * innerRingR;
            float inZ1 = (float) Math.sin(angle1) * innerRingR;
            float outX1 = (float) Math.cos(angle1) * outerRingR;
            float outZ1 = (float) Math.sin(angle1) * outerRingR;

            boundaryBuffer.vertex(modelMatrix, inX0, ringY, inZ0).color(255, 40, 50, pulsingAlpha + 30).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, inX1, ringY, inZ1).color(255, 40, 50, pulsingAlpha + 30).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, outX1, ringY, outZ1).color(220, 10, 20, 0).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            boundaryBuffer.vertex(modelMatrix, outX0, ringY, outZ0).color(220, 10, 20, 0).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        }

        // --- 3. RAPID 3D MID-AIR CLEAVE SLASHES ---
        VertexConsumer slashBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(SLASH_TEXTURE));
        long slashSeed = (time / 2) + barrier.getOwnerUuid().hashCode();
        Random slashRand = new Random(slashSeed);

        int slashCount = 10;
        for (int s = 0; s < slashCount; s++) {
            float dist = (slashRand.nextFloat() * 0.85f) * radius;
            double slashAngle = slashRand.nextDouble() * 2.0 * Math.PI;
            float sx = (float) (Math.cos(slashAngle) * dist);
            float sz = (float) (Math.sin(slashAngle) * dist);
            float sy = floorY + 1.0f + (slashRand.nextFloat() * (radius * 0.6f));

            float slashLen = 3.5f + slashRand.nextFloat() * 4.5f;
            float slashYaw = slashRand.nextFloat() * 360.0f;
            float slashPitch = (slashRand.nextFloat() - 0.5f) * 60.0f;
            float slashRoll = (slashRand.nextFloat() - 0.5f) * 90.0f;

            matrices.push();
            matrices.translate(sx, sy, sz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(slashYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(slashPitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(slashRoll));

            Matrix4f slashMatrix = matrices.peek().getPositionMatrix();
            float halfLen = slashLen / 2.0f;
            float halfWidth = 0.35f;

            // Stark white cutting blade with crimson glow
            slashBuffer.vertex(slashMatrix, -halfLen, 0, -halfWidth).color(255, 255, 255, 230).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, halfLen, 0, -halfWidth).color(255, 255, 255, 230).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, halfLen, 0, halfWidth).color(255, 50, 60, 230).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, -halfLen, 0, halfWidth).color(255, 50, 60, 230).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

            // Double sided
            slashBuffer.vertex(slashMatrix, -halfLen, 0, halfWidth).color(255, 50, 60, 230).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, halfLen, 0, halfWidth).color(255, 50, 60, 230).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, halfLen, 0, -halfWidth).color(255, 255, 255, 230).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            slashBuffer.vertex(slashMatrix, -halfLen, 0, -halfWidth).color(255, 255, 255, 230).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

            matrices.pop();
        }

        // --- 4. RADIATING GROUND SLASH SCARS (MALEVOLENT SHRINE TECTONIC FISSURES WITH LUMINESCENT GLOW) ---
        renderGroundSlashScars(slashBuffer, modelMatrix, barrier, radius, floorY, time);

        matrices.pop();
    }

    // Renders glowing radiating slash scars and laceration fissures etched into the earth around Malevolent Shrine
    private static void renderGroundSlashScars(VertexConsumer buffer, Matrix4f modelMatrix, ActiveBarrier barrier, float radius, float floorY, long time) {
        int fissureCount = 20;

        // Growth expansion during the first 25 ticks of the domain
        int elapsedTicks = Math.max(1, barrier.getCurrentTicks());
        float growth = Math.min(1.0f, elapsedTicks / 25.0f);

        for (int k = 0; k < fissureCount; k++) {
            long seed = barrier.getOwnerUuid().hashCode() * 31L + (k * 1337L);
            Random fissureRand = new Random(seed);

            double baseAngle = (k * 2.0 * Math.PI / fissureCount) + (fissureRand.nextDouble() - 0.5) * 0.22;
            float maxDist = radius * (0.42f + fissureRand.nextFloat() * 0.46f);
            float fissureLen = maxDist * growth;

            if (fissureLen <= 1.0f) continue;

            // Start slightly outside the shrine center pillar
            double currX = Math.cos(baseAngle) * 1.5;
            double currZ = Math.sin(baseAngle) * 1.5;
            double currentAngle = baseAngle;

            int segments = 6;
            float segStep = fissureLen / segments;

            // Dynamic pulsing luminescence for this fissure
            int haloPulse = (int) (30 * Math.sin(time * 0.10 + k * 1.3));
            int glowPulse = (int) (35 * Math.sin(time * 0.15 + k * 1.7));
            int haloAlpha = Math.max(0, Math.min(255, 115 + haloPulse));
            int glowAlpha = Math.max(0, Math.min(255, 205 + glowPulse));
            int coreAlpha = Math.max(0, Math.min(255, 245 + (int) (10 * Math.sin(time * 0.2 + k))));
            int vaporAlpha = Math.max(0, Math.min(255, 65 + (int) (25 * Math.sin(time * 0.12 + k))));

            for (int s = 0; s < segments; s++) {
                // Introduce organic jagged fracture meandering
                currentAngle += (fissureRand.nextDouble() - 0.5) * 0.28;

                double nextX = currX + Math.cos(currentAngle) * segStep;
                double nextZ = currZ + Math.sin(currentAngle) * segStep;

                double dx = nextX - currX;
                double dz = nextZ - currZ;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.001) continue;

                float perpX = (float) (-dz / len);
                float perpZ = (float) (dx / len);

                float segProgress = (float) s / segments;
                float taper = 1.0f - (segProgress * 0.45f);

                // --- PASS 0: LUMINESCENT AMBIENT HALO (Soft glowing fringe spreading into stone) ---
                float haloWidth = 0.58f * taper;
                drawGroundQuad(buffer, modelMatrix, currX, currZ, nextX, nextZ, perpX, perpZ, haloWidth, floorY + 0.025f, 255, 25, 40, haloAlpha);

                // --- PASS 1: VIBRANT CURSED ENERGY CREVICE (Vivid blood-red slash body) ---
                float glowWidth = 0.26f * taper;
                drawGroundQuad(buffer, modelMatrix, currX, currZ, nextX, nextZ, perpX, perpZ, glowWidth, floorY + 0.040f, 255, 50, 65, glowAlpha);

                // --- PASS 2: INCANDESCENT RAZOR CORE (White-hot glowing center line) ---
                float coreWidth = 0.07f * taper;
                drawGroundQuad(buffer, modelMatrix, currX, currZ, nextX, nextZ, perpX, perpZ, coreWidth, floorY + 0.055f, 255, 250, 255, coreAlpha);

                // --- PASS 3: VOLUMETRIC VERTICAL HEAT/VAPOR GLOW (Radiates upward from the crack) ---
                drawVerticalGlow(buffer, modelMatrix, currX, currZ, nextX, nextZ, floorY + 0.025f, 0.32f, 255, 30, 45, vaporAlpha);

                // Branching Sub-Cuts (forks in the scar)
                if (s == 2 && fissureRand.nextBoolean()) {
                    double branchAngle = currentAngle + (fissureRand.nextBoolean() ? 0.6 : -0.6);
                    float branchLen = segStep * 1.6f * growth;
                    double branchX = nextX + Math.cos(branchAngle) * branchLen;
                    double branchZ = nextZ + Math.sin(branchAngle) * branchLen;

                    double bdx = branchX - nextX;
                    double bdz = branchZ - nextZ;
                    double blen = Math.sqrt(bdx * bdx + bdz * bdz);
                    if (blen > 0.001) {
                        float bperpX = (float) (-bdz / blen);
                        float bperpZ = (float) (bdx / blen);
                        drawGroundQuad(buffer, modelMatrix, nextX, nextZ, branchX, branchZ, bperpX, bperpZ, haloWidth * 0.7f, floorY + 0.025f, 255, 20, 35, haloAlpha - 25);
                        drawGroundQuad(buffer, modelMatrix, nextX, nextZ, branchX, branchZ, bperpX, bperpZ, glowWidth * 0.7f, floorY + 0.040f, 255, 45, 60, glowAlpha - 20);
                        drawGroundQuad(buffer, modelMatrix, nextX, nextZ, branchX, branchZ, bperpX, bperpZ, coreWidth * 0.7f, floorY + 0.055f, 255, 250, 255, coreAlpha - 10);
                        drawVerticalGlow(buffer, modelMatrix, nextX, nextZ, branchX, branchZ, floorY + 0.025f, 0.22f, 255, 30, 45, vaporAlpha - 20);
                    }
                }

                currX = nextX;
                currZ = nextZ;
            }
        }
    }

    private static void drawGroundQuad(VertexConsumer buffer, Matrix4f modelMatrix, double x1, double z1, double x2, double z2, float perpX, float perpZ, float halfWidth, float y, int r, int g, int b, int alpha) {
        float x1a = (float) (x1 - perpX * halfWidth);
        float z1a = (float) (z1 - perpZ * halfWidth);
        float x1b = (float) (x1 + perpX * halfWidth);
        float z1b = (float) (z1 + perpZ * halfWidth);

        float x2a = (float) (x2 - perpX * halfWidth);
        float z2a = (float) (z2 - perpZ * halfWidth);
        float x2b = (float) (x2 + perpX * halfWidth);
        float z2b = (float) (z2 + perpZ * halfWidth);

        // Top Face (pointing Up into camera)
        buffer.vertex(modelMatrix, x1a, y, z1a).color(r, g, b, alpha).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2a, y, z2a).color(r, g, b, alpha).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2b, y, z2b).color(r, g, b, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x1b, y, z1b).color(r, g, b, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

        // Bottom Face (reverse winding)
        buffer.vertex(modelMatrix, x1b, y, z1b).color(r, g, b, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2b, y, z2b).color(r, g, b, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2a, y, z2a).color(r, g, b, alpha).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x1a, y, z1a).color(r, g, b, alpha).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
    }

    // Volumetric vertical glow quad rising upward from the fissure crevice
    private static void drawVerticalGlow(VertexConsumer buffer, Matrix4f modelMatrix, double x1, double z1, double x2, double z2, float yBottom, float height, int r, int g, int b, int alpha) {
        float x1f = (float) x1;
        float z1f = (float) z1;
        float x2f = (float) x2;
        float z2f = (float) z2;

        // Front Face (fade to 0 alpha at top)
        buffer.vertex(modelMatrix, x1f, yBottom, z1f).color(r, g, b, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2f, yBottom, z2f).color(r, g, b, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2f, yBottom + height, z2f).color(r, g, b, 0).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x1f, yBottom + height, z1f).color(r, g, b, 0).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

        // Reverse Face
        buffer.vertex(modelMatrix, x1f, yBottom + height, z1f).color(r, g, b, 0).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2f, yBottom + height, z2f).color(r, g, b, 0).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x2f, yBottom, z2f).color(r, g, b, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(modelMatrix, x1f, yBottom, z1f).color(r, g, b, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
    }

    // --- MAGICAL DEFENSIVE SPHERICAL BARRIER (REVAMPED) ---
    // Translucent glowing azure bubble, smooth expansion bloom, dual-layer counter-rotating runic circles, and feet-level energy bands
    private static void renderMagicalSphereBarrier(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, float targetRadius, WorldRenderContext context) {
        matrices.push();

        Vec3d pos = barrier.getPosition();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);

        long time = context.world().getTime();
        float tickDelta = context.tickCounter().getTickProgress(false);
        float currentTicks = barrier.getCurrentTicks() + tickDelta;
        int remainingTicks = barrier.getMaxTicks() - barrier.getCurrentTicks();

        // 1. Dynamic expansion animation (first 10 ticks: snappy ease-out expansion with over-bounce)
        float expansionProgress = Math.min(1.0F, currentTicks / 10.0F);
        float scaleMultiplier = (float) Math.sin(expansionProgress * Math.PI * 0.5F);
        if (expansionProgress < 1.0F) {
            scaleMultiplier += (float) Math.sin(expansionProgress * Math.PI) * 0.06F;
        }

        // 2. Fade out animation during final 20 ticks
        float fadeMultiplier = 1.0F;
        if (remainingTicks < 20) {
            fadeMultiplier = Math.max(0.0F, (float) remainingTicks / 20.0F);
        }

        float radius = targetRadius * scaleMultiplier;
        if (radius <= 0.05F) {
            matrices.pop();
            return;
        }

        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();

        // Pulsing luminescence
        float pulse = (float) Math.sin((time + tickDelta) * 0.12F);
        int shellAlpha = (int) (Math.max(15, Math.min(115, 60 + pulse * 20)) * fadeMultiplier);

        int segments = 40;

        // --- LAYER 1: TRANSLUCENT AZURE SPHERE SHELL (Double-sided rendering) ---
        VertexConsumer shellBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER_TEXTURE));

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

                float u0 = ((float) j / segments) * 2.0F;
                float u1 = ((float) (j + 1) / segments) * 2.0F;
                float v0 = 1.0f - ((float) i / segments);
                float v1 = 1.0f - ((float) (i + 1) / segments);

                float px1 = x0 * yr0 * radius, py1 = y0 * radius, pz1 = z0 * yr0 * radius;
                float px2 = x1 * yr0 * radius, py2 = y0 * radius, pz2 = z1 * yr0 * radius;
                float px3 = x1 * yr1 * radius, py3 = y1 * radius, pz3 = z1 * yr1 * radius;
                float px4 = x0 * yr1 * radius, py4 = y1 * radius, pz4 = z0 * yr1 * radius;

                // Cyan/Azure barrier tint: R=130, G=225, B=255
                // Outward winding
                shellBuffer.vertex(modelMatrix, px4, py4, pz4).color(130, 225, 255, shellAlpha).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px3, py3, pz3).color(130, 225, 255, shellAlpha).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px2, py2, pz2).color(130, 225, 255, shellAlpha).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px1, py1, pz1).color(130, 225, 255, shellAlpha).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

                // Inward winding (viewable from inside)
                shellBuffer.vertex(modelMatrix, px1, py1, pz1).color(130, 225, 255, shellAlpha).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px2, py2, pz2).color(130, 225, 255, shellAlpha).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px3, py3, pz3).color(130, 225, 255, shellAlpha).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
                shellBuffer.vertex(modelMatrix, px4, py4, pz4).color(130, 225, 255, shellAlpha).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            }
        }

        // Center of sphere is at caster.getPos().add(0, 1.2, 0).
        // Caster's feet are 1.2 blocks below center (y = -1.2 relative to center).
        float feetY = -1.2F;

        // --- LAYER 2: GLOWING ENERGY BANDS AT CASTER FEET LEVEL ---
        VertexConsumer bandBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER_TEXTURE));
        int bandAlpha = (int) ((110 + pulse * 35) * fadeMultiplier);
        float bandWidth = 0.35F + (radius * 0.02F);

        // Render energy band around the sphere at caster's feet level (one block down from eye level)
        float feetLatAngle = (float) Math.asin(Math.max(-0.95, Math.min(0.95, feetY / radius)));
        renderLatitudinalBand(bandBuffer, modelMatrix, radius, bandWidth, feetLatAngle, (time + tickDelta) * 0.03F, bandAlpha);

        // --- LAYER 3: ROTATING MAGICAL RUNIC CIRCLES (magic_circle.png) AT CASTER FEET LEVEL ---
        VertexConsumer circleBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(MAGIC_CIRCLE_TEXTURE));
        int circleAlpha = (int) ((140 + pulse * 45) * fadeMultiplier);

        // Primary ground seal disk at caster feet level (feetY + 0.03 to sit cleanly on floor)
        float feetDiskRadius = (float) Math.sqrt(Math.max(1.0, (radius * radius) - (feetY * feetY)));
        float rotBase = (time + tickDelta) * 1.2F;
        renderRunicDisk(matrices, circleBuffer, feetDiskRadius, feetY + 0.03F, rotBase, circleAlpha);

        // Secondary counter-rotating concentric seal disk slightly above feet for rich magical depth
        float rotMid = -(time + tickDelta) * 0.8F;
        renderRunicDisk(matrices, circleBuffer, feetDiskRadius * 0.70F, feetY + 0.08F, rotMid, (int) (circleAlpha * 0.65F));

        matrices.pop();
    }

    private static void renderRunicDisk(MatrixStack matrices, VertexConsumer buffer, float radius, float y, float rotDegrees, int alpha) {
        matrices.push();
        matrices.translate(0, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotDegrees));

        Matrix4f mat = matrices.peek().getPositionMatrix();

        // Top Face (pointing Up)
        buffer.vertex(mat, -radius, 0, -radius).color(120, 220, 255, alpha).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(mat, -radius, 0, radius).color(120, 220, 255, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(mat, radius, 0, radius).color(120, 220, 255, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        buffer.vertex(mat, radius, 0, -radius).color(120, 220, 255, alpha).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

        // Bottom Face (pointing Down)
        buffer.vertex(mat, radius, 0, -radius).color(120, 220, 255, alpha).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, -1, 0);
        buffer.vertex(mat, radius, 0, radius).color(120, 220, 255, alpha).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, -1, 0);
        buffer.vertex(mat, -radius, 0, radius).color(120, 220, 255, alpha).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, -1, 0);
        buffer.vertex(mat, -radius, 0, -radius).color(120, 220, 255, alpha).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, -1, 0);

        matrices.pop();
    }

    private static void renderLatitudinalBand(VertexConsumer buffer, Matrix4f modelMatrix, float sphereRadius, float bandWidth, float latAngle, float uOffset, int alpha) {
        int segs = 36;
        float yCenter = (float) Math.sin(latAngle) * sphereRadius;
        float ringRadius = (float) Math.cos(latAngle) * (sphereRadius + 0.02F);

        float y0 = yCenter - bandWidth * 0.5F;
        float y1 = yCenter + bandWidth * 0.5F;

        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2.0 * Math.PI * i / segs);
            float a1 = (float) (2.0 * Math.PI * (i + 1) / segs);

            float x0 = (float) Math.cos(a0) * ringRadius;
            float z0 = (float) Math.sin(a0) * ringRadius;
            float x1 = (float) Math.cos(a1) * ringRadius;
            float z1 = (float) Math.sin(a1) * ringRadius;

            float u0 = ((float) i / segs) * 2.0F + uOffset;
            float u1 = ((float) (i + 1) / segs) * 2.0F + uOffset;

            buffer.vertex(modelMatrix, x0, y0, z0).color(140, 230, 255, alpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y0, z1).color(140, 230, 255, alpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y1, z1).color(140, 230, 255, alpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x0, y1, z0).color(140, 230, 255, alpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

            buffer.vertex(modelMatrix, x0, y1, z0).color(140, 230, 255, alpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y1, z1).color(140, 230, 255, alpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y0, z1).color(140, 230, 255, alpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x0, y0, z0).color(140, 230, 255, alpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        }
    }

    // Domain Expansion Closed Sphere (Solid black shell exterior, inner texture skybox)
    private static void renderDomainSphere(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, ActiveBarrier barrier, float radius) {
        Identifier texture = barrier.getTexture() != null ? barrier.getTexture() : BARRIER_TEXTURE;

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

                // Flipped winding sequence (4 -> 3 -> 2 -> 1) forces the solid shell to face OUTWARD
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

                // Standard winding sequence (1 -> 2 -> 3 -> 4) forces the glowing skybox to face INWARD
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
