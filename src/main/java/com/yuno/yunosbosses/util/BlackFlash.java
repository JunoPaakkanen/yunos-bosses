package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.data.client.Models;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class BlackFlash {

    public static void blackFlash(LivingEntity user, LivingEntity target) {

        // Deal damage
        target.damage(target.getDamageSources().indirectMagic(user, user), 10f);

        // Enable wall slamming damage
        if (target instanceof WallSlamData data) {
            // During these 30 ticks the target will be able to take damage from slamming into a wall
            data.yunos$setWallSlamTimer(30);
        }

        // Deal knockback
        Vec3d pushDirection = target.getPos().subtract(user.getPos()).normalize();
        double pushStrength = 4.0;
        target.addVelocity(pushDirection.x * pushStrength, 0.2, pushDirection.z * pushStrength);
        target.velocityModified = true;

        // Grant mana
        var manaComponent = ModEntityComponents.MANA.get(user);
        manaComponent.addMana(manaComponent.getMaxMana() * 0.1f);
    }
}
