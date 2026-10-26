package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.binding_vow.BindingVowManager;
import com.yuno.yunosbosses.binding_vow.VexLifeLeechBindingVow;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.util.BossContributionTracker;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.util.EffectRemovalContext;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.yuno.yunosbosses.util.BossCombatHelper;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "setAttacker", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$preventBossSelfAttacker(LivingEntity attacker, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof YunosBossEntity && attacker != null && (attacker == entity || attacker.getUuid().equals(entity.getUuid()))) {
            ci.cancel();
        }
    }

    @Inject(method = "getAttacker", at = @At("RETURN"), cancellable = true)
    private void yunosbosses$sanitizeBossAttacker(CallbackInfoReturnable<LivingEntity> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof YunosBossEntity && cir.getReturnValue() != null) {
            if (cir.getReturnValue() == entity || cir.getReturnValue().getUuid().equals(entity.getUuid())) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$preventGliding(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasStatusEffect(ModEffects.ANTI_ESCAPE)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isUsingRiptide", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelRiptide(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasStatusEffect(ModEffects.ANTI_ESCAPE)) {
            cir.setReturnValue(false);
        }
    }

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

    @Inject(method = "damage", at = @At("RETURN"))
    private void applyEffectOnSuccessfulHit(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // cir.getReturnValue() is true ONLY if damage was successfully dealt
        if (!cir.getReturnValue()) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();

        // Alert summoned vexes to defend their summoner immediately
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.isAlive() && livingAttacker != target && !livingAttacker.isTeammate(target)) {
            String casterTag = "Caster_" + target.getUuidAsString();
            Box vexSearchBox = target.getBoundingBox().expand(48.0);
            List<VexEntity> summonedVexes = world.getEntitiesByClass(
                    VexEntity.class,
                    vexSearchBox,
                    v -> v.isAlive() && v.getCommandTags().contains(casterTag)
            );
            for (VexEntity vex : summonedVexes) {
                if (vex.getTarget() == null || !vex.getTarget().isAlive()) {
                    vex.setTarget(livingAttacker);
                    vex.setBounds(null);
                }
            }
        }

        if (attacker instanceof PlayerEntity player) {
            var component = ModEntityComponents.SPELL_DATA.get(player);
            if (component.getMeter(ModSpells.PROJECTION_SORCERY) >= 100) {
                target.addStatusEffect(new StatusEffectInstance(ModEffects.FRAME_FREEZE, 40, 0, false, false, true));
                component.setMeter(ModSpells.PROJECTION_SORCERY, 0);
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

        // Play the Frame Freeze sound and forcefully release any charging staff spell when Frame Freeze is applied
        if (effect.getEffectType().value() == ModEffects.FRAME_FREEZE.value()) {
            if (entity.isUsingItem()) {
                if (entity.getActiveItem().getItem() instanceof StaffItem) {
                    entity.stopUsingItem();
                } else {
                    entity.clearActiveItem();
                }
            }
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
            if (entity.isUsingItem()) {
                if (entity.getActiveItem().getItem() instanceof StaffItem) {
                    entity.stopUsingItem();
                } else {
                    entity.clearActiveItem();
                }
            }

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

            // Decay invulnerability and hurt timers so damage can still be dealt
            if (entity.timeUntilRegen > 0) {
                entity.timeUntilRegen--;
            }
            if (entity.hurtTime > 0) {
                entity.hurtTime--;
            }

            // Decay status effects on both server and client
            this.tickStatusEffects();

            entity.onLanding();
            ci.cancel();
        }
    }

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void yunosbosses$trackBossDamage(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof YunosBossEntity) {
            BossContributionTracker.recordDamage(entity, source, amount);
            if (entity instanceof MobEntity mob) {
                mob.setPersistent();
            }
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void yunosbosses$onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof YunosBossEntity) {
            BossContributionTracker.onBossDeath(entity, damageSource);
        }

        // Vex Life Leech Binding Vow death trigger
        if (entity instanceof VexEntity vex && !vex.getWorld().isClient() && vex.getWorld() instanceof ServerWorld serverWorld) {
            // Only triggers if this vex was spawned under the vow
            if (vex.getCommandTags().contains("VexLifeLeech")) {
                for (String tag : vex.getCommandTags()) {
                    if (tag.startsWith("Caster_")) {
                        try {
                            UUID casterUuid = UUID.fromString(tag.substring(7));
                            PlayerEntity caster = serverWorld.getPlayerByUuid(casterUuid);
                            // Caster must be alive and still possess the binding vow
                            if (caster != null && caster.isAlive() && BindingVowManager.hasVow(caster, VexLifeLeechBindingVow.ID)) {
                                caster.heal(VexLifeLeechBindingVow.HEAL_AMOUNT);
                                serverWorld.spawnParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                                serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8F, 1.3F);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                    }
                }
            }
        }

        if (entity instanceof PlayerEntity player) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                // Clear target and attacker on any boss currently engaged with this player
                Box searchBox = player.getBoundingBox().expand(128.0);
                List<MobEntity> nearbyBosses = serverWorld.getEntitiesByClass(MobEntity.class, searchBox,
                        mob -> mob instanceof YunosBossEntity && (mob.getTarget() == player || mob.getAttacker() == player));
                for (MobEntity boss : nearbyBosses) {
                    boss.setTarget(null);
                    boss.setAttacker(null);
                    boss.getNavigation().stop();
                }
            }
        }
    }

    @Redirect(
        method = "modifyAppliedDamage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/effect/StatusEffectInstance;getAmplifier()I"
        )
    )
    private int yunosbosses$capResistanceAmplifier(StatusEffectInstance instance, DamageSource source, float amount) {
        int amplifier = instance.getAmplifier();
        if (BossCombatHelper.shouldCapResistance(source)) {
            return Math.min(amplifier, 1); // Cap to Resistance II (amplifier 1 = 40% reduction)
        }
        return amplifier;
    }

}
