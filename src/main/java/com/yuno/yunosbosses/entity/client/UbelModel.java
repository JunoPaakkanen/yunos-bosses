package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class UbelModel extends GeoModel<UbelEntity> {
    @Override
    public Identifier getModelResource(UbelEntity animatable) {
        // Point to: assets/yunosbosses/geo/ubel.geo.json
        return Identifier.of("yunosbosses", "geo/ubel.geo.json");
    }

    @Override
    public Identifier getTextureResource(UbelEntity animatable) {
        // Point to: assets/yunosbosses/textures/entity/ubel.png
        return Identifier.of("yunosbosses", "textures/entity/ubel.png");
    }

    @Override
    public Identifier getAnimationResource(UbelEntity animatable) {
        // Point to: assets/yunosbosses/animations/ubel.animation.json
        return Identifier.of("yunosbosses", "animations/ubel.animation.json");
    }
}
