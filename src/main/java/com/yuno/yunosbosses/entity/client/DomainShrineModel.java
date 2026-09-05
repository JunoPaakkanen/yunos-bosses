package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DomainShrineModel extends GeoModel<DomainShrineEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "shrine");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/shrine_building.png");
    }

    @Override
    public Identifier getAnimationResource(DomainShrineEntity animatable) {
        return null;
    }
}
