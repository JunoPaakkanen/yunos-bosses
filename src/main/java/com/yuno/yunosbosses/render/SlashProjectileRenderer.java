package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class SlashProjectileRenderer extends EntityRenderer<SlashProjectileEntity> {
    private static final Identifier TEXTURE = Identifier.of("yunosbosses", "textures/entity/slash.png");
    
    public SlashProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(SlashProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Make the slash face the camera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));

        // Optional: Add a slight spin for energy effect
        float spin = entity.randomRoll + (entity.age + tickDelta) * 5.0f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));

        // Scale effect: start small, grow, then shrink
        float ageProgress = (entity.age + tickDelta) / 20.0f; // 0.0 to 1.0 over 20 ticks
        float scale = 1.0f - (ageProgress * 0.3f); // Shrink slightly over time
        matrices.scale(scale, scale, scale);

        // Calculate alpha fade
        int alpha = (int) ((1.0f - ageProgress) * 255);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Draw the slash arc as a curved ribbon
        drawSlashArc(buffer, posMatrix, alpha);

        matrices.pop();
    }

    private void drawSlashArc(VertexConsumer buffer, Matrix4f matrix, int alpha) {
        // Define the slash arc curve
        int segments = 8; // Number of segments in the arc
        float size = 1f; // Overall size of the slash
        float thickness = 0.4f; // Thickness of the slash ribbon

        // Create a crescent/arc shape
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;

            // Calculate curve points (crescent shape)
            Vec3d p1 = getArcPoint(t1, size);
            Vec3d p2 = getArcPoint(t2, size);

            // Calculate perpendicular offset for thickness
            Vec3d dir = p2.subtract(p1).normalize();
            Vec3d perpendicular = new Vec3d(-dir.y, dir.x, 0).multiply(thickness);

            // Four corners of the quad
            Vec3d v1 = p1.subtract(perpendicular);
            Vec3d v2 = p1.add(perpendicular);
            Vec3d v3 = p2.add(perpendicular);
            Vec3d v4 = p2.subtract(perpendicular);

            // Calculate alpha fade along the arc (brightest in middle)
            float fadeStart = Math.abs(t1 - 0.5f) * 2.0f; // 0 at center, 1 at edges
            float fadeEnd = Math.abs(t2 - 0.5f) * 2.0f;
            int alphaStart = (int) (alpha * (1.0f - fadeStart * 0.5f));
            int alphaEnd = (int) (alpha * (1.0f - fadeEnd * 0.5f));

            // Draw the quad segment (front and back)
            drawSlashQuad(buffer, matrix, v1, v2, v3, v4, t1, t2, alphaStart, alphaEnd);
        }
    }

    private Vec3d getArcPoint(float t, float size) {
        // Create a curved crescent shape
        // t goes from 0 to 1 along the arc
        float angle = (t - 0.5f) * (float) Math.PI; // -PI/2 to PI/2
        
        // Crescent curve formula
        float x = (float) Math.sin(angle) * size;
        float y = (float) (Math.cos(angle) - 1.0f) * size * 0.6f; // Creates the arc
        
        return new Vec3d(x, y, 0);
    }

    private void drawSlashQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4,
                               float u1, float u2, int alpha1, int alpha2) {
        // Front face
        drawVertex(buffer, matrix, v1, u1, 0, alpha1);
        drawVertex(buffer, matrix, v2, u1, 1, alpha1);
        drawVertex(buffer, matrix, v3, u2, 1, alpha2);
        drawVertex(buffer, matrix, v4, u2, 0, alpha2);

        // Back face (reversed winding order)
        drawVertex(buffer, matrix, v4, u2, 0, alpha2);
        drawVertex(buffer, matrix, v3, u2, 1, alpha2);
        drawVertex(buffer, matrix, v2, u1, 1, alpha1);
        drawVertex(buffer, matrix, v1, u1, 0, alpha1);
    }

    private void drawVertex(VertexConsumer buffer, Matrix4f matrix, Vec3d pos, float u, float v, int alpha) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(200, 220, 255, alpha) // Light blue-white color
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Full brightness
                .normal(0, 0, 1);
    }

    @Override
    public Identifier getTexture(SlashProjectileEntity entity) {
        return TEXTURE;
    }
}
