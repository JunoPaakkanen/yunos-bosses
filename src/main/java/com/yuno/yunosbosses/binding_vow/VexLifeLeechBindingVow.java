package com.yuno.yunosbosses.binding_vow;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VexLifeLeechBindingVow implements BindingVow {
    public static final Identifier ID = Identifier.of("yunosbosses", "vex_life_leech");
    public static final float HEAL_AMOUNT = 3.0F; // 1.5 hearts
    public static final float MIN_HEALTH_PERCENTAGE_TO_PLEDGE = 0.90F; // Must be at least 90% HP to pledge

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Text getName() {
        return Text.literal("Vexing Sacrilege: Life Leech");
    }

    @Override
    public Text getSacrifice() {
        return Text.literal("Requires >= 90% HP to pledge. Summon Vex cannot be cast above 50% HP.");
    }

    @Override
    public Text getGain() {
        return Text.literal("Summoned Vexes heal you for 1.5 hearts (3.0 HP) upon death.");
    }

    @Override
    public Text getDescription() {
        return Text.literal("Pledges your life force to the spirits. Vexes summoned under this vow transfer vital essence upon their death (+3.0 HP). In exchange, this vow can only be pledged while healthy (at least 90% HP), and Summon Vex cannot be cast while above 50% HP.");
    }

    @Override
    public float getActivationManaCost(PlayerEntity player) {
        return 0.0F;
    }

    @Override
    public boolean canAccept(PlayerEntity player) {
        var spells = ModEntityComponents.SPELL_DATA.get(player);
        if (!spells.getKnownSpells().contains(ModSpells.SUMMON_VEX_SPELL)) {
            return false;
        }

        // Must have at least 90% HP to pledge
        return player.getHealth() >= (player.getMaxHealth() * MIN_HEALTH_PERCENTAGE_TO_PLEDGE);
    }

    @Override
    public Text getCannotAcceptReason(PlayerEntity player) {
        var spells = ModEntityComponents.SPELL_DATA.get(player);
        if (!spells.getKnownSpells().contains(ModSpells.SUMMON_VEX_SPELL)) {
            return Text.literal("Requires knowledge of the Summon Vex spell!");
        }

        if (player.getHealth() < (player.getMaxHealth() * MIN_HEALTH_PERCENTAGE_TO_PLEDGE)) {
            return Text.literal("Must be healthy (at least 90% HP) to pledge this vow!");
        }

        return Text.literal("Cannot pledge this vow.");
    }

    @Override
    public void onPledged(ServerPlayerEntity player) {
    }

    @Override
    public void onSevered(ServerPlayerEntity player, boolean penalized) {
    }
}
