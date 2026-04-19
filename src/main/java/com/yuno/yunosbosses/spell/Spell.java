package com.yuno.yunosbosses.spell;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

public abstract class Spell {
    private final Identifier id;

    public Spell(Identifier id) {
        this.id = id;
    }

    public abstract void cast(World world, LivingEntity caster, ItemStack staff);

    public float getManaCost() {
        return 10.0F;
    }

    public Identifier getId() {
        return id;
    }
}
