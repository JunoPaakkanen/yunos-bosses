package com.yuno.yunosbosses.entity.client;

import com.yuno.yunosbosses.entity.other.DomainTcmpEyeEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DomainTcmpEyeModel extends GeoModel<DomainTcmpEyeEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "tcmp_eye");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.of("yunosbosses", "textures/entity/tcmp_eye.png");
    }

    @Override
    public Identifier getAnimationResource(DomainTcmpEyeEntity animatable) {
        return null;
    }

    @Override
    public void setCustomAnimations(AnimationState<DomainTcmpEyeEntity> animationState) {
        super.setCustomAnimations(animationState);
        GeoBone eye = getAnimationProcessor().getBone("eye");
        if (eye != null) {
            Float pitch = animationState.getData(DataTickets.ENTITY_PITCH);
            if (pitch != null) {
                eye.setRotX(-pitch * MathHelper.RADIANS_PER_DEGREE);
            }
        }
    }
}
