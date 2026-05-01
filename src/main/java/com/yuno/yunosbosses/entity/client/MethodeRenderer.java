package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class MethodeRenderer extends GeoEntityRenderer<MethodeEntity> {

    public MethodeRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MethodeModel());

        this.shadowRadius = 0.5f; // Casts a shadow

        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, MethodeEntity animatable) {
                if (bone.getName().equals("bone_hand_R")) {
                    return animatable.getMainHandStack();
                }
                return null;
            }

            @Override
            protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, MethodeEntity animatable) {
                return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, MethodeEntity animatable, VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
                // Apply offsets/rotations here
                poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                poseStack.translate(0.0D, 0.2D, 0.0D);

                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public boolean shouldRender(MethodeEntity entity, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }

        return true;
    }
}
