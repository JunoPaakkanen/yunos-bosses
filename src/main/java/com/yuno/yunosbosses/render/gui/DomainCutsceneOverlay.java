package com.yuno.yunosbosses.render.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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

        // Banner dimensions
        int bannerHeight = 80;
        int bannerY = (screenHeight / 4) - (bannerHeight / 2); // Placed in the upper-mid screen

        // Draw a black background rectangle
        drawContext.fill(0, bannerY, screenWidth, bannerY + bannerHeight, 0x80000000); // 50% opaque black

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
                int modelY1 = bannerY;
                int modelX2 = (screenWidth / 2) - 60;

                // Pushing the bottom of the box way down shifts the model downwards,
                // hiding the legs so the chest/head are framed in the center.
                int modelY2 = bannerY + bannerHeight + 60;

                int modelSize = 45; // How zoomed in the model is

                // Enable Scissor (Clipping)
                drawContext.enableScissor(0, bannerY, screenWidth, bannerY + bannerHeight);

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

                // Disable Scissor
                drawContext.disableScissor();
            }
        }

        // --- DRAW TEXT ---
        String text = "Domain Expansion: " + DomainCutsceneManager.domainName;
        int textWidth = client.textRenderer.getWidth(text);
        int textX = (screenWidth / 2) - (textWidth / 2) + 40; // Offset to the right of the face
        int textY = bannerY + (bannerHeight / 2) - 4;

        // Draw with a shadow
        drawContext.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFFF);
    }
}
