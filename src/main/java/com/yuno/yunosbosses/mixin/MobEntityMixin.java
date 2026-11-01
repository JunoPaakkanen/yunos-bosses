package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.YunosMiniBossEntity;
import com.yuno.yunosbosses.util.BlackFlash;
import com.yuno.yunosbosses.util.BossCombatHelper;
import com.yuno.yunosbosses.util.MiniBossHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements YunosMiniBossEntity {

    @Unique
    private boolean yunos$isMiniBoss = false;

    @Unique
    private boolean yunos$miniBossEvaluated = false;

    @Unique
    private int yunos$reinforcementBrokenTicks = 0;

    @Override
    public boolean yunos$isMiniBoss() {
        return this.yunos$isMiniBoss;
    }

    @Override
    public void yunos$setMiniBoss(boolean isMiniBoss) {
        this.yunos$isMiniBoss = isMiniBoss;
    }

    @Override
    public boolean yunos$isMiniBossEvaluated() {
        return this.yunos$miniBossEvaluated;
    }

    @Override
    public void yunos$setMiniBossEvaluated(boolean evaluated) {
        this.yunos$miniBossEvaluated = evaluated;
    }

    @Override
    public int yunos$getReinforcementBrokenTicks() {
        return this.yunos$reinforcementBrokenTicks;
    }

    @Override
    public void yunos$setReinforcementBrokenTicks(int ticks) {
        this.yunos$reinforcementBrokenTicks = ticks;
    }

    @Override
    public void yunos$breakReinforcement(int ticks, DamageSource source) {
        MobEntity mob = (MobEntity) (Object) this;
        BossCombatHelper.executeMiniBossReinforcementBreak(this, mob, source, ticks);
    }

    @Inject(method = "initialize", at = @At("RETURN"))
    private void yunosbosses$onInitializeMiniBoss(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        MobEntity mob = (MobEntity) (Object) this;
        MiniBossHelper.tryEvaluateMiniBossSpawn(mob, world.toServerWorld());
    }

    @Inject(method = "mobTick", at = @At("TAIL"))
    private void yunosbosses$onMobTickMiniBoss(ServerWorld world, CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (!this.yunos$miniBossEvaluated) {
            MiniBossHelper.tryEvaluateMiniBossSpawn(mob, world);
        }
        if (this.yunos$isMiniBoss) {
            MiniBossHelper.tickMiniBoss(mob, world);
        }
    }

    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void yunosbosses$onMiniBossMeleeAttack(ServerWorld world, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !this.yunos$isMiniBoss) {
            return;
        }
        MobEntity mob = (MobEntity) (Object) this;
        if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
            BlackFlash.blackFlashChanceNonChaining(mob, livingTarget, MiniBossHelper.BLACK_FLASH_CHANCE);
        }
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void yunosbosses$writeMiniBossData(WriteView view, CallbackInfo ci) {
        view.putBoolean("YunosIsMiniBoss", this.yunos$isMiniBoss);
        view.putBoolean("YunosMiniBossEvaluated", this.yunos$miniBossEvaluated);
        if (this.yunos$reinforcementBrokenTicks > 0) {
            view.putInt("YunosReinforcementBrokenTicks", this.yunos$reinforcementBrokenTicks);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void yunosbosses$readMiniBossData(ReadView view, CallbackInfo ci) {
        this.yunos$isMiniBoss = view.getBoolean("YunosIsMiniBoss", false);
        this.yunos$miniBossEvaluated = view.getBoolean("YunosMiniBossEvaluated", false);
        this.yunos$reinforcementBrokenTicks = view.getInt("YunosReinforcementBrokenTicks", 0);
    }

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
