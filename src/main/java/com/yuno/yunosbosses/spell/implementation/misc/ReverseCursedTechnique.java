package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ReverseCursedTechnique extends Spell {

    public ReverseCursedTechnique(Identifier id) { super(id, true); }

    private LivingEntity currentCaster;

    @Override
    public float getManaCost() {
        if (currentCaster != null && currentCaster instanceof ServerPlayerEntity serverPlayer) {
            return ModEntityComponents.MANA.get(serverPlayer).getMaxMana();
        }
        return 80f;
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        this.currentCaster = caster;
        if (!world.isClient) {
            // Heal the caster to full health
            caster.heal(caster.getMaxHealth());

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

            // If transformed, reverse transformation
            var transformationComponent = ModEntityComponents.TRANSFORMATION_DATA.get(caster);
            if (transformationComponent.isTransformed()) {

                // Reset transformation
                transformationComponent.setTransformed(false);

                // Allow changing spells
                var spellComponent = ModEntityComponents.SPELL_DATA.get(caster);
                spellComponent.setCanChangeSpell(true);
                ModEntityComponents.SPELL_DATA.sync(caster);

                // Play voice line
                caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        ModSounds.HONORED_ONE, SoundCategory.NEUTRAL, 1.0f, 1.0f + (caster.getRandom().nextFloat() * 0.2f - 0.1f));
            }
        }
    }
}
