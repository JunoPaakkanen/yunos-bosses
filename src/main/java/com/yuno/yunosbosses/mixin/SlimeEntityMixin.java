package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelSlimeDamageWhenFrozen(LivingEntity target, CallbackInfo ci) {
        SlimeEntity slime = (SlimeEntity) (Object) this;
        if (slime.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            ci.cancel();
        }
    }
}
