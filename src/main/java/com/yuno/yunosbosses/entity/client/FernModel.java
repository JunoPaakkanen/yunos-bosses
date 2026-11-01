package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.FernEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class FernModel extends GeoModel<FernEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "fern");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/fern.png");
    }

    @Override
    public Identifier getAnimationResource(FernEntity animatable) {
        return Identifier.of("yunosbosses", "fern");
    }
}
