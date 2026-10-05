package com.yuno.yunosbosses.binding_vow;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GojoBindingVow implements BindingVow {
    public static final Identifier ID = Identifier.of("yunosbosses", "gojo");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Text getName() {
        return Text.literal("Indomitable Spirit");
    }

    @Override
    public Text getSacrifice() {
        return Text.literal("50% Max Mana and pauses mana regeneration.");
    }

    @Override
    public Text getGain() {
        return Text.literal("Lets you survive a lethal blow by sacrificing your upper body.");
    }

    @Override
    public Text getDescription() {
        return Text.literal("Consumes 50% max mana. Cheats a fatal blow and triggers emergency RCT, but permanently halts mana regeneration.");
    }

    @Override
    public float getActivationManaCost(PlayerEntity player) {
        var mana = ModEntityComponents.MANA.get(player);
        return mana.getMaxMana() / 2.0F;
    }

    @Override
    public boolean canAccept(PlayerEntity player) {
        var transform = ModEntityComponents.TRANSFORMATION_DATA.get(player);
        return !transform.isTransformed();
    }

    @Override
    public Text getCannotAcceptReason(PlayerEntity player) {
        var transform = ModEntityComponents.TRANSFORMATION_DATA.get(player);
        if (transform.isTransformed()) {
            return Text.literal("Resurrection already used in this life!");
        }
        return Text.literal("Cannot accept this vow right now.");
    }

    @Override
    public void onPledged(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(ModEffects.GOJO_BINDING_VOW, StatusEffectInstance.INFINITE, 0, false, true, true));
        // Pause mana regeneration while the binding vow is active
        ModEntityComponents.MANA.get(player).setManaRegen(0.0f);
    }

    @Override
    public void onSevered(ServerPlayerEntity player, boolean penalized) {
        player.removeStatusEffect(ModEffects.GOJO_BINDING_VOW);
        if (penalized) {
            // Broken prematurely by choice: restore normal mana regeneration
            ModEntityComponents.MANA.get(player).setManaRegen(0.25f);
        }
    }
}
