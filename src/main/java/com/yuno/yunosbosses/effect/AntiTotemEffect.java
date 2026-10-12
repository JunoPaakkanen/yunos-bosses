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

public class AntiTotemEffect extends StatusEffect implements MilkProof {

    public AntiTotemEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * Feedback when an entity's totem attempt is blocked.
     */
    public static void onTotemBlocked(LivingEntity living) {
        if (living.getWorld() instanceof ServerWorld sw) {
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 0.6F);
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0F, 0.7F);
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.0F, 1.5F);

            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, living.getX(), living.getY() + 1.0, living.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, living.getX(), living.getY() + 1.0, living.getZ(),
                    10, 0.25, 0.4, 0.25, 0.05);
        }
        if (living instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.literal("§4§l[Anti-Totem]§r §cYour Totem failed to activate!§r"), true);
        }
    }
}
