package com.yuno.yunosbosses.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ManaHudRenderer {
    private static final Identifier MANA_BAR_TEXTURE = Identifier.of(YunosBosses.MOD_ID, "textures/gui/mana_bar.png");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    public static void renderManaBar(DrawContext guiGraphics, int screenWidth, int screenHeight, ClientPlayerEntity player) {
        var manaComponent = ModEntityComponents.MANA.get(player);
        Spell activeSpell = ModEntityComponents.SPELL_DATA.get(player).getActiveSpell();

        float mana = manaComponent.getMana();
        float maxMana = manaComponent.getMaxMana();
        float spellCost;
        if (activeSpell != null) {
            spellCost = activeSpell.getManaCost(player);
        } else {
            spellCost = 0;
        }

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight - 55;

        // Draw background
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000);

        // Draw mana bar
        // The bar is purple if transformed, blue otherwise
        boolean isTransformed = ModEntityComponents.TRANSFORMATION_DATA.get(player).isTransformed();
        int manaBarColor = isTransformed ? 0xFFAA00FF : 0xFF0066FF;
        int filledWidth = (int) ((mana / maxMana) * (BAR_WIDTH - 2));
        guiGraphics.fill(x + 1, y + 1, x + 1 + filledWidth, y + BAR_HEIGHT - 1, manaBarColor);

        float effectiveCost = Math.min(spellCost, mana);
        int costWidth = (int) ((effectiveCost / maxMana) * (BAR_WIDTH - 2));

        if (costWidth > 0) {
            // The bar starts at the current mana level (right) and extends left (x - costWidth)
            int costXStart = x + 1 + filledWidth - costWidth;
            int costXEnd = x + 1 + filledWidth;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Render mana cost preview
            if (spellCost > mana) {
                // Red if not enough mana
                guiGraphics.fill(costXStart, y + 1, costXEnd, y + BAR_HEIGHT - 1, 0xFFFF4444);
            } else {
                // Lighter overlay color if enough mana
                guiGraphics.fill(costXStart, y + 1, costXEnd, y + BAR_HEIGHT - 1, 0xAAADD8E6);
            }

            RenderSystem.disableBlend();
        }

        // Variables for text rendering
        String manaString = (int)mana + "/" + (int)maxMana;
        String transformedString = "\"Nah, I'd Win\" (V)";
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // Calculate the text position for the mana count
        int textX = x + BAR_WIDTH / 2;
        int textY = y -10;

        // Render the active spell name
        if (activeSpell != null) {
            Text spellName = activeSpell.getName();
            int spellTextY = textY - 12;

            guiGraphics.drawCenteredTextWithShadow(textRenderer, spellName, textX, spellTextY, 0xFFAA00AA);
        }

        // Render the text for the mana count
        if (isTransformed) {
            guiGraphics.drawCenteredTextWithShadow(textRenderer, Text.literal(transformedString), textX, textY, 0xFFFF0000);
        } else {
            guiGraphics.drawCenteredTextWithShadow(textRenderer, Text.literal(manaString), textX, textY, 0xFFFFFFFF);
        }
    }
}
