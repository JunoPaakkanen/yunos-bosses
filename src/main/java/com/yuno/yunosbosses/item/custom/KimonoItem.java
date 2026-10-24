package com.yuno.yunosbosses.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class KimonoItem extends Item {
    public KimonoItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.literal("Special: ").formatted(Formatting.GOLD)
                .append(Text.translatable("item.yunosbosses.kimono.tooltip.title").formatted(Formatting.AQUA)));
        textConsumer.accept(Text.translatable("item.yunosbosses.kimono.tooltip.description").formatted(Formatting.GRAY));
    }
}
