package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ModEntityComponents;
import net.minecraft.data.client.Models;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class BlackFlash {

    public static void blackFlash(LivingEntity user, LivingEntity target) {
        if (user.getWorld().isClient) return;

        // Deal damage
        target.damage(target.getDamageSources().indirectMagic(user, user), 10f);

        ServerWorld serverWorld = (ServerWorld) user.getWorld();

        // Spawn particles
        Vector3f pureRed = new Vector3f(1.0f, 0.0f, 0.0f); // Red color
        DustParticleEffect redDust = new DustParticleEffect(pureRed, 1.2f);
        serverWorld.spawnParticles(
                redDust,
                target.getX(), target.getBodyY(0.5D), target.getZ(),
                10,
                0.5, 0.1, 0.5,
                0.04
        );

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

    public static void blackFlashFinisher(LivingEntity user, LivingEntity target) {
        if (user.getWorld().isClient) return;

        // Deal damage
        target.damage(target.getDamageSources().indirectMagic(user, user), 20f);

        ServerWorld serverWorld = (ServerWorld) user.getWorld();

        // Spawn particles
        Vector3f pureRed = new Vector3f(1.0f, 0.0f, 0.0f); // Red color
        DustParticleEffect redDust = new DustParticleEffect(pureRed, 1.2f);
        serverWorld.spawnParticles(
                redDust,
                target.getX(), target.getBodyY(0.5D), target.getZ(),
                20,
                1.0, 0.1, 1.0,
                0.04
        );

        // Enable wall slamming damage
        if (target instanceof WallSlamData data) {
            // During these 30 ticks the target will be able to take damage from slamming into a wall
            data.yunos$setWallSlamTimer(30);
        }

        // Deal knockback
        Vec3d pushDirection = target.getPos().subtract(user.getPos()).normalize();
        double pushStrength = 6.0;
        target.addVelocity(pushDirection.x * pushStrength, 0.2, pushDirection.z * pushStrength);
        target.velocityModified = true;

        // Grant mana
        var manaComponent = ModEntityComponents.MANA.get(user);
        manaComponent.addMana(manaComponent.getMaxMana() * 0.6f);
    }

    // Chance to black flash with a default chance of 3%
    public static boolean blackFlashChance(LivingEntity user, LivingEntity target) {
        if (user.getWorld().isClient) return false;

        // 3% probability of black flash
        if (user.getWorld().random.nextFloat() < 0.03f) {
            blackFlash(user, target);
            return true;
        }
        return false;
    }

    // Chance to black flash with a custom chance
    public static boolean blackFlashChance(LivingEntity user, LivingEntity target, float chance) {
        if (user.getWorld().isClient) return false;

        // Custom chance of black flash
        if (user.getWorld().random.nextFloat() < chance) {
            blackFlash(user, target);
            return true;
        }
        return false;
    }
}
