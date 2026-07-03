package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DomainShrineModel extends GeoModel<DomainShrineEntity> {
    @Override
    public Identifier getModelResource(DomainShrineEntity animatable) {
        return Identifier.of("yunosbosses", "geo/shrine.geo.json");
    }

    @Override
    public Identifier getTextureResource(DomainShrineEntity animatable) {
        return Identifier.of("yunosbosses", "textures/entity/shrine_building.png");
    }

    @Override
    public Identifier getAnimationResource(DomainShrineEntity animatable) {
        return null;
    }
}