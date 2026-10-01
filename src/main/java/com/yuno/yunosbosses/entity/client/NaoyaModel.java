package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.NaoyaEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class NaoyaModel extends GeoModel<NaoyaEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "naoya");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/naoya.png");
    }

    @Override
    public Identifier getAnimationResource(NaoyaEntity animatable) {
        return Identifier.of("yunosbosses", "naoya");
    }
}
