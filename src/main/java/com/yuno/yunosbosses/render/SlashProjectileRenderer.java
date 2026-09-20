package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
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

public class SlashProjectileRenderer extends EntityRenderer<SlashProjectileEntity, SlashProjectileRenderer.SlashProjectileRenderState> {
    private static final Identifier TEXTURE = Identifier.of("yunosbosses", "textures/entity/slash.png");

    public static class SlashProjectileRenderState extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float roll;
        public float scale;
        public float width;
        public int alpha;
        public boolean isFinisher;
    }

    public SlashProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public SlashProjectileRenderState createRenderState() {
        return new SlashProjectileRenderState();
    }

    @Override
    public void updateRenderState(SlashProjectileEntity entity, SlashProjectileRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.yaw = entity.getYaw(tickDelta);
        state.pitch = entity.getPitch(tickDelta);
        state.roll = entity.getRollAngle();
        state.isFinisher = entity.isFinisher();
        state.width = entity.getSlashWidth();

        float totalAge = entity.age + tickDelta;
        // Projectile travels ~2-3 ticks over 5 blocks
        float progress = Math.min(1.0f, totalAge / 3.0f);

        // Snappy scale: snaps out fast, cuts forward
        state.scale = 1.0f + (float) Math.sin(progress * Math.PI * 0.5) * 0.25f;

        // Fade out in the final 50%
        float fade = progress > 0.5f ? 1.0f - ((progress - 0.5f) / 0.5f) : 1.0f;
        state.alpha = Math.max(0, Math.min(255, (int) (fade * 255)));
    }

    @Override
    public void render(SlashProjectileRenderState state, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        if (state.alpha <= 0) return;

        matrices.push();

        // Orient facing forward along entity trajectory
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));

        // Apply roll angle corresponding to the combo slash plane
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));

        // Apply width and scale
        matrices.scale(state.scale * state.width, state.scale, state.scale);

        // Single entity translucent buffer for all layers to guarantee render pipeline stability
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // 1. Layer: Ethereal Translucent Vacuum Aura (Pale Jade / Turquoise Shimmer)
        drawSlashArc(buffer, posMatrix, (int) (state.alpha * 0.75f), 160, 250, 220, 0.44f, 1.2f);

        // 2. Layer: Inner Razor Incision Core (Brilliant Incandescent White Line)
        drawSlashArc(buffer, posMatrix, state.alpha, 255, 255, 255, 0.14f, 1.25f);

        // If finisher (Cross-Slash), render the crossing blade to form the iconic execution "X"
        if (state.isFinisher) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
            Matrix4f crossMatrix = matrices.peek().getPositionMatrix();

            // Outer shroud and inner core for crossing blade
            drawSlashArc(buffer, crossMatrix, (int) (state.alpha * 0.65f), 160, 250, 220, 0.44f, 1.2f);
            drawSlashArc(buffer, crossMatrix, (int) (state.alpha * 0.90f), 255, 255, 255, 0.14f, 1.25f);

            matrices.pop();
        }

        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    private void drawSlashArc(VertexConsumer buffer, Matrix4f matrix, int alpha,
                              int r, int g, int b, float thickness, float size) {
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;

            Vec3d p1 = getArcPoint(t1, size);
            Vec3d p2 = getArcPoint(t2, size);

            Vec3d dir = p2.subtract(p1).normalize();
            Vec3d perpendicular = new Vec3d(-dir.y, dir.x, 0).multiply(thickness);

            Vec3d v1 = p1.subtract(perpendicular);
            Vec3d v2 = p1.add(perpendicular);
            Vec3d v3 = p2.add(perpendicular);
            Vec3d v4 = p2.subtract(perpendicular);

            // Fade toward tips
            float fade1 = 1.0f - Math.abs(t1 - 0.5f) * 1.8f;
            float fade2 = 1.0f - Math.abs(t2 - 0.5f) * 1.8f;
            int a1 = (int) (alpha * Math.max(0.05f, Math.min(1.0f, fade1)));
            int a2 = (int) (alpha * Math.max(0.05f, Math.min(1.0f, fade2)));

            drawSlashQuad(buffer, matrix, v1, v2, v3, v4, t1, t2, a1, a2, r, g, b);
        }
    }

    private Vec3d getArcPoint(float t, float size) {
        float angle = (t - 0.5f) * (float) Math.PI; // -PI/2 to PI/2
        float x = (float) Math.sin(angle) * size;
        float y = (float) (Math.cos(angle) - 1.0f) * size * 0.55f;
        // Crescent center bows forward along Z (the direction of travel)
        float z = (float) Math.cos(angle) * size * 0.35f;
        return new Vec3d(x, y, z);
    }

    private void drawSlashQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4,
                               float u1, float u2, int a1, int a2, int r, int g, int b) {
        // Front face
        drawVertex(buffer, matrix, v1, u1, 0, r, g, b, a1);
        drawVertex(buffer, matrix, v2, u1, 1, r, g, b, a1);
        drawVertex(buffer, matrix, v3, u2, 1, r, g, b, a2);
        drawVertex(buffer, matrix, v4, u2, 0, r, g, b, a2);

        // Back face (reversed winding order)
        drawVertex(buffer, matrix, v4, u2, 0, r, g, b, a2);
        drawVertex(buffer, matrix, v3, u2, 1, r, g, b, a2);
        drawVertex(buffer, matrix, v2, u1, 1, r, g, b, a1);
        drawVertex(buffer, matrix, v1, u1, 0, r, g, b, a1);
    }

    private void drawVertex(VertexConsumer buffer, Matrix4f matrix, Vec3d pos, float u, float v,
                            int r, int g, int b, int alpha) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Max glowing brightness
                .normal(0, 1, 0);
    }
}
