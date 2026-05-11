package com.yuno.yunosbosses.network;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.SpellCastHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModMessages {
    public static void registerC2SPackets() {
        // Register the ID and Codec
        PayloadTypeRegistry.playC2S().register(SpellCyclePayload.ID, SpellCyclePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KickAttackPayload.ID, KickAttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CastSpellPayload.ID, CastSpellPayload.CODEC);

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
        // Spell casting (Staffless)
        ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                Spell spell = ModSpells.getSpell(payload.spellId());

                if (spell != null && spell.canCastWithoutStaff()) {
                    var component = ModEntityComponents.SPELL_DATA.get(player);
                    if (component.getActiveSpell() == spell) {
                        // Cast with empty ItemStack
                        SpellCastHelper.tryCastSpell(spell, player.getWorld(), player, ItemStack.EMPTY);
                    }
                }
            });
        });
    }
}