package com.yuno.yunosbosses.spell;

import net.minecraft.util.Formatting;

public enum SpellRarity {
    COMMON("Common", Formatting.GRAY, 0xFF9E9E9E),
    UNCOMMON("Uncommon", Formatting.GREEN, 0xFFA6E3A1),
    RARE("Rare", Formatting.BLUE, 0xFF89B4FA),
    EPIC("Epic", Formatting.LIGHT_PURPLE, 0xFFB026FF),
    LEGENDARY("Legendary", Formatting.GOLD, 0xFFFF8C00),
    SPECIAL("Special", Formatting.RED, 0xFFFF490D);

    private final String name;
    private final Formatting formatting;
    private final int colorHex; // For GUI borders / fills / text

    SpellRarity(String name, Formatting formatting, int colorHex) {
        this.name = name;
        this.formatting = formatting;
        this.colorHex = colorHex;
    }

    public String getName() {
        return name;
    }

    public Formatting getFormatting() {
        return formatting;
    }

    public int getColorHex() {
        return colorHex;
    }
}