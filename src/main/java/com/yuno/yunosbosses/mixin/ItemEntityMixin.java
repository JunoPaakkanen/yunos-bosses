package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void preventPickup(PlayerEntity player, CallbackInfo ci) {

        // Check if the player who touched the item is transformed
        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

        if (transformData.isTransformed()) {
            // Cancel the method
            ci.cancel();
        }
    }
}