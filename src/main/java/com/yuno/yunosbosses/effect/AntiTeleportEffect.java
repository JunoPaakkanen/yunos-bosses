package com.yuno.yunosbosses.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class AntiTeleportEffect extends StatusEffect implements MilkProof {

    public AntiTeleportEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * Subtle audio & visual feedback when an entity's teleportation attempt is cancelled.
     */
    public static void onTeleportBlocked(LivingEntity living) {
        if (living.getWorld() instanceof ServerWorld sw) {
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.9F, 1.4F);
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.6F, 1.8F);

            sw.spawnParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + 1.0, living.getZ(),
                    10, 0.25, 0.35, 0.25, 0.05);
            sw.spawnParticles(ParticleTypes.PORTAL, living.getX(), living.getY() + 1.0, living.getZ(),
                    14, 0.3, 0.4, 0.3, 0.1);
        }
        if (living instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.literal("§5§l[Anti-Teleport]§r §cTeleportation is sealed!§r"), true);
        }
    }
}
