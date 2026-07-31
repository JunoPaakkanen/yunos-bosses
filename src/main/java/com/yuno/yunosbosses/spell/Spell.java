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

    public Spell(Identifier id) {
        this.id = id;
        canCastWithoutStaff = false;
        // Maps spell ID to assets/yunosbosses/textures/gui/spells/<spell_name>.png
        this.iconTexture = Identifier.of(YunosBosses.MOD_ID, "textures/gui/spells/" + id.getPath() + ".png");
    }

    public Spell(Identifier id, boolean canCastWithoutStaff) {
        this.id = id;
        this.canCastWithoutStaff = canCastWithoutStaff;
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
}
