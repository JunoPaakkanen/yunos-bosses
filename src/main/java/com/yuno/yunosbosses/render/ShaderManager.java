package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.mixin.GameRendererAccessor;
import com.yuno.yunosbosses.render.gui.DomainCutsceneManager;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;

public class ShaderManager {

    // Post-processing effect id for Malevolent Shrine (assets/yunosbosses/post_effect/malevolent_shrine.json)
    private static final Identifier SHRINE_SHADER = Identifier.of("yunosbosses", "malevolent_shrine");
    private static final Identifier INVERT_SHADER = Identifier.of("yunosbosses", "malevolent_invert");

    public static int flashTicks = 0;

    public static void triggerFlash(int ticks) {
        flashTicks = Math.max(flashTicks, ticks);
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gameRenderer == null) return;

            if (flashTicks > 0) {
                flashTicks--;
            }

            boolean cutsceneFlash = DomainCutsceneManager.isInvertedFlashActive();
            boolean activeFlash = cutsceneFlash || (flashTicks > 0);

            // Inverted negative flash on cast activation / barrier explosion
            if (activeFlash) {
                if (!INVERT_SHADER.equals(client.gameRenderer.getPostProcessorId())) {
                    ((GameRendererAccessor) client.gameRenderer).invokeSetPostProcessor(INVERT_SHADER);
                }
                return;
            }

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
                if (!SHRINE_SHADER.equals(client.gameRenderer.getPostProcessorId())) {
                    ((GameRendererAccessor) client.gameRenderer).invokeSetPostProcessor(SHRINE_SHADER);
                }
            } else {
                // If they step out or the domain ends, turn it off!
                if (SHRINE_SHADER.equals(client.gameRenderer.getPostProcessorId()) || INVERT_SHADER.equals(client.gameRenderer.getPostProcessorId())) {
                    client.gameRenderer.clearPostProcessor();
                }
            }
        });
    }
}
