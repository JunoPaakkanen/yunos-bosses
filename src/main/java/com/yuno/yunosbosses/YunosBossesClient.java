package com.yuno.yunosbosses;

import com.yuno.yunosbosses.event.ModKeybindings;
import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.render.BeamManager;
import com.yuno.yunosbosses.render.KillingMagicRenderer;
import com.yuno.yunosbosses.util.ActiveBeam;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class YunosBossesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register keybindings
        ModKeybindings.register();

        // Register the Receiver for the BeamPayload packet sent from the server
        ClientPlayNetworking.registerGlobalReceiver(BeamPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BeamManager.addBeam(payload.ownerUuid(), payload.start(), payload.range(), 40);
            });
        });
        // Tick event that runs 20 times per second, used to age the beams
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                boolean isFrozen = client.world.getTickManager().isFrozen();
                if (!isFrozen) {
                    BeamManager.tick();
                }
            }
        });
        // Render event that runs every single frame, used to draw the beams
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            for (ActiveBeam beam : BeamManager.ACTIVE_BEAMS) {
                // Pass beam data to the renderer
                KillingMagicRenderer.renderBeam(context, beam);
            }
        });

    }
}
