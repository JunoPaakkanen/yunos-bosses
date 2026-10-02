package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import net.minecraft.client.model.ModelPart;
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

    public static class SeveredTorsoModel extends PlayerEntityModel {
        public SeveredTorsoModel(ModelPart root, boolean thinArms) {
            super(root, thinArms);
        }

        @Override
        public void setAngles(PlayerEntityRenderState state) {
            super.setAngles(state);

            // Hide the lower body (legs and pants)
            this.leftLeg.visible = false;
            this.rightLeg.visible = false;
            this.leftPants.visible = false;
            this.rightPants.visible = false;
            this.leftLeg.hidden = true;
            this.rightLeg.hidden = true;
            this.leftPants.hidden = true;
            this.rightPants.hidden = true;

            // Ensure the upper body remains visible
            this.head.visible = true;
            this.body.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;
            this.hat.visible = state.hatVisible;
            this.jacket.visible = state.jacketVisible;
            this.leftSleeve.visible = state.leftSleeveVisible;
            this.rightSleeve.visible = state.rightSleeveVisible;
        }
    }

    public SeveredTorsoRenderer(EntityRendererFactory.Context ctx) {
        // Use the custom torso model that hides the lower half
        super(ctx, new SeveredTorsoModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.0f);
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
        state.leftPantsLegVisible = false;
        state.rightPantsLegVisible = false;
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
        // Also ensure model parts are explicitly flagged before render pass
        this.model.leftLeg.visible = false;
        this.model.rightLeg.visible = false;
        this.model.leftPants.visible = false;
        this.model.rightPants.visible = false;
        this.model.leftLeg.hidden = true;
        this.model.rightLeg.hidden = true;
        this.model.leftPants.hidden = true;
        this.model.rightPants.hidden = true;

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
