package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MethodeModel extends GeoModel<MethodeEntity> {
    @Override
    public Identifier getModelResource(MethodeEntity animatable) {
        // Point to: assets/yunosbosses/geo/methode.geo.json
        return Identifier.of("yunosbosses", "geo/methode.geo.json");
    }

    @Override
    public Identifier getTextureResource(MethodeEntity animatable) {
        // Point to: assets/yunosbosses/textures/entity/methode.png
        return Identifier.of("yunosbosses", "textures/entity/methode.png");
    }

    @Override
    public Identifier getAnimationResource(MethodeEntity animatable) {
        // Point to: assets/yunosbosses/animations/methode.animation.json
        return Identifier.of("yunosbosses", "animations/methode.animation.json");
    }
}
