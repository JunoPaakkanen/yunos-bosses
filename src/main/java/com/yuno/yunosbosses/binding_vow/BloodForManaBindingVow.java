package com.yuno.yunosbosses.binding_vow;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BloodForManaBindingVow implements BindingVow {
    public static final Identifier ID = Identifier.of("yunosbosses", "blood_for_mana");
    
    // Conversion rate: 10 mana = 2 HP (1 heart). That means 1 HP = 5 Mana.
    public static final float MANA_PER_HP = 5.0F;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Text getName() {
        return Text.literal("Blood for Mana");
    }

    @Override
    public Text getSacrifice() {
        return Text.literal("Casting spells without sufficient mana consumes raw health (1 HP per 5 Mana).");
    }

    @Override
    public Text getGain() {
        return Text.literal("Allows casting and overcharging spells even when out of mana by burning your lifeforce.");
    }

    @Override
    public Text getDescription() {
        return Text.literal("Sacrifices flesh to channel supernatural power. When your mana is depleted or insufficient, spells drain your vitality directly to cover the deficit (bypassing damage reduction). In exchange, you are never prevented from casting due to lack of mana.");
    }

    @Override
    public float getActivationManaCost(PlayerEntity player) {
        return 0.0F;
    }

    @Override
    public boolean canAccept(PlayerEntity player) {
        return true;
    }

    @Override
    public Text getCannotAcceptReason(PlayerEntity player) {
        return Text.literal("");
    }

    @Override
    public void onPledged(ServerPlayerEntity player) {
    }

    @Override
    public void onSevered(ServerPlayerEntity player, boolean penalized) {
    }
}
