package com.yuno.yunosbosses.spell;

import com.yuno.yunosbosses.YunosBosses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

public abstract class Spell {
    private final Identifier id;
    private final boolean canCastWithoutStaff;
    private final Identifier iconTexture;
    private final SpellRarity rarity;

    public Spell(Identifier id, SpellRarity rarity) {
        this(id, false, rarity);
    }

    public Spell(Identifier id, boolean canCastWithoutStaff, SpellRarity rarity) {
        this.id = id;
        this.canCastWithoutStaff = canCastWithoutStaff;
        this.rarity = rarity;
        this.iconTexture = Identifier.of(YunosBosses.MOD_ID, "textures/gui/spells/" + id.getPath() + ".png");
    }

    // Default cast method
    public abstract void cast(World world, LivingEntity caster, ItemStack staff);

    // Used for casting spells with charge levels
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        this.cast(world, caster, staff);
    }

    public abstract Text getName();

    public abstract boolean canBeCharged();

    public float getManaCost(LivingEntity caster) {
        return 10.0F;
    }

    public Identifier getId() {
        return id;
    }

    public Identifier getIconTexture() {
        return iconTexture;
    }

    public boolean canCastWithoutStaff() { return canCastWithoutStaff; }

    public SpellRarity getRarity() { return rarity; }
}
