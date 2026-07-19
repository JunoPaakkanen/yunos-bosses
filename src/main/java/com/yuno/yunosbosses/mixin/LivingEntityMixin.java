package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelJumping(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // If the entity has the Frame Freeze effect, cancel the jump method entirely
        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float yunosbosses$increaseDamageTaken(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Amplifies the damage by 2x.
            return amount * 2.0F;
        }

        return amount; // Return original damage if they aren't frozen
    }

    // Intercepts the exact moment a status effect is stripped or expires naturally
    @Inject(method = "onStatusEffectRemoved", at = @At("TAIL"))
    private void yunosbosses$triggerGlassShatter(StatusEffectInstance effect, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Check if the removed effect is Frame Freeze
        if (effect.getEffectType().value() == ModEffects.FRAME_FREEZE.value()) {

            // Execute only on the server
            if (!entity.getWorld().isClient() && entity.getWorld() instanceof ServerWorld serverWorld) {
                double x = entity.getX();
                double y = entity.getY() + (entity.getHeight() / 2.0); // Center at torso height
                double z = entity.getZ();

                // Play Frame Shatter sound effect
                serverWorld.playSound(null, x, y, z, ModSounds.FRAME_SHATTER, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Spawn Frame Shatter particle
                serverWorld.spawnParticles(
                        ModParticles.FRAME_SHATTER_PARTICLE,
                        x, y, z,
                        1,
                        0.1, 0.1, 0.1,
                        0.1
                );

                // Layer bright critical hit stars
                serverWorld.spawnParticles(
                        ParticleTypes.CRIT,
                        x, y, z,
                        20,
                        0.2, 0.4, 0.2,
                        0.15
                );

                // Damage the entity
                entity.damage(serverWorld.getDamageSources().indirectMagic(entity, entity), 5.0F);
            }
        }
    }

    @Inject(method = "onStatusEffectApplied", at = @At("TAIL"))
    private void yunosbosses$playFreezeSoundOnApply(StatusEffectInstance effect, Entity source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Play the Frame Freeze sound when the Frame Freeze effect is applied
        if (effect.getEffectType().value() == ModEffects.FRAME_FREEZE.value()) {
            if (!entity.getWorld().isClient() && entity.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSounds.FRAME_FREEZE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void yunosbosses$applyStackingProjectionSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Ensure this logic only runs on players since they own the component
        if (entity instanceof PlayerEntity player) {
            var component = ModEntityComponents.SPELL_DATA.get(player);
            int stacks = component.getSpeedStacks();

            if (stacks > 0) {
                float originalSpeed = cir.getReturnValue();
                // Each stack grants a 30% speed bonus
                float speedMultiplier = 1.0F + (stacks * 0.30F);

                cir.setReturnValue(originalSpeed * speedMultiplier);
            }
        }
    }
}