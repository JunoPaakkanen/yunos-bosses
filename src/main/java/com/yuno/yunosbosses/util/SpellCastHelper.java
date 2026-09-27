package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SpellCastHelper {

    // Check if the player has enough mana to start casting the spell
    public static boolean canStartCasting(Spell spell, LivingEntity caster) {
        if (!(caster instanceof PlayerEntity player)) {
            if (spell instanceof DomainExpansion && DomainClashManager.hasBurnout(caster.getUuid())) {
                return false;
            }
            return true;
        }

        if (spell instanceof DomainExpansion && DomainClashManager.hasBurnout(player.getUuid())) {
            int sec = DomainClashManager.getBurnoutSeconds(player.getUuid());
            player.sendMessage(Text.literal("§cCannot expand domain! Technique Burnout (" + sec + "s remaining)§r"), true);
            return false;
        }

        var spellComponent = ModEntityComponents.SPELL_DATA.get(player);
        if (spell instanceof DomainExpansion && BarrierManager.hasActiveDomain(player.getUuid()) && !spellComponent.hasAltCastWindow(spell)) {
            return false;
        }

        if (spell == ModSpells.SHRINE && spellComponent.getShrineCooldown() > 0) {
            return false;
        }

        var manaComponent = ModEntityComponents.MANA.get(player);
        float baseManaCost = spell.getManaCost(player);

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
