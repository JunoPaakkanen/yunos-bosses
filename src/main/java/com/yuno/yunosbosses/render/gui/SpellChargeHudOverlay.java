package com.yuno.yunosbosses.render.gui;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

public class SpellChargeHudOverlay implements HudRenderCallback {

    private int previousChargeLevel = 0;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        PlayerEntity player = client.player;
        boolean isCharging = false;

        // Check if the player is holding and actively using the staff
        if (player.isUsingItem()) {
            ItemStack activeStack = player.getActiveItem();

            if (activeStack.getItem() instanceof StaffItem staffItem) {
                SpellComponent component = ModEntityComponents.SPELL_DATA.get(player);
                Spell activeSpell = component.getActiveSpell();

                // Only show HUD if the spell can be charged
                if (activeSpell != null && activeSpell.canBeCharged()) {
                    isCharging = true;

                    // Calculate charge level using the same logic as the StaffItem
                    int maxUseTime = staffItem.getMaxUseTime(activeStack, player);
                    int ticksUsed = maxUseTime - player.getItemUseTimeLeft();
                    int currentLevel = calculateChargeLevel(ticksUsed);

                    // Play a sound when the charge level changes
                    if (this.previousChargeLevel > 0 && currentLevel > this.previousChargeLevel) {
                        // Pitch 1.1 for Level 2, Pitch 1.5 for Level 3
                        float pitch = (currentLevel == 3) ? 1.5F : 1.1F;

                        // Play the XP orb "ding" sound locally for the client
                        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, pitch);
                    }

                    // Update the tracker for the next frame
                    this.previousChargeLevel = currentLevel;

                    // Fetch the translated spell name
                    String spellName = activeSpell.getName().getString();

                    renderChargeHud(context, client.textRenderer, spellName, currentLevel);
                }
            }
        }

        // If the player stops charging, reset the tracker
        if (!isCharging) {
            this.previousChargeLevel = 0;
        }
    }

    private int calculateChargeLevel(int ticksUsed) {
        if (ticksUsed < 40) return 1;       // < 2 seconds = Level 1
        if (ticksUsed < 100) return 2;      // 2 - 5 seconds = Level 2
        return 3;                           // 5+ seconds = Level 3
    }

    private void renderChargeHud(DrawContext context, TextRenderer textRenderer, String spellName, int currentLevel) {
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Position it 40 pixels to the left of the center crosshair
        int baseX = (screenWidth / 2) - 40;

        // Start rendering slightly above the very center of the screen
        int centerY = (screenHeight / 2) - 15;

        // Render the 3 levels from top to bottom (III, II, I)
        for (int i = 3; i >= 1; i--) {
            boolean isActive = (i == currentLevel);
            String text = spellName + " " + getRomanNumeral(i);

            // Calculate vertical position (Level 3 is top, 2 is middle, 1 is bottom)
            int yOffset = (3 - i) * 15;
            int yPos = centerY + yOffset;

            // Dark red for active, white for inactive
            int color = isActive ? 0xAA0000 : 0xFFFFFF;

            // Get text width so we can right-align the text cleanly
            int textWidth = textRenderer.getWidth(text);

            context.getMatrices().push();

            if (isActive) {
                // Shift 10 pixels to the left, and scale up by 15%
                context.getMatrices().translate(baseX - 10, yPos, 0);
                context.getMatrices().scale(1.15f, 1.15f, 1.0f);

                // Draw at -textWidth so the right edge of the text stays perfectly aligned
                context.drawTextWithShadow(textRenderer, text, -textWidth, 0, color);
            } else {
                // Normal size and position
                context.getMatrices().translate(baseX, yPos, 0);
                context.drawTextWithShadow(textRenderer, text, -textWidth, 0, color);
            }

            context.getMatrices().pop();
        }
    }

    private String getRomanNumeral(int level) {
        return switch (level) {
            case 3 -> "III";
            case 2 -> "II";
            default -> "I";
        };
    }
}
