package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class SeveredTorsoRenderer extends LivingEntityRenderer<SeveredTorsoEntity, PlayerEntityRenderState, PlayerEntityModel> {

    public SeveredTorsoRenderer(EntityRendererFactory.Context ctx) {
        // Use the standard player model
        super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.0f);
        this.shadowRadius = 0.0f;
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    @Override
    public void updateRenderState(SeveredTorsoEntity entity, PlayerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        if (entity.getOwnerUuid() != null) {
            state.skinTextures = DefaultSkinHelper.getSkinTextures(entity.getOwnerUuid());
        } else {
            state.skinTextures = DefaultSkinHelper.getSteve();
        }
    }

    @Override
    protected void setupTransforms(PlayerEntityRenderState state, MatrixStack matrices, float bodyYaw, float tickDelta) {
        super.setupTransforms(state, matrices, bodyYaw, tickDelta);

        // Tilt the torso 90 degrees
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

        // Sink it slightly into the ground
        matrices.translate(0, -1.1, -0.25);
    }

    @Override
    public void render(PlayerEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
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

        super.render(state, matrixStack, vertexConsumerProvider, light);
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        if (state.skinTextures != null) {
            return state.skinTextures.texture();
        }
        return Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    }

    @Override
    protected boolean hasLabel(SeveredTorsoEntity livingEntity, double d) {
        return false;
    }
}
