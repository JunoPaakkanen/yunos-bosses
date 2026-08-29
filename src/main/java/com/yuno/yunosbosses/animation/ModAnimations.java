package com.yuno.yunosbosses.animation;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.util.Identifier;

public class ModAnimations {

    // Animation slot
    public static final Identifier ANIM_SLOT = Identifier.of("yunosbosses", "animation");

    // Animations
    public static final Identifier KICK_ANIM = Identifier.of("yunosbosses", "kick");
    public static final Identifier KICK_ANIM_2 = Identifier.of("yunosbosses", "kick_2");
    public static final Identifier DOMAIN_EXPANSION_SHRINE_ANIM = Identifier.of("yunosbosses", "domain_expansion");
    public static final Identifier RUN_ANIM = Identifier.of("yunosbosses", "run");

    private static boolean wasSprinting = false;
    private static RawAnimation runAnimation;

    public static void registerAnimations() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIM_SLOT,
                42,
                player -> new PlayerAnimationController(player, (controller, state, animSetter) -> PlayState.STOP)
        );
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIM_SLOT,
                42,
                player -> new PlayerAnimationController(player,
                        (controller, state, animSetter) -> {
                            boolean sprinting = player.isSprinting();

                            if (sprinting) {
                                if (runAnimation == null) {
                                    runAnimation = PlayerRawAnimationBuilder.begin()
                                            .then(RUN_ANIM, Animation.LoopType.LOOP)
                                            .build();
                                }
                                if (!wasSprinting) {
                                    controller.forceAnimationReset();
                                }
                                wasSprinting = true;
                                return animSetter.setAnimation(runAnimation);
                            }

                            wasSprinting = false;
                            return PlayState.STOP;
                        }
                )
        );
    }
}