package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void interceptScreen(Screen screen, CallbackInfo ci) {
        // If the game is trying to open the player's inventory
        if (screen instanceof InventoryScreen) {
            var player = MinecraftClient.getInstance().player;

            if (player != null) {
                var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);
                if (transformData.isTransformed()) {
                    // Cancel it
                    ci.cancel();
                }
            }
        }
    }
}