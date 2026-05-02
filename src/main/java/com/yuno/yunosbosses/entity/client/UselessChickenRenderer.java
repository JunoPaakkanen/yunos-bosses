package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.modified.UselessChickenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class UselessChickenRenderer extends MobEntityRenderer<UselessChickenEntity, ChickenEntityModel<UselessChickenEntity>> {

    // This points to your custom .png file
    private static final Identifier TEXTURE = Identifier.of("yunosbosses", "textures/entity/useless_chicken.png");

    public UselessChickenRenderer(EntityRendererFactory.Context context) {
        super(context, new ChickenEntityModel<>(context.getPart(EntityModelLayers.CHICKEN)), 0.3f);
    }

    @Override
    public Identifier getTexture(UselessChickenEntity entity) {
        return TEXTURE;
    }
}