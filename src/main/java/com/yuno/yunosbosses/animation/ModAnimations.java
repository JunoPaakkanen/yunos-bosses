package com.yuno.yunosbosses.animation;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.util.Identifier;

public class ModAnimations {

    // Animation slot
    public static final Identifier ANIM_SLOT = Identifier.of("yunosbosses", "animation");

    // Animations
    public static final Identifier KICK_ANIM = Identifier.of("yunosbosses", "kick");
    public static final Identifier KICK_ANIM_2 = Identifier.of("yunosbosses", "kick_2");

    public static void registerAnimations() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIM_SLOT,
                42,
                player -> new PlayerAnimationController(player, (controller, state, animSetter) -> PlayState.STOP)
        );
    }
}