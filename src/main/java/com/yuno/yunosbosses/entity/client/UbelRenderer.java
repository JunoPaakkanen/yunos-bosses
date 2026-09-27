package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import com.yuno.yunosbosses.render.gui.DomainClashOverlay;
import com.yuno.yunosbosses.render.gui.DomainCutsceneOverlay;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.ItemInHandGeoLayer;

public class UbelRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<UbelEntity, R> {

    public UbelRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new UbelModel());

        this.shadowRadius = 0.5f; // Casts a shadow

        this.addRenderLayer(new ItemInHandGeoLayer<>(this, "bone_hand_R", "bone_hand_L") {
            @Override
            protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, ItemDisplayContext displayContext, R renderState, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
                // Apply offsets/rotations
                poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0));
                poseStack.translate(0.0D, 0.2D, 0.0D);

                int light = (DomainCutsceneOverlay.isRenderingGuiOverlay || DomainClashOverlay.isRenderingGuiOverlay || packedLight >= 15728880) ? 15728880 : packedLight;
                int overlay = (DomainCutsceneOverlay.isRenderingGuiOverlay || DomainClashOverlay.isRenderingGuiOverlay || packedLight >= 15728880) ? OverlayTexture.DEFAULT_UV : packedOverlay;

                super.renderStackForBone(poseStack, bone, stack, displayContext, renderState, bufferSource, light, overlay);
            }
        });
    }

    @Override
    public void render(R renderState, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        if (DomainCutsceneOverlay.isRenderingGuiOverlay || DomainClashOverlay.isRenderingGuiOverlay || packedLight >= 15728880) {
            renderState.addGeckolibData(DataTickets.PACKED_LIGHT, 15728880);
            renderState.addGeckolibData(DataTickets.PACKED_OVERLAY, OverlayTexture.DEFAULT_UV);
        } else {
            renderState.addGeckolibData(DataTickets.PACKED_LIGHT, packedLight);
        }
        super.render(renderState, poseStack, bufferSource, packedLight);
    }

    @Override
    public void addRenderData(UbelEntity animatable, Void relatedObject, R renderState) {
        super.addRenderData(animatable, relatedObject, renderState);
        if (DomainCutsceneOverlay.isRenderingGuiOverlay || DomainClashOverlay.isRenderingGuiOverlay) {
            renderState.addGeckolibData(DataTickets.PACKED_LIGHT, 15728880);
            renderState.addGeckolibData(DataTickets.PACKED_OVERLAY, OverlayTexture.DEFAULT_UV);
        }
    }

    @Override
    public boolean shouldRender(UbelEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
