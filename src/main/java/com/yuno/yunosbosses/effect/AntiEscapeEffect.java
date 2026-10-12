package com.yuno.yunosbosses.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class AntiEscapeEffect extends StatusEffect implements MilkProof {

    public AntiEscapeEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    public static void onEscapeAttemptBlocked(LivingEntity living, String reason) {
        if (living.getWorld() instanceof ServerWorld sw) {
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 1.4F, 0.6F);
            sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5F, 1.6F);

            sw.spawnParticles(new DustParticleEffect(0x2B3A42, 1.2F), living.getX(), living.getY() + 1.0, living.getZ(),
                    12, 0.3, 0.4, 0.3, 0.02);
            sw.spawnParticles(ParticleTypes.SPLASH, living.getX(), living.getY() + 0.5, living.getZ(),
                    10, 0.3, 0.2, 0.3, 0.05);
        }
        if (living instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.literal("§6§l[Anti-Escape]§r §c" + reason + " is sealed!§r"), true);
        }
    }
}
