package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.AntiTeleportEffect;
import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPearlTeleportOnImpact(HitResult hitResult, CallbackInfo ci) {
        EnderPearlEntity pearl = (EnderPearlEntity) (Object) this;
        Entity owner = pearl.getOwner();
        if (owner instanceof LivingEntity living && living.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(living);
            pearl.discard();
            ci.cancel();
        }
    }
}
