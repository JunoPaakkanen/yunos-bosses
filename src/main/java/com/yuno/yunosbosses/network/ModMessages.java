package com.yuno.yunosbosses.network;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModMessages {
    public static void registerC2SPackets() {
        // Register the ID and Codec
        PayloadTypeRegistry.playC2S().register(SpellCyclePayload.ID, SpellCyclePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KickAttackPayload.ID, KickAttackPayload.CODEC);

        // Register Receivers
        // Spell cycling
        ServerPlayNetworking.registerGlobalReceiver(SpellCyclePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var component = ModEntityComponents.SPELL_DATA.get(context.player());
                component.cycleSpell();
                ModEntityComponents.SPELL_DATA.sync(context.player());
            });
        });
        // Kicking
        ServerPlayNetworking.registerGlobalReceiver(KickAttackPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    var rotationVector = player.getRotationVector();
                    var offset = rotationVector.multiply(1.5);
                    var hitbox = player.getBoundingBox().offset(offset).expand(1.0);

                    var targets = player.getWorld().getOtherEntities(player, hitbox);

                    for (var target : targets) {
                        if (target instanceof LivingEntity livingTarget) {
                            livingTarget.damage(player.getDamageSources().playerAttack(player), 10.0f);
                        }
                    }
            });
        });
    }
}