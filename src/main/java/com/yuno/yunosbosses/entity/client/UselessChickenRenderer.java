package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.modified.UselessChickenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ChickenEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class UselessChickenRenderer extends MobEntityRenderer<UselessChickenEntity, ChickenEntityRenderState, ChickenEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("yunosbosses", "textures/entity/useless_chicken.png");

    public UselessChickenRenderer(EntityRendererFactory.Context context) {
        super(context, new ChickenEntityModel(context.getPart(EntityModelLayers.CHICKEN)), 0.3f);
    }

    @Override
    public ChickenEntityRenderState createRenderState() {
        return new ChickenEntityRenderState();
    }

    @Override
    public void updateRenderState(UselessChickenEntity entity, ChickenEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.flapProgress = MathHelper.lerp(tickDelta, entity.lastFlapProgress, entity.flapProgress);
        state.maxWingDeviation = MathHelper.lerp(tickDelta, entity.lastMaxWingDeviation, entity.maxWingDeviation);
    }

    @Override
    public Identifier getTexture(ChickenEntityRenderState state) {
        return TEXTURE;
    }
}
