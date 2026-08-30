package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DomainShrineRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<DomainShrineEntity, R> {

    public DomainShrineRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new DomainShrineModel());
        withScale(4.0F);
    }

    @Override
    public void adjustPositionForRender(R renderState, MatrixStack poseStack, BakedGeoModel model, boolean isReRender) {
        super.adjustPositionForRender(renderState, poseStack, model, isReRender);
        if (!isReRender) {
            float riseDuration = 45.0f; // 2.25 seconds
            float currentAge = renderState.age;
            float startingDepth = -6.0f; // Starts 6 blocks underground
            float yOffset = currentAge < riseDuration ? startingDepth * (1.0f - (currentAge / riseDuration)) : 0.0f;

            poseStack.translate(0.0, yOffset, 0.0);
        }
    }

    @Override
    public void actuallyRender(R renderState, MatrixStack poseStack, BakedGeoModel model, RenderLayer renderType,
                               VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                               int packedLight, int packedOverlay, int renderColor) {
        int lightLevel = 15728880;
        super.actuallyRender(renderState, poseStack, model, renderType, bufferSource, buffer, isReRender, lightLevel, packedOverlay, renderColor);
    }
}
