package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class SeveredTorsoRenderer extends LivingEntityRenderer<SeveredTorsoEntity, PlayerEntityModel<SeveredTorsoEntity>> {

    public SeveredTorsoRenderer(EntityRendererFactory.Context ctx) {
        // Use the standard player model
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.0f);
        this.shadowRadius = 0.0f;
    }

    @Override
    protected void setupTransforms(SeveredTorsoEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);

        // Tilt the torso 90 degrees
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

        // Sink it slightly into the ground
        matrices.translate(0, -1.1, -0.25);
    }

    @Override
    public void render(SeveredTorsoEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        // Hide the legs before rendering
        this.model.leftLeg.visible = false;
        this.model.rightLeg.visible = false;
        this.model.leftPants.visible = false;
        this.model.rightPants.visible = false;

        // Show the rest
        this.model.head.visible = true;
        this.model.body.visible = true;
        this.model.leftArm.visible = true;
        this.model.rightArm.visible = true;

        super.render(entity, 0, 0, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(SeveredTorsoEntity entity) {
        // Use the stored UUID to get the player's skin
        if (entity.getOwnerUuid() != null) {
            return DefaultSkinHelper.getSkinTextures(entity.getOwnerUuid()).texture();
        }
        // Fallback to Steve's skin if no UUID is stored
        return Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    }

    @Override
    protected boolean hasLabel(SeveredTorsoEntity livingEntity) {
        return false;
    }

    @Override
    protected float getAnimationProgress(SeveredTorsoEntity event, float f) {
        return 0.0f;
    }
}