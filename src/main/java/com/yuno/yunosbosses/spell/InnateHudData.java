package com.yuno.yunosbosses.spell;

import net.minecraft.text.Text;

public record InnateHudData(Text text, int color) {
    public InnateHudData(String text, int color) {
        this(Text.literal(text), color);
    }

    public InnateHudData(String text) {
        this(Text.literal(text), 0xFFFFFFFF);
    }

    public InnateHudData(Text text) {
        this(text, 0xFFFFFFFF);
    }

    public static InnateHudData of(String text, int color) {
        return new InnateHudData(text, color);
    }

    public static InnateHudData of(String text) {
        return new InnateHudData(text);
    }

    public static InnateHudData of(Text text, int color) {
        return new InnateHudData(text, color);
    }

    public static InnateHudData of(Text text) {
        return new InnateHudData(text);
    }
}
