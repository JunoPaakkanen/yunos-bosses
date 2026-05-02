package com.yuno.yunosbosses.spell.implementation.summon;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.character.modified.UselessChickenEntity;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class SummonUselessChicken extends Spell {

    public SummonUselessChicken(Identifier id) { super(id); }

    float manaCost = 90.0F;

    @Override
    public float getManaCost() {return manaCost;}

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {

            UselessChickenEntity chicken = new UselessChickenEntity(ModEntities.USELESS_CHICKEN, world);

            chicken.refreshPositionAndAngles(caster.getX(), caster.getY(), caster.getZ(), 0, 0);

            world.spawnEntity(chicken);
        }
    }
}
