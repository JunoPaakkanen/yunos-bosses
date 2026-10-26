package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class ReverseCursedTechnique extends Spell {

    public ReverseCursedTechnique(Identifier id, SpellRarity rarity) { super(id, true, rarity); }

    @Override
    public float getManaCost(LivingEntity caster) {
        boolean isTransformed = ModEntityComponents.TRANSFORMATION_DATA.get(caster).isTransformed();
        if (isTransformed) {
            return ModEntityComponents.MANA.get(caster).getMaxMana();
        }
        return 50.0F;
    }

    @Override
    public Text getManaCostTooltip(PlayerEntity player) {
        return Text.literal("Mana Cost: 50").formatted(Formatting.AQUA);
    }

    @Override
    public void appendVariantTooltips(PlayerEntity player, List<Text> tooltip) {
        tooltip.add(Text.literal("• Normal Cast: ").formatted(Formatting.GOLD)
                .append(Text.literal("Restores health").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("• Sneak Cast (in Burnout): ").formatted(Formatting.GOLD)
                .append(Text.literal("Resets Technique Burnout, inflicts Brain Damage").formatted(Formatting.GRAY)));
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            // If transformed
            var transformationComponent = ModEntityComponents.TRANSFORMATION_DATA.get(caster);
            if (transformationComponent.isTransformed()) {
                // Heal the caster to full health
                caster.heal(caster.getMaxHealth());

                // Reverse transformation
                transformationComponent.setTransformed(false);

                // Allow changing spells
                var spellComponent = ModEntityComponents.SPELL_DATA.get(caster);
                spellComponent.setCanChangeSpell(true);
                ModEntityComponents.SPELL_DATA.sync(caster);

                // Play voice line
                caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        ModSounds.HONORED_ONE, SoundCategory.NEUTRAL, 1.0f, 1.0f + (caster.getRandom().nextFloat() * 0.2f - 0.1f));

                // Allow mana regeneration
                var manaComponent = ModEntityComponents.MANA.get(caster);
                manaComponent.setManaRegen(0.25F);
                ModEntityComponents.MANA.sync(caster);

                if (world instanceof ServerWorld serverWorld) {
                    // Spawn particles
                    serverWorld.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            caster.getX(), caster.getY() + 1.0, caster.getZ(),
                            20, 0.5, 0.5, 0.5, 0.1
                    );

                    // Play sound
                    serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                            1.0f, 1.0f);
                }
            } else if (DomainClashManager.hasBurnout(caster.getUuid()) && caster.isSneaking()) {
                // Sneak cast while in technique burnout

                // Destroy and then heal the brain to refresh technique burnout
                DomainClashManager.removeBurnout(caster.getUuid());

                // Consequently, the caster suffers long-lasting brain damage
                int currentBrainDamage = transformationComponent.getBrainDamage();
                transformationComponent.setBrainDamage(currentBrainDamage + 1);
                ModEntityComponents.TRANSFORMATION_DATA.sync(caster);

            } else {
                // Normal cast
                caster.heal(3.0F);
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.reverse_cursed_technique");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
