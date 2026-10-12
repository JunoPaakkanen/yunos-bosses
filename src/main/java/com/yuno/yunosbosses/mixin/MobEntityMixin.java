package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.YunosBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "isAiDisabled", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$disableAiOnFrameFreeze(CallbackInfoReturnable<Boolean> cir) {
        MobEntity mob = (MobEntity) (Object) this;
        if (mob.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$preventBossSelfTarget(LivingEntity target, CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (mob instanceof YunosBossEntity && target != null && (target == mob || target.getUuid().equals(mob.getUuid()))) {
            ci.cancel();
        }
    }

    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    private void yunosbosses$sanitizeBossTarget(CallbackInfoReturnable<LivingEntity> cir) {
        MobEntity mob = (MobEntity) (Object) this;
        if (mob instanceof YunosBossEntity && cir.getReturnValue() != null) {
            if (cir.getReturnValue() == mob || cir.getReturnValue().getUuid().equals(mob.getUuid())) {
                cir.setReturnValue(null);
            }
        }
    }
}
