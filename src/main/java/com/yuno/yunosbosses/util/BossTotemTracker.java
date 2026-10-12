package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.implementation.misc.AntiTotemMagic;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the number of Totems of Undying used by players against bosses.
 * If a player uses 3 totems against a boss, the boss casts AntiTotemMagic on them.
 */
public class BossTotemTracker {
    // Boss UUID -> (Player UUID -> Totem Count)
    private static final Map<UUID, Map<UUID, Integer>> BOSS_TOTEM_COUNTS = new ConcurrentHashMap<>();

    public static void recordTotemUsed(LivingEntity boss, ServerPlayerEntity player) {
        if (boss == null || player == null || !boss.isAlive()) return;
        if (player.hasStatusEffect(ModEffects.ANTI_TOTEM)) return;

        UUID bossUuid = boss.getUuid();
        UUID playerUuid = player.getUuid();

        Map<UUID, Integer> counts = BOSS_TOTEM_COUNTS.computeIfAbsent(bossUuid, k -> new ConcurrentHashMap<>());
        int count = counts.merge(playerUuid, 1, Integer::sum);

        if (count == 1) {
            player.sendMessage(Text.literal("§c[!] The boss watches your Totem crumble... (1/3)§r"), true);
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.2F, 0.8F);
        } else if (count == 2) {
            player.sendMessage(Text.literal("§c§l[!] The boss grows furious at your repeated Totem use! (2/3)§r"), true);
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.5F, 0.6F);
        } else if (count >= 3) {
            // Cast AntiTotemMagic guaranteed on this specific player
            if (ModSpells.ANTI_TOTEM_MAGIC instanceof AntiTotemMagic antiTotem) {
                antiTotem.castOnTarget(boss.getWorld(), boss, player);
            }
            player.sendMessage(Text.literal("§4§l[!] The boss has shattered your connection to Totems! (3/3)§r"), false);
        }
    }

    public static void clearBoss(UUID bossUuid) {
        BOSS_TOTEM_COUNTS.remove(bossUuid);
    }
}
