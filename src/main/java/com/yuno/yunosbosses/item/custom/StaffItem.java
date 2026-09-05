package com.yuno.yunosbosses.item.custom;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.SpellCastHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class StaffItem extends Item {
    // Properties
    private final float powerMultiplier;

    public StaffItem(Settings settings, float powerMultiplier) {
        super(settings);
        this.powerMultiplier = powerMultiplier;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000; // Allows the staff to be held down indefinitely
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW; // Gives the player the charging animation
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack staff = user.getStackInHand(hand);
        if (world.isClient) return ActionResult.PASS;

        SpellComponent component = ModEntityComponents.SPELL_DATA.get(user);
        Spell active = component.getActiveSpell();

        if (active != null) {
            // --- INSTANT CAST SPELLS ---
            if (!active.canBeCharged()) {
                if (SpellCastHelper.tryCastSpell(active, world, user, staff)) {
                    // Success!
                    return ActionResult.SUCCESS;
                }
                // Not enough mana
                return ActionResult.FAIL;
            }

            // --- CHARGED SPELLS ---
            else {
                if (SpellCastHelper.canStartCasting(active, user)) {
                    user.setCurrentHand(hand);
                    return ActionResult.CONSUME;
                }
            }
        }
        return ActionResult.FAIL;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) return;

        int ticksUsed = this.getMaxUseTime(stack, user) - remainingUseTicks;
        Spell active = ModEntityComponents.SPELL_DATA.get(user).getActiveSpell();

        if (active != null) {
            // After 1 second, start draining additional mana for "Overcharging"
            if (ticksUsed > 20) {
                // Drain a small amount of mana every tick (5% of base cost per tick)
                float drainRate = active.getManaCost(user) * 0.05f;

                if (!ModEntityComponents.MANA.get(user).useMana(drainRate)) {
                    // Out of mana! Force release the spell early.
                    user.stopUsingItem();
                }
            }

            // TODO: In the future spawn particles here depending on the charge level
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient) return false;

        Spell active = ModEntityComponents.SPELL_DATA.get(user).getActiveSpell();
        if (active != null) {
            // Calculate how long they held it
            int ticksUsed = this.getMaxUseTime(stack, user) - remainingUseTicks;
            int chargeLevel = calculateChargeLevel(ticksUsed);

            // Execute the final cast
            SpellCastHelper.castChargedSpell(active, world, user, stack, chargeLevel);
            return true;
        }
        return false;
    }

    private int calculateChargeLevel(int ticksUsed) {
        if (ticksUsed < 40) return 1;       // Less than 2 seconds = Level 1
        if (ticksUsed < 100) return 2;      // 2 to 5 seconds = Level 2
        return 3;                           // 5+ seconds = Level 3
    }

    public float getPowerMultiplier() {
        return powerMultiplier;
    }
}
