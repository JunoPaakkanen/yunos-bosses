package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import com.yuno.yunosbosses.render.gui.DomainClashOverlay;
import com.yuno.yunosbosses.render.gui.DomainCutsceneOverlay;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class NaoyaRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<NaoyaEntity, R> {

    public NaoyaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new NaoyaModel());
        this.shadowRadius = 0.5f;
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
    public void addRenderData(NaoyaEntity animatable, Void relatedObject, R renderState) {
        super.addRenderData(animatable, relatedObject, renderState);
        if (DomainCutsceneOverlay.isRenderingGuiOverlay || DomainClashOverlay.isRenderingGuiOverlay) {
            renderState.addGeckolibData(DataTickets.PACKED_LIGHT, 15728880);
            renderState.addGeckolibData(DataTickets.PACKED_OVERLAY, OverlayTexture.DEFAULT_UV);
        }
    }

    @Override
    public boolean shouldRender(NaoyaEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
