package com.yuno.yunosbosses;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.client.UbelRenderer;
import com.yuno.yunosbosses.event.ModKeybindings;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.particle.SlashImpactScissorsParticle;
import com.yuno.yunosbosses.render.SlashProjectileRenderer;
import com.yuno.yunosbosses.util.BeamManager;
import com.yuno.yunosbosses.render.DefensiveMagicRenderer;
import com.yuno.yunosbosses.render.KillingMagicRenderer;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

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
        EntityRendererRegistry.register(ModEntities.SLASH_PROJECTILE, SlashProjectileRenderer::new);

        // Register particle factories
        ParticleFactoryRegistry.getInstance().register(ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE, SlashImpactScissorsParticle.Factory::new);

        // Client tick event that runs 20 times per second
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                boolean isFrozen = client.world.getTickManager().isFrozen();
                if (!isFrozen) {
                    BeamManager.tick();
                    BarrierManager.tick(client.world);
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
                BarrierManager.addBarrier(payload.ownerUuid(), payload.position(), payload.direction(), payload.maxTicks(), true);
            });
        });
    }
}
