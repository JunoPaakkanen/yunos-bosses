package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

public class ManaHudRenderer {
    private static final Identifier MANA_BAR_TEXTURE =
            Identifier.of(YunosBosses.MOD_ID, "textures/gui/mana_bar.png");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    public static void renderManaBar(DrawContext guiGraphics, int screenWidth, int screenHeight, ClientPlayerEntity player) {
        var manaComponent = ModEntityComponents.MANA.get(player);

        float mana = manaComponent.getMana();
        float maxMana = manaComponent.getMaxMana();

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight - 55;

        // Draw background
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000);

        // Draw mana bar
        int filledWidth = (int) ((mana / maxMana) * (BAR_WIDTH - 2));
        guiGraphics.fill(x + 1, y + 1, x + 1 + filledWidth, y + BAR_HEIGHT - 1, 0xFF0066FF);
    }
}
