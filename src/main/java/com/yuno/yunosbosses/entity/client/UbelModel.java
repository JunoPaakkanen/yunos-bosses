package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class UbelModel extends GeoModel<UbelEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "ubel");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/ubel.png");
    }

    @Override
    public Identifier getAnimationResource(UbelEntity animatable) {
        return Identifier.of("yunosbosses", "ubel");
    }
}
