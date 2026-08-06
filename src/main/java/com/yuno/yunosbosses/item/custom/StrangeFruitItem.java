package com.yuno.yunosbosses.item.custom;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.SpellLottery;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class StrangeFruitItem extends Item {

    public StrangeFruitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        // Only run on the server side
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            // Roll random rarity based on weights
            SpellRarity rolledRarity = SpellLottery.getRandomSpellRarity(world.getRandom());
            // Then pick a completely random spell of that rarity
            Spell rolledSpell = SpellLottery.getRandomSpell(world.getRandom(), rolledRarity);

            // Fallback to any random spell if the rarity roll failed
            if (rolledSpell == null) {
                rolledSpell = SpellLottery.getRandomSpell(world.getRandom());
            }

            // Grant the spell to the player
            var component = ModEntityComponents.SPELL_DATA.get(player);
            component.learnSpell(rolledSpell);

            // Notify the player with formatted rarity text
            player.sendMessage(
                    Text.literal("You unlocked: ")
                            .append(Text.literal(rolledSpell.getId().getPath().replace('_', ' ').toUpperCase())
                                    .formatted(rolledSpell.getRarity().getFormatting())),
                    false
            );
        }
        return super.finishUsing(stack, world, user);
    }
}
