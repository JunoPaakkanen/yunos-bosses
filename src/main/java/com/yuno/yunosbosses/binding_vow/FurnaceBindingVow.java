package com.yuno.yunosbosses.binding_vow;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FurnaceBindingVow implements BindingVow {
    public static final Identifier ID = Identifier.of("yunosbosses", "furnace");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Text getName() {
        return Text.literal("Furnace: Dust Ignition");
    }

    @Override
    public Text getSacrifice() {
        return Text.literal("Single-target Flame Arrow (Outside of domain finisher window).");
    }

    @Override
    public Text getGain() {
        return Text.literal("Thermobaric Kamino Finisher in Malevolent Shrine.");
    }

    @Override
    public Text getDescription() {
        return Text.literal("Restricts Flame Arrow to a single direct target outside the domain finisher. In return, unlocks the domain-wide Thermobaric Finisher.");
    }

    @Override
    public float getActivationManaCost(PlayerEntity player) {
        return 0.0F; // Purely strategic tactical vow
    }

    @Override
    public boolean canAccept(PlayerEntity player) {
        var spells = ModEntityComponents.SPELL_DATA.get(player);
        return spells.getKnownSpells().contains(ModSpells.SHRINE)
                || spells.getKnownSpells().contains(ModSpells.DOMAIN_EXPANSION_SHRINE);
    }

    @Override
    public Text getCannotAcceptReason(PlayerEntity player) {
        return Text.literal("Requires knowledge of the Shrine technique!");
    }

    @Override
    public void onPledged(ServerPlayerEntity player) {
    }

    @Override
    public void onSevered(ServerPlayerEntity player, boolean penalized) {
    }
}
