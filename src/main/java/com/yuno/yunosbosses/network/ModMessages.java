package com.yuno.yunosbosses.network;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModMessages {
    public static void registerC2SPackets() {
        // Register the ID and Codec
        PayloadTypeRegistry.playC2S().register(SpellCyclePayload.ID, SpellCyclePayload.CODEC);

        // Register the Receiver logic
        ServerPlayNetworking.registerGlobalReceiver(SpellCyclePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var component = ModEntityComponents.SPELL_DATA.get(context.player());
                component.cycleSpell();
                ModEntityComponents.SPELL_DATA.sync(context.player());
            });
        });


    }
}