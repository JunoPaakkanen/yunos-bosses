package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DomainShrineRenderer extends GeoEntityRenderer<DomainShrineEntity> {

    public DomainShrineRenderer(EntityRendererFactory.Context ctx) {
        // Pass the GeckoLib model instance directly to the super constructor
        super(ctx, new DomainShrineModel());
    }

    @Override
    public void render(DomainShrineEntity entity, float entityYaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        matrixStack.push();

        // RISING ANIMATION
        float riseDuration = 45.0f; // 2.25 seconds
        float currentAge = entity.age + tickDelta;
        float startingDepth = -6.0f; // Starts 6 blocks underground
        float yOffset = currentAge < riseDuration ? startingDepth * (1.0f - (currentAge / riseDuration)) : 0.0f;

        matrixStack.translate(0.0, yOffset, 0.0);

        // Make it massive
        matrixStack.scale(4.0F, 4.0F, 4.0F);

        // 3. Draw the model using GeckoLib's rendering engine
        super.render(entity, entityYaw, tickDelta, matrixStack, vertexConsumers, light);

        matrixStack.pop();
    }
}