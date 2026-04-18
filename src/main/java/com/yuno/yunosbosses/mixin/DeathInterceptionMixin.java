package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.other.SeveredTorsoEntity;
import com.yuno.yunosbosses.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DeathInterceptionMixin {
    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void triggerTransformation(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof PlayerEntity player) {
            var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(player);

            // Transform if the player has the effect and hasn't yet been transformed
            // --- BINDING VOW: GOJO ---
            if (player.hasStatusEffect(ModEffects.GOJO_BINDING_VOW) && !transformData.isTransformed()) {

                // Play voice line
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.STILL_ALIVE, SoundCategory.NEUTRAL, 1.0f, 1.0f + (player.getRandom().nextFloat() * 0.2f - 0.1f));

                // Spawn severed torso
                if (!player.getWorld().isClient) {
                    SeveredTorsoEntity torso = new SeveredTorsoEntity(ModEntities.SEVERED_TORSO, player.getWorld());
                    // Position it exactly where the player is
                    torso.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0);
                    // Apply some death physics
                    torso.setVelocity(player.getVelocity().x, 0.1, player.getVelocity().z);
                    torso.setOwnerUuid(player.getUuid());
                    player.getWorld().spawnEntity(torso);
                }

                // Apply healing effects
                player.setHealth(2.0f);
                player.clearStatusEffects();
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));

                // Set the new data
                transformData.setTransformed(true);

                // Return true to stop death
                cir.setReturnValue(true);
            }
        }
    }
}