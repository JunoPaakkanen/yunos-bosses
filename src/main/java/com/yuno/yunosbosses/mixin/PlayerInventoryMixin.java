package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow public int selectedSlot;
    @Shadow @Final public PlayerEntity player;

    // Intercept the Mouse Scroll Wheel
    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void limitAbilityScrolling(double scrollAmount, CallbackInfo ci) {
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(this.player);

        if (transformData.isTransformed()) {
            int direction = (int) Math.signum(scrollAmount);
            this.selectedSlot -= direction;

            // Loop smoothly between slots 0, 1, and 2
            if (this.selectedSlot < 0) {
                this.selectedSlot = 2; // Scroll past 1 -> goes to 3
            } else if (this.selectedSlot > 2) {
                this.selectedSlot = 0; // Scroll past 3 -> goes to 1
            }

            // Cancel the vanilla scrolling entirely
            ci.cancel();
        }
    }

    // Prevent the 4-9 keys from selecting empty space
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void clampNumberKeys(CallbackInfo ci) {
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(this.player);

        if (transformData.isTransformed()) {
            // If they press 4, 5, 6, etc., snap it back to the 3rd ability
            if (this.selectedSlot > 2) {
                this.selectedSlot = 2;
            }
        }
    }
}