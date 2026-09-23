package com.yuno.yunosbosses.render.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class DomainCutsceneOverlay implements HudRenderCallback {

    // Custom background texture for the banner behind the face
    private static final Identifier BANNER_BG = Identifier.of("yunosbosses", "textures/gui/domain_banner_bg.png");

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (DomainCutsceneManager.ticksRemaining <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Calculate a simple slide-in/slide-out animation based on ticks
        float progress = 1.0f - ((float) DomainCutsceneManager.ticksRemaining / DomainCutsceneManager.maxTicks);

        float animScale = 1.0f;
        float transitionThreshold = 0.15f; // Spends 15% opening, and 15% closing

        if (progress < transitionThreshold) {
            animScale = progress / transitionThreshold;
        } else if (progress > 1.0f - transitionThreshold) {
            animScale = (1.0f - progress) / transitionThreshold;
        }

        // Apply a Cubic Ease-Out curve to make it snap open smoothly
        animScale = (float) (1.0 - Math.pow(1.0 - animScale, 3));

        // Cinematic anamorphic bars (letterboxing)
        int letterboxHeight = (int) ((screenHeight / 12) * animScale);
        if (letterboxHeight > 0) {
            drawContext.fill(0, 0, screenWidth, letterboxHeight, 0xD0000000);
            drawContext.fill(0, screenHeight - letterboxHeight, screenWidth, screenHeight, 0xD0000000);
        }

        // Banner dimensions
        int maxBannerHeight = 80;
        int bannerHeight = (int) (maxBannerHeight * animScale);

        if (bannerHeight <= 0) return;

        int bannerY = (screenHeight / 4) - (bannerHeight / 2); // Placed in the upper-mid screen

        // Draw deep crimson-black background rectangle with subtle cursed border lines
        drawContext.fill(0, bannerY, screenWidth, bannerY + bannerHeight, 0xEE180204);
        drawContext.fill(0, bannerY - 1, screenWidth, bannerY, 0xFF991122);
        drawContext.fill(0, bannerY + bannerHeight, screenWidth, bannerY + bannerHeight + 1, 0xFF991122);

        // Enable scissor (clipping)
        drawContext.enableScissor(0, bannerY, screenWidth, bannerY + bannerHeight);

        // --- DRAW CASTER MODEL (3D) ---
        if (DomainCutsceneManager.casterUuid != null && client.world != null) {
            // Grab the live entity, try the player first, then fall back to any LivingEntity in the world
            LivingEntity caster = client.world.getPlayerByUuid(DomainCutsceneManager.casterUuid);

            if (caster == null) {
                for (var entity : client.world.getEntities()) {
                    if (entity.getUuid().equals(DomainCutsceneManager.casterUuid) && entity instanceof LivingEntity livingEntity) {
                        caster = livingEntity;
                        break;
                    }
                }
            }
            if (caster != null) {
                // Setup coordinates for the box the entity will be drawn in
                int modelX1 = (screenWidth / 2) - 140;
                int centerY = (screenHeight / 4);
                int modelY1 = centerY - (maxBannerHeight / 2);
                int modelX2 = (screenWidth / 2) - 60;
                int modelY2 = centerY + (maxBannerHeight / 2) + 60;

                int modelSize = 45; // How zoomed in the model is

                // Draw the 3D Entity
                net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                        drawContext,
                        modelX1, modelY1,
                        modelX2, modelY2,
                        modelSize,
                        0.0625f,
                        0f, 0f,
                        caster
                );
            }
        }

        // --- DRAW TEXT ---
        String text = "Domain Expansion: " + DomainCutsceneManager.domainName;
        int textWidth = client.textRenderer.getWidth(text);
        int textX = (screenWidth / 2) - (textWidth / 2) + 40; // Offset to the right of the face

        if (DomainCutsceneManager.isOpenBarrier) {
            int mainY = (screenHeight / 4) - 9;
            drawContext.drawTextWithShadow(client.textRenderer, text, textX, mainY, 0xFFFF3344);

            String subText = "Open Barrier";
            float subScale = 0.8f;
            int subTextWidth = client.textRenderer.getWidth(subText);
            float mainCenterX = textX + (textWidth / 2.0f);
            float subX = (mainCenterX / subScale) - (subTextWidth / 2.0f);
            float subY = (mainY + 13) / subScale;

            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().scale(subScale, subScale);
            drawContext.drawTextWithShadow(client.textRenderer, subText, (int) subX, (int) subY, 0xFFFF8855);
            drawContext.getMatrices().popMatrix();
        } else {
            int textY = (screenHeight / 4) - 4;
            drawContext.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFF3344);
        }

        // Disable Scissor
        drawContext.disableScissor();
    }
}
