package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.effect.AntiTeleportEffect;
import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPlayerTeleportTo(TeleportTarget target, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(player);
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPlayerServerTeleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, boolean resetCamera, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(player);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "requestTeleport", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPlayerRequestTeleport(double destX, double destY, double destZ, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(player);
            ci.cancel();
        }
    }

    @Inject(method = "requestTeleportOffset", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$cancelPlayerRequestTeleportOffset(double destX, double destY, double destZ, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(player);
            ci.cancel();
        }
    }
}
