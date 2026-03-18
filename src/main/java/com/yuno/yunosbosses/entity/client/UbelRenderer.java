package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.character.UbelEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UbelRenderer extends GeoEntityRenderer<UbelEntity> {

    public UbelRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new UbelModel());
        this.shadowRadius = 0.5f; // Casts a shadow
    }
}
