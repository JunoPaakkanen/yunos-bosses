package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.InnateHudData;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ManaHudRenderer {
    private static final Identifier MANA_BAR_BACKGROUND = Identifier.of(YunosBosses.MOD_ID, "textures/gui/mana_bar_background.png");
    private static final Identifier MANA_BAR_FILLING = Identifier.of(YunosBosses.MOD_ID, "textures/gui/mana_bar_filling.png");

    private static final int BAR_WIDTH = 202;
    private static final int BAR_HEIGHT = 17;
    private static final int FILL_START_X = 2;
    private static final int FILL_WIDTH = 198;

    // Texture row V offsets inside mana_bar_filling.png (256x256)
    private static final float V_NORMAL_BLUE = 0.0F;
    private static final float V_HIGHLIGHT_LIGHT_BLUE = 17.0F;
    private static final float V_INSUFFICIENT_RED = 34.0F;
    private static final float V_DRAIN_TRAIL_WHITE = 51.0F;

    // Smooth animation state for mana bar & trailing spell-drain effect
    private static float displayedMana = -1.0F;
    private static float trailingMana = -1.0F;
    private static float trailHoldSeconds = 0.0F;
    private static long lastRenderTimeMs = 0L;

    public static void renderManaBar(DrawContext guiGraphics, int screenWidth, int screenHeight, ClientPlayerEntity player) {
        var manaComponent = ModEntityComponents.MANA.get(player);
        var spellComponent = ModEntityComponents.SPELL_DATA.get(player);
        Spell activeSpell = spellComponent.getActiveSpell();
        Spell[] equippedSpells = spellComponent.getEquippedSpells();

        float mana = manaComponent.getMana();
        float maxMana = manaComponent.getMaxMana();
        float spellCost = (activeSpell != null) ? activeSpell.getManaCost(player) : 0.0F;

        updateAnimatedMana(mana, maxMana);

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight - 55;

        // 1. Draw background bar texture
        guiGraphics.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                MANA_BAR_BACKGROUND,
                x, y,
                0.0F, 0.0F,
                BAR_WIDTH, BAR_HEIGHT,
                256, 256
        );

        if (maxMana > 0.0F) {
            int filledPx = toFillPixels(displayedMana, maxMana);
            int trailingPx = toFillPixels(trailingMana, maxMana);

            if (activeSpell != null && spellCost > 0.0F) {
                int costPx = Math.max(1, Math.round(Math.min(1.0F, spellCost / maxMana) * FILL_WIDTH));

                if (mana >= spellCost) {
                    // Enough mana: normal blue for remaining mana, lighter sky-cyan highlight for the section that will be drained
                    int previewCostPx = Math.min(filledPx, costPx);
                    int remainPx = Math.max(0, filledPx - previewCostPx);

                    drawBarSlice(guiGraphics, x, y, 0, remainPx, V_NORMAL_BLUE, 0xFFFFFFFF);
                    drawBarSlice(guiGraphics, x, y, remainPx, filledPx, V_HIGHLIGHT_LIGHT_BLUE, 0xFFFFFFFF);
                } else {
                    // Not enough mana: highlight current mana in crimson red + dim red ghost bar up to required spell cost
                    int targetCostPx = Math.min(FILL_WIDTH, costPx);

                    if (targetCostPx > filledPx) {
                        drawBarSlice(guiGraphics, x, y, filledPx, targetCostPx, V_INSUFFICIENT_RED, 0x75B84040);
                    }
                    drawBarSlice(guiGraphics, x, y, 0, filledPx, V_INSUFFICIENT_RED, 0xFFFFFFFF);
                }
            } else {
                // No active spell cost: draw standard blue mana fill
                drawBarSlice(guiGraphics, x, y, 0, filledPx, V_NORMAL_BLUE, 0xFFFFFFFF);
            }

            // Draw smooth trailing drain afterimage when mana was just spent
            if (trailingPx > filledPx) {
                drawBarSlice(guiGraphics, x, y, filledPx, trailingPx, V_DRAIN_TRAIL_WHITE, 0xD8FFFFFF);
            }
        }

        // Variables for text rendering
        boolean isTransformed = ModEntityComponents.TRANSFORMATION_DATA.get(player).isTransformed();
        String manaString = (int) mana + "/" + (int) maxMana;
        String transformedString = "\"Nah, I'd Win\" (V)";
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // Calculate the text position for the mana count
        int textX = x + BAR_WIDTH / 2;
        int textY = y - 10;

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

        // Render Innate Technique HUD elements
        Spell innateSpell = null;
        for (Spell spell : equippedSpells) {
            if (spell != null && spell.isInnateTechnique()) {
                innateSpell = spell;
                break;
            }
        }

        if (innateSpell != null) {
            int gap = 10;
            int sideTextY = y - 10;

            InnateHudData leftData = innateSpell.getLeftInnateHudData(player, spellComponent);
            if (leftData != null && leftData.text() != null) {
                int leftX = x - gap - textRenderer.getWidth(leftData.text());
                guiGraphics.drawTextWithShadow(textRenderer, leftData.text(), leftX, sideTextY, leftData.color());
            }

            InnateHudData rightData = innateSpell.getRightInnateHudData(player, spellComponent);
            if (rightData != null && rightData.text() != null) {
                int rightX = x + BAR_WIDTH + gap;
                guiGraphics.drawTextWithShadow(textRenderer, rightData.text(), rightX, sideTextY, rightData.color());
            }
        }
    }

    private static int toFillPixels(float value, float maxMana) {
        if (value <= 0.05F || maxMana <= 0.0F) {
            return 0;
        }
        float ratio = Math.max(0.0F, Math.min(1.0F, value / maxMana));
        return Math.max(1, Math.round(ratio * FILL_WIDTH));
    }

    private static void drawBarSlice(DrawContext guiGraphics, int barX, int barY, int startPx, int endPx, float vOffset, int color) {
        int clampedStart = Math.max(0, Math.min(FILL_WIDTH, startPx));
        int clampedEnd = Math.max(0, Math.min(FILL_WIDTH, endPx));
        int width = clampedEnd - clampedStart;
        if (width <= 0) {
            return;
        }

        int drawX = barX + FILL_START_X + clampedStart;
        float uOffset = (float) (FILL_START_X + clampedStart);

        guiGraphics.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                MANA_BAR_FILLING,
                drawX, barY,
                uOffset, vOffset,
                width, BAR_HEIGHT,
                256, 256,
                color
        );
    }

    private static void updateAnimatedMana(float targetMana, float maxMana) {
        long now = Util.getMeasuringTimeMs();
        float dt = (lastRenderTimeMs == 0L) ? 0.0F : Math.min(0.1F, (now - lastRenderTimeMs) / 1000.0F);
        lastRenderTimeMs = now;

        if (displayedMana < 0.0F || maxMana <= 0.0F) {
            displayedMana = targetMana;
            trailingMana = targetMana;
            trailHoldSeconds = 0.0F;
            return;
        }

        displayedMana = Math.min(displayedMana, maxMana);
        trailingMana = Math.min(trailingMana, maxMana);

        // If a discrete chunk of mana was spent (e.g. spell cast), hold the trailing bar briefly before shrinking
        if (targetMana < displayedMana - 1.5F && trailingMana <= displayedMana + 0.5F) {
            trailHoldSeconds = 0.18F;
        }

        // Smoothly interpolate primary displayed mana toward actual mana
        float diff = targetMana - displayedMana;
        if (Math.abs(diff) < 0.05F) {
            displayedMana = targetMana;
        } else {
            float speed = (diff < 0.0F) ? 20.0F : 12.0F;
            displayedMana += diff * Math.min(1.0F, speed * dt);
        }

        // Update trailing drain bar
        if (trailingMana < displayedMana) {
            trailingMana = displayedMana;
            trailHoldSeconds = 0.0F;
        } else if (trailingMana > displayedMana) {
            if (trailHoldSeconds > 0.0F) {
                trailHoldSeconds = Math.max(0.0F, trailHoldSeconds - dt);
            } else {
                float trailDiff = displayedMana - trailingMana;
                if (Math.abs(trailDiff) < 0.1F) {
                    trailingMana = displayedMana;
                } else {
                    trailingMana += trailDiff * Math.min(1.0F, 6.5F * dt);
                }
            }
        }
    }
}
