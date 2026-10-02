package com.yuno.yunosbosses.binding_vow;

import com.yuno.yunosbosses.component.BindingVowComponent;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BindingVowManager {

    public static final int BURNOUT_PENALTY_TICKS = 300; // 15 seconds of Technique Burnout

    /**
     * Checks if the given entity currently has the specified Binding Vow active.
     */
    public static boolean hasVow(LivingEntity entity, BindingVow vow) {
        return vow != null && hasVow(entity, vow.getId());
    }

    /**
     * Checks if the given entity currently has the specified Binding Vow active.
     */
    public static boolean hasVow(LivingEntity entity, Identifier vowId) {
        if (entity instanceof PlayerEntity player && vowId != null) {
            BindingVowComponent component = ModEntityComponents.BINDING_VOWS.get(player);
            return component != null && component.hasVow(vowId);
        }
        return false;
    }

    /**
     * Pledges/activates a Binding Vow on the server.
     */
    public static boolean activateVow(ServerPlayerEntity player, Identifier vowId) {
        BindingVow vow = ModBindingVows.get(vowId);
        if (vow == null) {
            player.sendMessage(Text.literal("§cUnknown binding vow!§r"), false);
            return false;
        }

        BindingVowComponent vowComponent = ModEntityComponents.BINDING_VOWS.get(player);
        if (vowComponent.hasVow(vow.getId())) {
            player.sendMessage(Text.literal("§eYou already have this binding vow active!§r"), false);
            return false;
        }

        if (!vow.canAccept(player)) {
            player.sendMessage(vow.getCannotAcceptReason(player).copy().formatted(net.minecraft.util.Formatting.RED), false);
            return false;
        }

        var manaComponent = ModEntityComponents.MANA.get(player);
        float manaCost = vow.getActivationManaCost(player);
        if (manaCost > 0) {
            if (manaComponent.getMana() < manaCost) {
                player.sendMessage(Text.literal("§cNot enough mana! You need " + (int) manaCost + " mana to pledge this vow.§r"), false);
                return false;
            }
            manaComponent.useMana(manaCost);
        }

        vowComponent.addVow(vow.getId());
        vow.onPledged(player);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.3f, 1.2f);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.1f, 1.4f);

        player.sendMessage(Text.literal("§6[Binding Vow Pledged] §aYou have bound yourself to §f" + vow.getName().getString() + "§a!§r"), false);
        return true;
    }

    /**
     * Dissolves/severs an active Binding Vow on the server.
     * @param penalized true if broken prematurely by choice (inflicts Technique Burnout), false if fulfilled naturally.
     */
    public static boolean revokeVow(ServerPlayerEntity player, Identifier vowId, boolean penalized) {
        BindingVow vow = ModBindingVows.get(vowId);
        if (vow == null) return false;

        BindingVowComponent vowComponent = ModEntityComponents.BINDING_VOWS.get(player);
        if (!vowComponent.hasVow(vow.getId())) {
            return false;
        }

        vowComponent.removeVow(vow.getId());
        vow.onSevered(player, penalized);

        if (penalized) {
            // Apply severe Technique Burnout penalty for breaking a supernatural pact
            DomainClashManager.applyBurnout(player.getUuid(), BURNOUT_PENALTY_TICKS);

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.7f);
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 1.2f, 0.8f);

            player.sendMessage(Text.literal("§c[Binding Vow Broken] You severed your pact with §f" + vow.getName().getString() + "§c! Technique Burnout inflicted (15s)!§r"), false);
        } else {
            player.sendMessage(Text.literal("§7[Binding Vow Fulfilled] Your pact with §f" + vow.getName().getString() + "§7 has concluded.§r"), false);
        }

        return true;
    }
}
