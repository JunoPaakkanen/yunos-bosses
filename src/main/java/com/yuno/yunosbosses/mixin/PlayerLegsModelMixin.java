package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerLegsModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("HEAD"))
    private void yunosbosses$resetHeadVisibility(PlayerEntityRenderState state, CallbackInfo ci) {
        // In 1.21.2+, Minecraft's PlayerEntityModel#setAngles resets the visibility of body, arms, legs,
        // jacket, sleeves, hat, and pants each frame, but omits resetting head.visible.
        // Once head.visible is set to false during transformation, it never gets restored automatically.
        // We explicitly reset it here before angles and transformations are evaluated.
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        model.head.visible = !state.spectator;
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void hideTopHalf(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getEntityById(state.id) instanceof PlayerEntity player) {
            var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

            if (transformData.isTransformed()) {
                PlayerEntityModel model = (PlayerEntityModel) (Object) this;

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

        // In first person animation pass, ensure sleeves mirror arm visibility
        if (FirstPersonMode.isFirstPersonPass() && state instanceof IPlayerAnimationState animState && animState.playerAnimLib$isCameraEntity()) {
            PlayerEntityModel model = (PlayerEntityModel) (Object) this;
            model.rightSleeve.hidden = model.rightArm.hidden;
            model.leftSleeve.hidden = model.leftArm.hidden;
        }
    }
}
