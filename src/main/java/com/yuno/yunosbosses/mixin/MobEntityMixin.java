package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
    public abstract class MobEntityMixin {
        @Inject(method = "isAiDisabled", at = @At("HEAD"), cancellable = true)
        private void yunosbosses$disableAiOnFrameFreeze(CallbackInfoReturnable<Boolean> cir) {
            MobEntity mob = (MobEntity) (Object) this;
            if (mob.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
                cir.setReturnValue(true);}
        }
    }
