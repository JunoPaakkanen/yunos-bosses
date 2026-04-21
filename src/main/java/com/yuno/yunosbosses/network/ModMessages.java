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
                var component = ModEntityComponents.TRANSFORMATION_DATA.get(context.player());
                component.kick();
            });
        });
    }
}