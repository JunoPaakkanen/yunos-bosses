package com.yuno.yunosbosses.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {


    @Unique
    private static final Identifier PROJECTION_SPEED_MODIFIER = Identifier.of("yunosbosses", "projection_speed_modifier");

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void yunosbosses$disableProjectionFovChange(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        // Check if the synced movement attribute has the specific speed boost active
        EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

        if (speedAttr != null && speedAttr.getModifier(PROJECTION_SPEED_MODIFIER) != null) {
            // 1.30 FOV for sprinting, 1.15 otherwise
            float baseFov = player.isSprinting() ? 1.30F : 1.15F;
            cir.setReturnValue(baseFov);
        }
    }
}