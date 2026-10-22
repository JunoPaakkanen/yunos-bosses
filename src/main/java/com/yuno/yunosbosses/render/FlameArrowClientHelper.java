package com.yuno.yunosbosses.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlameArrowClientHelper {
    private static final Identifier BARRIER_WHITE_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier_white.png");

    public static class ShockwaveRingWave {
        private final Vec3d center;
        private final float startRadius;
        private final float maxRadius;
        private final int delayTicks;
        private final int durationTicks;
        private final boolean isThermobaric;
        private int ageTicks = 0;

        public ShockwaveRingWave(Vec3d center, float startRadius, float maxRadius, int delayTicks, int durationTicks, boolean isThermobaric) {
            this.center = center;
            this.startRadius = startRadius;
            this.maxRadius = maxRadius;
            this.delayTicks = delayTicks;
            this.durationTicks = durationTicks;
            this.isThermobaric = isThermobaric;
        }

        public void tick(ClientWorld world) {
            this.ageTicks++;
            int activeTick = this.ageTicks - this.delayTicks;
            if (activeTick >= 0 && activeTick < this.durationTicks && world != null) {
                float progress = (float) activeTick / (float) this.durationTicks;
                float eased = 1.0F - (float) Math.pow(1.0F - progress, 1.55F);
                float currentR = this.startRadius + (this.maxRadius - this.startRadius) * eased;

                // Accompany the expanding orange circle wave with synced flame particles along its edge
                if (progress < 0.85F) {
                    int points = Math.min(48, Math.max(12, (int) (currentR * (this.isThermobaric ? 2.2F : 2.8F))));
                    double angleOffset = activeTick * 0.23;
                    for (int i = 0; i < points; i++) {
                        double angle = (2.0 * Math.PI * i) / points + angleOffset;
                        double rx = Math.cos(angle);
                        double rz = Math.sin(angle);
                        double px = this.center.x + rx * currentR;
                        double pz = this.center.z + rz * currentR;
                        world.addParticleClient(ParticleTypes.FLAME, px, this.center.y + 0.25, pz, rx * 0.18, 0.03, rz * 0.18);
                    }
                }
            }
        }

        public boolean isStarted(float tickDelta) {
            return (this.ageTicks + tickDelta) >= this.delayTicks;
        }

        public boolean isExpired() {
            return (this.ageTicks - this.delayTicks) >= this.durationTicks;
        }

        public float getProgress(float tickDelta) {
            float activeAge = (this.ageTicks + tickDelta) - this.delayTicks;
            return Math.max(0.0F, Math.min(1.0F, activeAge / (float) this.durationTicks));
        }

        public float getCurrentRadius(float tickDelta) {
            float progress = getProgress(tickDelta);
            // Ease-out curve matching the outward deceleration of flame shockwave particles
            float eased = 1.0F - (float) Math.pow(1.0F - progress, 1.55F);
            return this.startRadius + (this.maxRadius - this.startRadius) * eased;
        }

        public Vec3d getCenter() { return center; }
        public float getMaxRadius() { return maxRadius; }
        public boolean isThermobaric() { return isThermobaric; }
    }

    public static final List<ShockwaveRingWave> ACTIVE_SHOCKWAVE_WAVES = new CopyOnWriteArrayList<>();

    public static void registerRenderer() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!ACTIVE_SHOCKWAVE_WAVES.isEmpty()) {
                renderFlameShockwaveCircles(context.matrixStack(), context.consumers(), context.camera().getPos(), context);
            }
        });
    }

    public static void onThermobaricDetonation() {
        ShaderManager.triggerFlash(8);
    }

    public static void addShockwaveWaves(Vec3d center, float maxRadius, boolean isThermobaric) {
        int waveCount = isThermobaric ? 4 : 3;
        int waveDelayStep = 4; // 4 ticks between each expanding wave
        int waveDuration = isThermobaric ? 22 : 18;
        float startRadius = 1.2F;

        for (int w = 0; w < waveCount; w++) {
            ACTIVE_SHOCKWAVE_WAVES.add(new ShockwaveRingWave(
                    center,
                    startRadius,
                    maxRadius,
                    w * waveDelayStep,
                    waveDuration,
                    isThermobaric
            ));
        }
    }

    public static void tick(ClientWorld world) {
        for (int i = ACTIVE_SHOCKWAVE_WAVES.size() - 1; i >= 0; i--) {
            if (i >= ACTIVE_SHOCKWAVE_WAVES.size()) continue;
            ShockwaveRingWave wave = ACTIVE_SHOCKWAVE_WAVES.get(i);
            wave.tick(world);
            if (wave.isExpired()) {
                ACTIVE_SHOCKWAVE_WAVES.remove(wave);
            }
        }
    }

    // --- FLAME ARROW EXPANDING GLOWING ORANGE EDGE CIRCLES (NON-SPINNING) ---
    private static void renderFlameShockwaveCircles(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, WorldRenderContext context) {
        float tickDelta = context.tickCounter().getTickProgress(false);
        VertexConsumer bandBuffer = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER_WHITE_TEXTURE));

        for (ShockwaveRingWave wave : ACTIVE_SHOCKWAVE_WAVES) {
            if (!wave.isStarted(tickDelta) || wave.isExpired()) continue;

            float progress = wave.getProgress(tickDelta);
            float currentRadius = wave.getCurrentRadius(tickDelta);
            if (currentRadius <= 0.1F) continue;

            // Fade out smoothly as the circle approaches maximum expansion
            float fade = 1.0F;
            if (progress < 0.12F) {
                fade = progress / 0.12F;
            } else if (progress > 0.55F) {
                float fadeOutProgress = (progress - 0.55F) / 0.45F;
                fade = (float) Math.cos(fadeOutProgress * Math.PI * 0.5F);
            }
            fade = Math.max(0.0F, Math.min(1.0F, fade));
            if (fade <= 0.01F) continue;

            matrices.push();
            Vec3d center = wave.getCenter();
            matrices.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            Matrix4f modelMatrix = matrices.peek().getPositionMatrix();

            float bandWidth = 0.32F + (currentRadius * 0.014F);
            int segs = Math.min(64, Math.max(36, (int) (currentRadius * 3.5F)));

            // Outer bright glowing orange edge circle (uOffset = 0.0F -> strictly non-spinning)
            int outerAlpha = (int) (215 * fade);
            renderEdgeCircleBand(bandBuffer, modelMatrix, currentRadius, 0.25F, bandWidth, 0.0F, 255, 120, 20, outerAlpha, segs);

            // Inner incandescent hot orange-gold core circle for extra glow
            int coreAlpha = (int) (165 * fade);
            renderEdgeCircleBand(bandBuffer, modelMatrix, Math.max(0.1F, currentRadius - 0.03F), 0.25F, bandWidth * 0.48F, 0.0F, 255, 215, 75, coreAlpha, segs);

            matrices.pop();
        }
    }

    private static void renderEdgeCircleBand(VertexConsumer buffer, Matrix4f modelMatrix, float ringRadius, float yCenter, float bandWidth, float uOffset, int r, int g, int b, int alpha, int segs) {
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

            // Outward face
            buffer.vertex(modelMatrix, x0, y0, z0).color(r, g, b, alpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y0, z1).color(r, g, b, alpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y1, z1).color(r, g, b, alpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x0, y1, z0).color(r, g, b, alpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);

            // Inward face (double-sided)
            buffer.vertex(modelMatrix, x0, y1, z0).color(r, g, b, alpha).texture(u0, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y1, z1).color(r, g, b, alpha).texture(u1, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x1, y0, z1).color(r, g, b, alpha).texture(u1, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
            buffer.vertex(modelMatrix, x0, y0, z0).color(r, g, b, alpha).texture(u0, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0, 1, 0);
        }
    }
}
