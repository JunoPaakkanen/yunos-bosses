package com.yuno.yunosbosses.network;

import com.yuno.yunosbosses.binding_vow.BindingVow;
import com.yuno.yunosbosses.binding_vow.BindingVowManager;
import com.yuno.yunosbosses.binding_vow.ModBindingVows;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.SpellCastHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static void registerC2SPackets() {
        // Register C2S IDs and Codecs
        PayloadTypeRegistry.playC2S().register(SpellCyclePayload.ID, SpellCyclePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KickAttackPayload.ID, KickAttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CastSpellPayload.ID, CastSpellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EquipSpellPayload.ID, EquipSpellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DomainClashInputPayload.ID, DomainClashInputPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleBindingVowPayload.ID, ToggleBindingVowPayload.CODEC);

        // Register S2C IDs and Codecs
        PayloadTypeRegistry.playS2C().register(OpenBindingVowScreenPayload.ID, OpenBindingVowScreenPayload.CODEC);

        // Register Receivers
        // Spell cycling
        ServerPlayNetworking.registerGlobalReceiver(SpellCyclePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var component = ModEntityComponents.SPELL_DATA.get(context.player());
                component.cycleSpell();
                ModEntityComponents.SPELL_DATA.sync(context.player());
            });
        });
        // Equipping Spells
        ServerPlayNetworking.registerGlobalReceiver(EquipSpellPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                var component = ModEntityComponents.SPELL_DATA.get(player);

                // Unequip if empty or "empty"
                if (payload.spellId() == null || payload.spellId().isEmpty() || payload.spellId().equals("empty")) {
                    component.setEquippedSpell(payload.slot(), null);
                    return;
                }

                Identifier spellId = Identifier.tryParse(payload.spellId());
                if (spellId == null) return;

                // Fetch spell from ModSpells
                Spell spellToEquip = ModSpells.getSpell(spellId);

                // Make sure the spell is known and can be equipped
                if (spellToEquip != null && component.getKnownSpells().contains(spellToEquip)) {
                    if (spellToEquip.isInnateTechnique() && payload.slot() != 0) {
                        return; // Innate technique spells can only be equipped in slot 0
                    }
                    component.setEquippedSpell(payload.slot(), spellToEquip);
                }
            });
        });
        // Kicking
        ServerPlayNetworking.registerGlobalReceiver(KickAttackPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var component = ModEntityComponents.TRANSFORMATION_DATA.get(context.player());
                component.kick();
            });
        });
        // Spell casting (Quick cast / Keybind cast)
        ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                Spell spell = ModSpells.getSpell(payload.spellId());

                if (spell != null && spell.canCastWithoutStaff()) {
                    var component = ModEntityComponents.SPELL_DATA.get(player);
                    if (component.getActiveSpell() == spell) {
                        // Check if the player is holding a staff in main hand or off-hand and apply its attributes
                        ItemStack staff = ItemStack.EMPTY;
                        if (player.getMainHandStack().getItem() instanceof StaffItem) {
                            staff = player.getMainHandStack();
                        } else if (player.getOffHandStack().getItem() instanceof StaffItem) {
                            staff = player.getOffHandStack();
                        }
                        SpellCastHelper.tryCastSpell(spell, player.getWorld(), player, staff);
                    }
                }
            });
        });
        // Domain Clash input mash
        ServerPlayNetworking.registerGlobalReceiver(DomainClashInputPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                DomainClashManager.handlePlayerInput(context.player(), payload.clashId());
            });
        });
        // Binding Vow toggle (Pledge or Sever)
        ServerPlayNetworking.registerGlobalReceiver(ToggleBindingVowPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                BindingVow vow = ModBindingVows.get(payload.vowId());
                if (vow != null) {
                    var vowComponent = ModEntityComponents.BINDING_VOWS.get(player);
                    if (vowComponent.hasVow(vow.getId())) {
                        // Voluntarily severing the vow - penalize with Technique Burnout
                        BindingVowManager.revokeVow(player, vow.getId(), true);
                    } else {
                        // Pledging the vow
                        BindingVowManager.activateVow(player, vow.getId());
                    }
                }
            });
        });
    }
}
