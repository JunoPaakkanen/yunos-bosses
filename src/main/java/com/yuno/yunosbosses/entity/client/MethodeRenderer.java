package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.ItemInHandGeoLayer;

public class MethodeRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<MethodeEntity, R> {

    public MethodeRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MethodeModel());

        this.shadowRadius = 0.5f; // Casts a shadow

        this.addRenderLayer(new ItemInHandGeoLayer<>(this, "bone_hand_R", "bone_hand_L") {
            @Override
            protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, ItemDisplayContext displayContext, R renderState, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
                // Apply offsets/rotations here
                poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                poseStack.translate(0.0D, 0.2D, 0.0D);

                super.renderStackForBone(poseStack, bone, stack, displayContext, renderState, bufferSource, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public boolean shouldRender(MethodeEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
