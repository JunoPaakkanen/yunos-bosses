package com.yuno.yunosbosses.animation;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.util.Identifier;

public class ModAnimations {

    // Action animation slot (for one-shot triggered animations like kicks and domain expansions)
    public static final Identifier ANIM_SLOT = Identifier.of("yunosbosses", "animation");
    // Continuous movement animation slot (for sprinting / running)
    public static final Identifier SPRINT_SLOT = Identifier.of("yunosbosses", "sprint_animation");

    // Animations
    public static final Identifier KICK_ANIM = Identifier.of("yunosbosses", "kick");
    public static final Identifier KICK_ANIM_2 = Identifier.of("yunosbosses", "kick_2");
    public static final Identifier DOMAIN_EXPANSION_SHRINE_ANIM = Identifier.of("yunosbosses", "domain_expansion");
    public static final Identifier RUN_ANIM = Identifier.of("yunosbosses", "run");
    public static final Identifier FLAME_ARROW_ANIM = Identifier.of("yunosbosses", "flame_arrow");

    private static RawAnimation runRawAnimation;

    public static RawAnimation getRunAnimation() {
        if (runRawAnimation == null || runRawAnimation.getAnimationStages().isEmpty() || runRawAnimation.getAnimationStages().getFirst().animation() == null) {
            runRawAnimation = PlayerRawAnimationBuilder.begin()
                    .then(RUN_ANIM, Animation.LoopType.LOOP)
                    .build();
        }
        return runRawAnimation;
    }

    public static void registerAnimations() {
        // One-shot action controller
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIM_SLOT,
                1500,
                player -> {
                    PlayerAnimationController controller = new PlayerAnimationController(player, (c, state, animSetter) -> PlayState.STOP);
                    controller.setFirstPersonModeHandler(c -> {
                        if (c.isActive()) {
                            Animation current = c.getCurrentAnimationInstance();
                            Animation flameArrow = PlayerAnimResources.getAnimation(FLAME_ARROW_ANIM);
                            if (current != null && current == flameArrow) {
                                return FirstPersonMode.THIRD_PERSON_MODEL;
                            }
                        }
                        return FirstPersonMode.NONE;
                    });
                    FirstPersonConfiguration config = new FirstPersonConfiguration(true, true, false, false);
                    controller.setFirstPersonConfigurationHandler(c -> config);
                    return controller;
                }
        );

        // Sprinting movement controller
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                SPRINT_SLOT,
                1000,
                player -> new PlayerAnimationController(player, (controller, state, animSetter) -> PlayState.STOP)
        );
    }
}
