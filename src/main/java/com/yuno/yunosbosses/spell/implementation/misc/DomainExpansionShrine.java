package com.yuno.yunosbosses.spell.implementation.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class DomainExpansionShrine extends DomainExpansion {

    public DomainExpansionShrine(Identifier id) {
        super(id);
    }

    @Override
    public Identifier getBarrierTexture() {
        return Identifier.of("yunosbosses", "textures/domain_expansion/shrine.png");
    }

    @Override
    public int getLifetimeTicks() {
        return 400; // 20 seconds
    }

    @Override
    public float getRadius() {
        return 12;
    }

    @Override
    public void onDomainEffect(Entity affectedEntity) {
        if (affectedEntity instanceof LivingEntity) {
            // Damage the entity every 10 ticks
            if (affectedEntity.age % 10 == 0) {
                affectedEntity.damage(affectedEntity.getDamageSources().magic(), 2.0F);
            }
        }
    }

    public float manaCost = 100.0F;

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}

}
