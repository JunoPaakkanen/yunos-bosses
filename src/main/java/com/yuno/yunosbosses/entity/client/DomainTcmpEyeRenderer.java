package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainTcmpEyeEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Client renderer for the Time Cell Moon Palace centerpiece eye entity.
 * Renders the organic ocular eye with floating celestial levitation,
 * full emissive luminescence, and dynamic gaze tracking.
 */
public class DomainTcmpEyeRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<DomainTcmpEyeEntity, R> {

    public DomainTcmpEyeRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new DomainTcmpEyeModel());
        withScale(2.2F);
    }

    @Override
    public void adjustPositionForRender(R renderState, MatrixStack poseStack, BakedGeoModel model, boolean isReRender) {
        super.adjustPositionForRender(renderState, poseStack, model, isReRender);
        if (!isReRender) {
            float currentAge = renderState.age;

            // Organic breathing levitation bob (gentle floating effect, NO continuous spinning!)
            float hoverBob = MathHelper.sin(currentAge * 0.045f) * 0.12f;
            poseStack.translate(0.0, hoverBob, 0.0);
        }
    }

    @Override
    public void actuallyRender(R renderState, MatrixStack poseStack, BakedGeoModel model, RenderLayer renderType,
                               VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                               int packedLight, int packedOverlay, int renderColor) {
        int lightLevel = 15728880; // Full emissive light
        super.actuallyRender(renderState, poseStack, model, renderType, bufferSource, buffer, isReRender, lightLevel, packedOverlay, renderColor);
    }
}
