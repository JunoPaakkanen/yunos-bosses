package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class MethodeModel extends GeoModel<MethodeEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "methode");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/methode.png");
    }

    @Override
    public Identifier getAnimationResource(MethodeEntity animatable) {
        return Identifier.of("yunosbosses", "methode");
    }
}
