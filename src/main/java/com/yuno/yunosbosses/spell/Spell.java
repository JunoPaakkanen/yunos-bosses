package com.yuno.yunosbosses.spell;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

public abstract class Spell {
    private final Identifier id;

    public Spell(Identifier id) {
        this.id = id;
    }

    public abstract void cast(World world, PlayerEntity player, ItemStack staff);

    public Identifier getId() {
        return id;
    }
}
