package com.yuno.yunosbosses;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.client.*;
import com.yuno.yunosbosses.network.DomainCutscenePayload;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.particle.*;
import com.yuno.yunosbosses.render.gui.AbilityHudOverlay;
import com.yuno.yunosbosses.event.ModKeybindings;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.render.SlashProjectileRenderer;
import com.yuno.yunosbosses.render.gui.DomainCutsceneManager;
import com.yuno.yunosbosses.render.gui.DomainCutsceneOverlay;
import com.yuno.yunosbosses.util.BeamManager;
import com.yuno.yunosbosses.render.DefensiveMagicRenderer;
import com.yuno.yunosbosses.render.KillingMagicRenderer;
import com.yuno.yunosbosses.util.BarrierManager;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class YunosBossesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register keybindings
        ModKeybindings.register();

        // Register Renderers
        KillingMagicRenderer.register();
        DefensiveMagicRenderer.register();

        // Register Entity Renderers
        EntityRendererRegistry.register(ModEntities.UBEL, UbelRenderer::new);
        EntityRendererRegistry.register(ModEntities.METHODE, MethodeRenderer::new);
        EntityRendererRegistry.register(ModEntities.SLASH_PROJECTILE, SlashProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEVERED_TORSO, SeveredTorsoRenderer::new);
        EntityRendererRegistry.register(ModEntities.USELESS_CHICKEN, UselessChickenRenderer::new);
        EntityRendererRegistry.register(ModEntities.DOMAIN_SHRINE, DomainShrineRenderer::new);

        // Register particle factories
        ParticleFactoryRegistry.getInstance().register(ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE, SlashImpactScissorsParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.LAPSE_BLUE_PARTICLE, LapseBlueParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.REVERSAL_RED_PARTICLE, ReversalRedParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.DISMANTLE_A_PARTICLE, DismantleAParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.DISMANTLE_B_PARTICLE, DismantleBParticle.Factory::new);

        // Register animations
        ModAnimations.registerAnimations();

        // Register custom HUD
        HudRenderCallback.EVENT.register(new AbilityHudOverlay());
        HudRenderCallback.EVENT.register(new DomainCutsceneOverlay());

        // Client tick event that runs 20 times per second
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                boolean isFrozen = client.world.getTickManager().isFrozen();
                boolean isPaused = client.isPaused();
                if (!isFrozen && !isPaused) {
                    BeamManager.tick();
                    BarrierManager.tick(client.world);
                    DomainCutsceneManager.tick();
                }
            }
        });

        // Receivers

        // Receiver for Beams
        ClientPlayNetworking.registerGlobalReceiver(BeamPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BeamManager.addBeam(payload.ownerUuid(), payload.start(), payload.range(), 40, payload.useCustomStart(), payload.direction());
            });
        });

        // Receiver for Barriers
        ClientPlayNetworking.registerGlobalReceiver(BarrierPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Add to a client-side list of barriers for the renderer to draw
                BarrierManager.addBarrier(payload.ownerUuid(), payload.position(), payload.direction(), payload.maxTicks(), payload.texture(), true);
            });
        });

        // Receiver for Player Animations
        ClientPlayNetworking.registerGlobalReceiver(PlayerAnimationPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().world != null) {
                    PlayerEntity player = context.client().world.getPlayerByUuid(payload.playerUuid());

                    if (player != null) {
                        // Get the animation identifier sent by the server
                        Identifier animId = payload.animationId();

                        var layer = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer((AbstractClientPlayerEntity) player, ModAnimations.ANIM_SLOT);
                        if (layer instanceof PlayerAnimationController controller) {
                            controller.triggerAnimation(animId);
                        }
                    }
                }
            });
        });

        // Receiver for Domain Cutscenes
        ClientPlayNetworking.registerGlobalReceiver(DomainCutscenePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                DomainCutsceneManager.startCutscene(payload.casterUuid(), payload.domainName(), payload.durationTicks());
            });
        });
    }
}
