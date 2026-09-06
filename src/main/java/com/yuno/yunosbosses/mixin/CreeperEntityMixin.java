package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$freezeCreeper(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (creeper.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Cancel before the fuse increments or explode() is called
            ci.cancel();
        }
    }
}
