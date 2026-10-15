package com.yuno.yunosbosses.binding_vow;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Represents a Jujutsu Binding Vow: a supernatural pact of sacrifice and reward.
 */
public interface BindingVow {

    Identifier getId();

    Text getName();

    Text getSacrifice();

    Text getGain();

    Text getDescription();

    /**
     * Whether this vow is permanent once pledged and cannot be manually severed by players.
     * Can only be cleared/severed via admin commands.
     */
    default boolean isPermanent() {
        return false;
    }

    /**
     * Warning message shown when attempting to pledge this permanent vow.
     */
    default Text getPermanentWarning() {
        return Text.literal("This binding vow is PERMANENT. Once pledged, it cannot be deactivated or severed!");
    }

    /**
     * One-time mana consumed when establishing the vow.
     */
    float getActivationManaCost(PlayerEntity player);

    /**
     * Whether the player currently meets prerequisites to establish this vow.
     */
    boolean canAccept(PlayerEntity player);

    /**
     * Reason provided to the player if prerequisite check fails.
     */
    Text getCannotAcceptReason(PlayerEntity player);

    /**
     * Called when the player pledges the vow on the server.
     */
    void onPledged(ServerPlayerEntity player);

    /**
     * Called when the vow is dissolved on the server.
     * @param penalized true if broken prematurely by the player (inflicts technique burnout),
     *                  false if fulfilled naturally (e.g. fatal blow intercepted) or cleared by command.
     */
    void onSevered(ServerPlayerEntity player, boolean penalized);
}
