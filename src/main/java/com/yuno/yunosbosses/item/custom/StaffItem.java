package com.yuno.yunosbosses.item.custom;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.SpellCastHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class StaffItem extends Item {
    // Properties
    private final float powerMultiplier;

    public StaffItem(Settings settings, float powerMultiplier) {
        super(settings);
        this.powerMultiplier = powerMultiplier;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            SpellComponent component = ModEntityComponents.SPELL_DATA.get(user);
            Spell active = component.getActiveSpell();

            if (active != null) {
                ItemStack staff = user.getStackInHand(hand);
                if (SpellCastHelper.tryCastSpell(active, world, user, staff)) {
                    // Spell casted successfully
                    return TypedActionResult.success(staff);

                }
                // Not enough mana
                return TypedActionResult.fail(staff);
            }
        }
        return super.use(world, user, hand);
    }

    public float getPowerMultiplier() {
        return powerMultiplier;
    }
}
