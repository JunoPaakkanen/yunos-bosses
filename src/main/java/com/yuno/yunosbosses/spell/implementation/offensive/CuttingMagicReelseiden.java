package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CuttingMagicReelseiden extends Spell {

    public CuttingMagicReelseiden(Identifier id) {super(id); }

    @Override
    public void cast(World world, PlayerEntity player, ItemStack staff) {
        if (!world.isClient) {
            // Spell implementation
            int maxRange = 5;
            float baseDamage = 50.0F;
            float cooldown = 15.0F;

            Vec3d look = player.getRotationVector();
            Vec3d start = player.getEyePos().add(look.multiply(1.0));

            SlashProjectileEntity projectile = new SlashProjectileEntity(ModEntities.SLASH_PROJECTILE, world);
            projectile.setPosition(start);

            //projectile.refreshPositionAndAngles(start.x, start.y, start.z, player.getYaw(), player.getPitch());
            // Velocity of 2.5 so the slash travels 2.5 blocks per tick, 5 blocks in 2 ticks
            projectile.setVelocity(look.multiply(2.5));
            projectile.setOwner(player);

            world.spawnEntity(projectile);
        }
    }
}
