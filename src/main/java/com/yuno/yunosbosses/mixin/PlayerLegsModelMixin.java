package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerLegsModelMixin {

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void hideTopHalf(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

            if (transformData.isTransformed()) {
                PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;

                // Hide everything but the legs
                model.head.visible = false;
                model.hat.visible = false;
                model.body.visible = false;
                model.jacket.visible = false;
                model.leftArm.visible = false;
                model.rightArm.visible = false;
                model.leftSleeve.visible = false;
                model.rightSleeve.visible = false;

                // Keep the legs
                model.leftLeg.visible = true;
                model.rightLeg.visible = true;
            }
        }
    }
}