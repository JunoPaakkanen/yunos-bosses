package com.yuno.yunosbosses.item.custom;

import com.yuno.yunosbosses.component.ManaComponent;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.SpellLottery;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class StrangeFruitItem extends Item {

    public static final float MANA_INCREASE_PER_FRUIT = 10.0F;

    public StrangeFruitItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        // Only run on the server side
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            // 1. Permanently increase player's Max Mana pool (up to current absolute cap)
            ManaComponent manaComponent = ModEntityComponents.MANA.get(player);
            float currentMax = manaComponent.getMaxMana();
            float absoluteCap = manaComponent.getAbsoluteMaxManaCap();

            if (currentMax < absoluteCap) {
                float newMax = Math.min(absoluteCap, currentMax + MANA_INCREASE_PER_FRUIT);
                manaComponent.setMaxMana(newMax);
                manaComponent.addMana(MANA_INCREASE_PER_FRUIT); // Also replenish current mana

                player.sendMessage(
                        Text.literal("✦ Your maximum mana has expanded to ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal(String.valueOf((int) newMax)).formatted(Formatting.GOLD, Formatting.BOLD))
                                .append(Text.literal("/" + (int) absoluteCap + "!").formatted(Formatting.AQUA)),
                        false
                );

                // Cosmic chime audio feedback
                world.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                        SoundCategory.PLAYERS,
                        1.5F,
                        1.3F
                );
            } else {
                player.sendMessage(
                        Text.literal("✦ Your mana pool has reached your current cap (" + (int) absoluteCap + "). Defeat powerful bosses to break your limits further!").formatted(Formatting.DARK_AQUA),
                        false
                );
            }

            // 2. Roll random spell unlock
            SpellRarity rolledRarity = SpellLottery.getRandomSpellRarity(world.getRandom());
            Spell rolledSpell = SpellLottery.getRandomSpell(world.getRandom(), rolledRarity);

            if (rolledSpell == null) {
                rolledSpell = SpellLottery.getRandomSpell(world.getRandom());
            }

            if (rolledSpell != null) {
                var spellComponent = ModEntityComponents.SPELL_DATA.get(player);
                spellComponent.learnSpell(rolledSpell);

                if (spellComponent.getKnownSpells().contains(rolledSpell)) {
                    player.sendMessage(
                            Text.literal("Too bad. You've already unlocked: ")
                                    .append(rolledSpell.getName().copy().formatted(rolledSpell.getRarity().getFormatting())),
                            false
                    );
                } else {
                    player.sendMessage(
                            Text.literal("You unlocked: ")
                                    .append(rolledSpell.getName().copy().formatted(rolledSpell.getRarity().getFormatting())),
                            false
                    );
                }
            }

            // Awakening particles
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        25, 0.4, 0.5, 0.4, 0.5
                );
                serverWorld.spawnParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        12, 0.3, 0.4, 0.3, 0.1
                );
            }
        }
        return super.finishUsing(stack, world, user);
    }
}
