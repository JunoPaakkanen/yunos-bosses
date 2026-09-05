package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.EffectRemovalContext;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

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
    private float yunosbosses$increaseDamageTaken(float amount, ServerWorld world, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Amplify damage and shatter frame
            float damage = ProjectionSorcery.shatterFrame(entity, source, amount);

            // Remove effect manually (bypass onStatusEffectRemoved)
            try {
                EffectRemovalContext.setManualRemoval(true);
                entity.removeStatusEffect(ModEffects.FRAME_FREEZE);
            } finally {
                EffectRemovalContext.setManualRemoval(false);
            }

            return damage;
        }

        return amount; // Return original damage if they aren't frozen
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void increaseMeterOnDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // The entity being attacked/damaged
        LivingEntity victim = (LivingEntity) (Object) this;

        // Check if the direct attacker is a LivingEntity
        if (source.getAttacker() instanceof PlayerEntity attacker) {

            // Check if the attacker has projection sorcery as the active spell
            var component = ModEntityComponents.SPELL_DATA.get(attacker);
            if (component.getActiveSpell() instanceof ProjectionSorcery) {

                // Ensure it was a direct melee punch rather than a projectile
                if (source.getSource() == attacker) {
                    component.addFrameMeter(10);
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void applyEffectOnSuccessfulHit(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // cir.getReturnValue() is true ONLY if damage was successfully dealt
        if (!cir.getReturnValue()) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();

        if (attacker instanceof PlayerEntity player) {
            var component = ModEntityComponents.SPELL_DATA.get(player);
            if (component.getFrameMeter() >= 100) {
                target.addStatusEffect(new StatusEffectInstance(ModEffects.FRAME_FREEZE, 40, 0, false, false, true));
                component.setFrameMeter(0);
            }
        }
    }

    // Intercepts the exact moment status effects are stripped or expire naturally
    @Inject(method = "onStatusEffectsRemoved", at = @At("TAIL"))
    private void yunosbosses$triggerGlassShatter(Collection<StatusEffectInstance> effects, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        for (StatusEffectInstance effect : effects) {
            // Sync effect removal to nearby tracking clients so client-side rendering (e.g., LivingEntityRendererMixin) updates
            if (!entity.getWorld().isClient()) {
                RemoveEntityStatusEffectS2CPacket packet = new RemoveEntityStatusEffectS2CPacket(entity.getId(), effect.getEffectType());
                for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
                    player.networkHandler.sendPacket(packet);
                }
            }

            // If the effect removal is triggered manually, skip this
            if (EffectRemovalContext.isManualRemoval()) {
                continue;
            }

            // Check if the removed effect is Frame Freeze
            if (effect.getEffectType().value() == ModEffects.FRAME_FREEZE.value()) {
                float damage = ProjectionSorcery.shatterFrame(entity, null, 0);
                if (entity.getWorld() instanceof ServerWorld serverWorld) {
                    entity.damage(serverWorld, entity.getDamageSources().indirectMagic(entity, entity), damage);
                }
            }
        }
    }

    @Inject(method = "onStatusEffectApplied", at = @At("TAIL"))
    private void yunosbosses$onStatusEffectApplied(StatusEffectInstance effect, Entity source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Sync effect application to nearby tracking clients
        if (!entity.getWorld().isClient()) {
            EntityStatusEffectS2CPacket packet = new EntityStatusEffectS2CPacket(entity.getId(), effect, true);
            for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
                player.networkHandler.sendPacket(packet);
            }
        }

        // Play the Frame Freeze sound when the Frame Freeze effect is applied
        if (effect.getEffectType().value() == ModEffects.FRAME_FREEZE.value()) {
            if (!entity.getWorld().isClient() && entity.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSounds.FRAME_FREEZE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Inject(method = "onStatusEffectUpgraded", at = @At("TAIL"))
    private void yunosbosses$onStatusEffectUpgraded(StatusEffectInstance effect, boolean reapplyEffect, Entity source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Sync effect upgrade to nearby tracking clients
        if (!entity.getWorld().isClient()) {
            EntityStatusEffectS2CPacket packet = new EntityStatusEffectS2CPacket(entity.getId(), effect, true);
            for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    @Shadow protected abstract void tickStatusEffects();
    @Shadow public LimbAnimator limbAnimator;
    @Shadow public float lastBodyYaw;
    @Shadow public float lastHeadYaw;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTickFreeze(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            // Lock velocity
            entity.setVelocity(Vec3d.ZERO);
            entity.velocityDirty = true;

            // Sync coordinates to current coordinates to stop interpolation jitter
            entity.lastRenderX = entity.getX();
            entity.lastRenderY = entity.getY();
            entity.lastRenderZ = entity.getZ();

            // Sync rotation fields
            entity.lastYaw = entity.getYaw();
            entity.lastPitch = entity.getPitch();
            this.lastBodyYaw = entity.bodyYaw;
            this.lastHeadYaw = entity.headYaw;

            // Freeze limb animation calculations
            if (this.limbAnimator != null) {
                this.limbAnimator.setSpeed(0.0F);
                this.limbAnimator.reset();
            }

            // Decay status effect on the server
            if (!entity.getWorld().isClient()) {
                this.tickStatusEffects();
            }

            entity.onLanding();
            ci.cancel();
        }
    }
}
