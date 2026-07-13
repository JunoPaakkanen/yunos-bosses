package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class SpellCastHelper {

    // Check if the player has enough mana to start casting the spell
    public static boolean canStartCasting(Spell spell, LivingEntity caster) {
        if (spell instanceof DomainExpansion && BarrierManager.hasActiveDomain(caster.getUuid())) {
            return false;
        }

        var manaComponent = ModEntityComponents.MANA.get(caster);
        float baseManaCost = spell.getManaCost(caster);

        return manaComponent.useMana(baseManaCost);
    }

    // Execute the spell with the given charge level
    public static void castChargedSpell(Spell spell, World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        spell.cast(world, caster, staff, chargeLevel);
    }

    // Used for immediate casting of spells
    public static boolean tryCastSpell(Spell spell, World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient) return false;

        if (canStartCasting(spell, caster)) {
            // Force a Level 1 cast instantly
            castChargedSpell(spell, world, caster, staff, 1);
            return true;
        }
        return false;
    }
}

