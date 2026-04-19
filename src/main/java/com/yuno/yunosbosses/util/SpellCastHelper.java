package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class SpellCastHelper {
    public static boolean tryCastSpell(Spell spell, World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient) {
            return false;
        }

        var manaComponent = ModEntityComponents.MANA.get(caster);
        float manaCost = spell.getManaCost();

        if (manaComponent.useMana(manaCost)) {
            // Mana consumed successfully, cast the spell
            spell.cast(world, caster, staff);
            return true;
        }

        // Not enough mana
        return false;
    }
}

