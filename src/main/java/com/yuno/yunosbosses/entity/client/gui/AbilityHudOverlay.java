package com.yuno.yunosbosses.entity.client.gui;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public class AbilityHudOverlay implements HudRenderCallback {

    private static final Identifier ABILITY_ICONS = Identifier.of("yunosbosses", "textures/gui/abilities.png");

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var transformData = ModEntityComponents.TRANSFORMATION_DATA.get(client.player);

        // Only draw this if the player is transformed
        if (transformData.isTransformed()) {

            // Get the currently selected slot
            int selectedSlot = client.player.getInventory().selectedSlot;

            // Get the current size of the game window
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();

            // Calculate the center X and the bottom Y
            int centerX = width / 2;
            int bottomY = height - 22; // 22 pixels up from the bottom

            // X coordinates of the 3 icons
            int slot1X = centerX - 35;
            int slot2X = centerX - 8;
            int slot3X = centerX + 19;

            // --- DRAW THE ICONS ---

            // Ability 1 (Left side)
            context.drawTexture(ABILITY_ICONS, slot1X, bottomY, 0, 0, 16, 16, 48, 16);

            // Ability 2 (Center)
            context.drawTexture(ABILITY_ICONS, slot2X, bottomY, 16, 0, 16, 16, 48, 16);

            // Ability 3 (Right side)
            context.drawTexture(ABILITY_ICONS, slot3X, bottomY, 32, 0, 16, 16, 48, 16);

            // Determine which icon to highlight based on the selected slot
            int highlightX = slot1X;
            if (selectedSlot == 1) highlightX = slot2X;
            if (selectedSlot == 2) highlightX = slot3X;

            // Highlight the selected icon
            context.fill(highlightX, bottomY, highlightX + 16, bottomY + 16, 0x66FFFFFF);

            // Draw a border around the selected icon
            int borderX = highlightX - 2;
            int borderY = bottomY - 2;
            context.fill(borderX, borderY, borderX + 20, borderY + 2, 0xFFFFFFFF); // Top line
            context.fill(borderX, borderY + 18, borderX + 20, borderY + 20, 0xFFFFFFFF); // Bottom line
            context.fill(borderX, borderY, borderX + 2, borderY + 20, 0xFFFFFFFF); // Left line
            context.fill(borderX + 18, borderY, borderX + 20, borderY + 20, 0xFFFFFFFF); // Right line
        }
    }
}
