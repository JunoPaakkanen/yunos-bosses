package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

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
        model.head.hidden = false;
        model.hat.hidden = false;
        model.body.hidden = false;
        model.jacket.hidden = false;
        model.leftArm.hidden = false;
        model.rightArm.hidden = false;
        model.leftSleeve.hidden = false;
        model.rightSleeve.hidden = false;
        model.leftLeg.hidden = false;
        model.rightLeg.hidden = false;
        model.leftPants.hidden = false;
        model.rightPants.hidden = false;
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void hideTopHalf(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;

        if (state instanceof FrameFreezeStateAccess access) {
            Map<String, PlayerAnimBone> capturedBones = access.yunosbosses$getCapturedPlayerAnimBones();
            if (capturedBones != null) {
                yunosbosses$applyCapturedBone(model.head, capturedBones.get("head"));
                yunosbosses$applyCapturedBone(model.body, capturedBones.get("torso"));
                yunosbosses$applyCapturedBone(model.rightArm, capturedBones.get("right_arm"));
                yunosbosses$applyCapturedBone(model.leftArm, capturedBones.get("left_arm"));
                yunosbosses$applyCapturedBone(model.rightLeg, capturedBones.get("right_leg"));
                yunosbosses$applyCapturedBone(model.leftLeg, capturedBones.get("left_leg"));
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getEntityById(state.id) instanceof PlayerEntity player) {
            var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

            if (transformData.isTransformed()) {
                // Hide everything but the legs
                model.head.visible = false;
                model.hat.visible = false;
                model.body.visible = false;
                model.jacket.visible = false;
                model.leftArm.visible = false;
                model.rightArm.visible = false;
                model.leftSleeve.visible = false;
                model.rightSleeve.visible = false;

                model.head.hidden = true;
                model.hat.hidden = true;
                model.body.hidden = true;
                model.jacket.hidden = true;
                model.leftArm.hidden = true;
                model.rightArm.hidden = true;
                model.leftSleeve.hidden = true;
                model.rightSleeve.hidden = true;

                // Keep the legs
                model.leftLeg.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.hidden = false;
                model.rightLeg.hidden = false;
            }
        }

        // In first person animation pass, ensure sleeves mirror arm visibility
        if (FirstPersonMode.isFirstPersonPass() && state instanceof IPlayerAnimationState animState && animState.playerAnimLib$isCameraEntity()) {
            model.rightSleeve.hidden = model.rightArm.hidden;
            model.leftSleeve.hidden = model.leftArm.hidden;
        }
    }

    @Unique
    private static void yunosbosses$applyCapturedBone(ModelPart part, PlayerAnimBone capturedBone) {
        if (capturedBone != null) {
            RenderUtil.translatePartToBone(part, capturedBone, part.getDefaultTransform());
        }
    }
}
