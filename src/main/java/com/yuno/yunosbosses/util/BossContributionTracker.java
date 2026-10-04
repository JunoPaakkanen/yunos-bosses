package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.component.ManaComponent;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.entity.YunosBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player damage dealt to bosses to reward all significant contributors (>= 35% damage)
 * with first-kill permanent mana cap increases (+50 Absolute Max Mana Cap).
 */
public class BossContributionTracker {

    /**
     * Map from Boss Entity UUID -> Map of Player UUID -> Damage Dealt
     */
    private static final Map<UUID, Map<UUID, Float>> BOSS_DAMAGE_MAP = new HashMap<>();

    /**
     * Total damage accumulated on this boss
     */
    private static final Map<UUID, Float> BOSS_TOTAL_DAMAGE = new HashMap<>();

    public static final float MINIMUM_CONTRIBUTION_RATIO = 0.35F; // 35% damage contribution
    public static final float CAP_INCREASE_ON_BOSS_DEFEAT = 50.0F;

    /**
     * Records damage dealt by players to a boss.
     */
    public static void recordDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof YunosBossEntity boss)) return;
        if (victim.getWorld().isClient) return;
        if (amount <= 0.0F) return;

        // Determine if damage source was from a player or player-owned entity
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            UUID bossUuid = victim.getUuid();
            UUID playerUuid = player.getUuid();

            BOSS_DAMAGE_MAP.computeIfAbsent(bossUuid, k -> new HashMap<>())
                    .merge(playerUuid, amount, Float::sum);
            BOSS_TOTAL_DAMAGE.merge(bossUuid, amount, Float::sum);
        }
    }

    /**
     * Evaluates contributions and rewards qualifying players on boss death.
     */
    public static void onBossDeath(LivingEntity victim, DamageSource deathSource) {
        if (!(victim instanceof YunosBossEntity boss)) return;
        if (victim.getWorld().isClient) return;

        UUID bossUuid = victim.getUuid();
        String bossId = boss.getBossIdentifier();

        Map<UUID, Float> damageContributions = BOSS_DAMAGE_MAP.remove(bossUuid);
        Float totalDamageRecorded = BOSS_TOTAL_DAMAGE.remove(bossUuid);

        float maxHp = victim.getMaxHealth();
        // Base reference is at least the boss max HP, or total damage taken if healed during fight
        float damageBaseline = Math.max(maxHp, totalDamageRecorded != null ? totalDamageRecorded : maxHp);

        if (damageContributions == null || damageContributions.isEmpty()) return;

        if (!(victim.getWorld() instanceof ServerWorld serverWorld)) return;

        for (Map.Entry<UUID, Float> entry : damageContributions.entrySet()) {
            UUID playerUuid = entry.getKey();
            float playerDamage = entry.getValue();
            float contribution = playerDamage / damageBaseline;

            // Player qualifies if they contributed >= 35% damage
            if (contribution >= MINIMUM_CONTRIBUTION_RATIO) {
                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(playerUuid);
                if (player != null) {
                    rewardBossDefeat(player, bossId, contribution);
                }
            }
        }
    }

    private static void rewardBossDefeat(ServerPlayerEntity player, String bossId, float contribution) {
        ManaComponent manaComponent = ModEntityComponents.MANA.get(player);

        if (!manaComponent.hasDefeatedBoss(bossId)) {
            // First time defeating this boss!
            manaComponent.recordBossDefeat(bossId);
            float newAbsoluteCap = manaComponent.getAbsoluteMaxManaCap();

            int contributionPercent = Math.round(contribution * 100.0F);

            // Announce breakthrough to player
            player.sendMessage(
                    Text.literal("★ ")
                            .formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("LIMIT BREAK! ").formatted(Formatting.YELLOW, Formatting.BOLD))
                            .append(Text.literal("By conquering ").formatted(Formatting.WHITE))
                            .append(Text.literal(formatBossName(bossId)).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                            .append(Text.literal(" (" + contributionPercent + "% contribution), your absolute mana cap expanded to ").formatted(Formatting.WHITE))
                            .append(Text.literal(String.valueOf((int) newAbsoluteCap)).formatted(Formatting.AQUA, Formatting.BOLD))
                            .append(Text.literal("!").formatted(Formatting.WHITE)),
                    false
            );

            // Play celebratory audio & fanfares
            ServerWorld world = (ServerWorld) player.getWorld();
            world.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundCategory.PLAYERS,
                    2.0F,
                    0.8F
            );
            world.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT,
                    SoundCategory.PLAYERS,
                    1.8F,
                    1.2F
            );

            // Limit break fireworks/particles around player
            world.spawnParticles(
                    ParticleTypes.FIREWORK,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    35, 0.5, 0.6, 0.5, 0.15
            );
            world.spawnParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.8, 0.5, 0.2
            );
        }
    }

    private static String formatBossName(String bossId) {
        return switch (bossId) {
            case "ubel" -> "Übel";
            case "methode" -> "Methode";
            case "naoya" -> "Naoya Zen'in";
            default -> bossId.substring(0, 1).toUpperCase() + bossId.substring(1);
        };
    }
}
