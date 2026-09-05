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

    // Intercept slot changes (Mouse Scroll Wheel and Number Keys)
    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void limitAbilityScrolling(int slot, CallbackInfo ci) {
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(this.player);

        if (transformData.isTransformed()) {
            int targetSlot = slot;

            // Handle scrolling transitions:
            // If we were at slot 2 and scrolled up (vanilla cycles to 3), wrap to 0.
            // If we were at slot 0 and scrolled down (vanilla cycles to 8), wrap to 2.
            if (this.selectedSlot == 2 && targetSlot == 3) {
                targetSlot = 0;
            } else if (this.selectedSlot == 0 && targetSlot == 8) {
                targetSlot = 2;
            } else if (targetSlot > 2) {
                // If number keys 4-9 are pressed, snap back to the 3rd ability
                targetSlot = 2;
            } else if (targetSlot < 0) {
                targetSlot = 0;
            }

            this.selectedSlot = targetSlot;
            ci.cancel();
        }
    }

    // Prevent the 4-9 keys or other sources from keeping an out-of-bounds slot
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void clampNumberKeys(CallbackInfo ci) {
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(this.player);

        if (transformData.isTransformed()) {
            // If selected slot is somehow beyond 2, snap it back to 2
            if (this.selectedSlot > 2) {
                this.selectedSlot = 2;
            }
        }
    }
}
