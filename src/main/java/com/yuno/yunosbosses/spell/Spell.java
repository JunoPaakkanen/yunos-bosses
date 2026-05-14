package com.yuno.yunosbosses.spell;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

public abstract class Spell {
    private final Identifier id;

    private final boolean canCastWithoutStaff;

    public Spell(Identifier id) {
        this.id = id;
        canCastWithoutStaff = false;
    }

    public Spell(Identifier id, boolean canCastWithoutStaff) {
        this.id = id;
        this.canCastWithoutStaff = canCastWithoutStaff;
    }

    public abstract void cast(World world, LivingEntity caster, ItemStack staff);

    public float getManaCost(LivingEntity caster) {
        return 10.0F;
    }

    public Identifier getId() {
        return id;
    }

    public boolean canCastWithoutStaff() { return canCastWithoutStaff; }
}
