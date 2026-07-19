package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class ClientInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void yunosbosses$paralyzeMovementInput(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            Input input = (Input) (Object) this;

            // Override and zero out all active keyboard input values
            input.movementSideways = 0.0F;
            input.movementForward = 0.0F;
            input.pressingLeft = false;
            input.pressingRight = false;
            input.pressingForward = false;
            input.pressingBack = false;
            input.jumping = false;
            input.sneaking = false;
        }
    }
}