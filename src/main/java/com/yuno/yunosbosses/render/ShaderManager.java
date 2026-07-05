package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.mixin.GameRendererAccessor;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansionShrine;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;

public class ShaderManager {

    // Point this to your post-processing pipeline JSON
    private static final Identifier SHRINE_SHADER = Identifier.of("yunosbosses", "shaders/post/malevolent_shrine.json");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gameRenderer == null) return;

            boolean insideShrine = false;

            // Check if a player is inside the barrier
            for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
                double distanceSq = client.player.getPos().squaredDistanceTo(barrier.getPosition());

                // Identify if this barrier is Malevolent Shrine
                boolean isShrine = barrier.getTexture() != null && barrier.getTexture().getPath().contains("shrine");

                if (isShrine) {
                    if (distanceSq <= (barrier.getRadius() * barrier.getRadius())) {
                        insideShrine = true;
                        break;
                    }
                }
            }

            // LOAD OR UNLOAD SHADER
            if (insideShrine) {
                // If the shader isn't already loaded, turn it on!
                if (client.gameRenderer.getPostProcessor() == null) {
                    ((GameRendererAccessor) client.gameRenderer).invokeLoadPostProcessor(SHRINE_SHADER);
                }
            } else {
                // If they step out or the domain ends, turn it off!
                if (client.gameRenderer.getPostProcessor() != null) {
                    client.gameRenderer.disablePostProcessor();
                }
            }
        });
    }
}
