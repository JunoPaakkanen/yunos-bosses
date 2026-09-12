package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.entity.projectile.FlameArrowEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class FlameArrowRenderer extends EntityRenderer<FlameArrowEntity, FlameArrowRenderer.FlameArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.of("yunosbosses", "textures/entity/flame_arrow.png");

    public static class FlameArrowRenderState extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float roll;
        public float scale;
        public float potency;
        public float age;
    }

    public FlameArrowRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public FlameArrowRenderState createRenderState() {
        return new FlameArrowRenderState();
    }

    @Override
    public void updateRenderState(FlameArrowEntity entity, FlameArrowRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.yaw = entity.getYaw(tickDelta);
        state.pitch = entity.getPitch(tickDelta);

        float totalAge = entity.age + tickDelta;
        state.age = totalAge;
        state.roll = totalAge * 36.0f; // High-speed spin along flight axis
        state.potency = entity.getPotency();
        state.scale = 1.0f + (state.potency - 1.0f) * 0.25f;
    }

    @Override
    public void render(FlameArrowRenderState state, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Rotate arrow to point in the flight direction
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));

        // High-speed axial spin
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));

        // Scale based on potency
        matrices.scale(state.scale, state.scale, state.scale);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));

        float length = 0.95f;
        float halfWidth = 0.24f;

        // Draw 4 intersecting planes (0°, 45°, 90°, 135°) to form a dense 8-point volumetric fiery arrow
        for (int i = 0; i < 4; i++) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 45.0f));
            Matrix4f planeMatrix = matrices.peek().getPositionMatrix();

            // 1. Core Arrow Shaft & Head Quad
            drawArrowQuad(buffer, planeMatrix, length, halfWidth, 255, 250, 180, 255);

            // 2. Pulsating outer fire corona
            float auraWidth = halfWidth * 1.6f;
            float pulse = (float) (0.65 + 0.25 * Math.sin(state.age * 0.8f + i * 1.2f));
            int alphaInt = Math.clamp((int) (pulse * 255), 0, 255);
            drawArrowQuad(buffer, planeMatrix, length * 1.08f, auraWidth, 255, 110, 20, alphaInt);

            // 3. Trailing Flame Wake Ribbon stretching backwards behind the arrow
            float tailLength = length * 2.6f;
            float flickerTail = auraWidth * (float) (0.8 + 0.3 * Math.sin(state.age * 1.4f + i));
            drawTailRibbon(buffer, planeMatrix, -length, -(length + tailLength), halfWidth, flickerTail);

            matrices.pop();
        }

        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    private void drawArrowQuad(VertexConsumer buffer, Matrix4f matrix, float length, float halfWidth,
                               int r, int g, int b, int alpha) {
        Vec3d p1 = new Vec3d(-halfWidth, 0, -length);
        Vec3d p2 = new Vec3d(halfWidth, 0, -length);
        Vec3d p3 = new Vec3d(halfWidth, 0, length);
        Vec3d p4 = new Vec3d(-halfWidth, 0, length);

        // Front Face
        drawVertex(buffer, matrix, p1, 0.0f, 0.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p2, 0.0f, 1.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p3, 1.0f, 1.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p4, 1.0f, 0.0f, r, g, b, alpha);

        // Back Face
        drawVertex(buffer, matrix, p4, 1.0f, 0.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p3, 1.0f, 1.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p2, 0.0f, 1.0f, r, g, b, alpha);
        drawVertex(buffer, matrix, p1, 0.0f, 0.0f, r, g, b, alpha);
    }

    private void drawTailRibbon(VertexConsumer buffer, Matrix4f matrix, float startZ, float endZ,
                                float startWidth, float endWidth) {
        Vec3d p1 = new Vec3d(-endWidth, 0, endZ);
        Vec3d p2 = new Vec3d(endWidth, 0, endZ);
        Vec3d p3 = new Vec3d(startWidth, 0, startZ);
        Vec3d p4 = new Vec3d(-startWidth, 0, startZ);

        // Tail ribbon fades to 0 alpha at the far end
        drawVertex(buffer, matrix, p1, 0.0f, 0.0f, 255, 60, 10, 0);
        drawVertex(buffer, matrix, p2, 0.0f, 1.0f, 255, 60, 10, 0);
        drawVertex(buffer, matrix, p3, 0.5f, 1.0f, 255, 140, 20, 210);
        drawVertex(buffer, matrix, p4, 0.5f, 0.0f, 255, 140, 20, 210);

        // Back face
        drawVertex(buffer, matrix, p4, 0.5f, 0.0f, 255, 140, 20, 210);
        drawVertex(buffer, matrix, p3, 0.5f, 1.0f, 255, 140, 20, 210);
        drawVertex(buffer, matrix, p2, 0.0f, 1.0f, 255, 60, 10, 0);
        drawVertex(buffer, matrix, p1, 0.0f, 0.0f, 255, 60, 10, 0);
    }

    private void drawVertex(VertexConsumer buffer, Matrix4f matrix, Vec3d pos, float u, float v,
                            int r, int g, int b, int alpha) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Full glowing brightness in all environments
                .normal(0, 1, 0);
    }
}
