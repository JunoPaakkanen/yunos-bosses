package com.yuno.yunosbosses.unlock;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Central manager for all custom progression and ability unlock systems.
 *
 * All unlock requirements, criteria checks, awakening effects, and progression
 * state tracking are consolidated here.
 */
public class UnlockManager {

    /**
     * Tracks players who have dropped below 15% health while their closed domain was active.
     */
    private static final Set<UUID> NEAR_DEATH_DOMAIN_CASTERS = new HashSet<>();

    /**
     * Initializes and registers lifecycle listeners required for unlock tracking.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(UnlockManager::onServerTick);
    }

    /**
     * Periodic server tick hook.
     * Evaluates continuous conditions such as health thresholds during active abilities.
     */
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            trackDomainNearDeath(player);
            // Future unlock tick monitors can be added here (e.g. survival timers, meditation, etc.)
        }
    }

    /**
     * Event hook triggered whenever a Black Flash is successfully landed.
     *
     * @param attacker The entity who landed the Black Flash
     * @param target   The entity that was struck
     */
    public static void onBlackFlash(LivingEntity attacker, LivingEntity target) {
        if (attacker.getWorld().isClient) return;

        // Check Open Domain awakening
        checkOpenDomainUnlock(attacker, target);

        // Future Black Flash-related unlocks can be hooked here:
        // checkBlackFlashChainUnlock(attacker);
        // checkReverseCursedTechniqueUnlock(attacker);
    }

    // =========================================================================
    // UNLOCK LOGIC: OPEN BARRIER DOMAIN EXPANSION
    // =========================================================================

    /**
     * Monitors active closed domains to check if the caster drops below 15% health.
     * If the domain expires or is no longer present, cleans up the tracking entry.
     */
    private static void trackDomainNearDeath(ServerPlayerEntity player) {
        ActiveBarrier activeDomain = BarrierManager.getActiveDomainBarrier(player.getUuid());

        if (activeDomain != null && !activeDomain.isOpenBarrier()) {
            // Check if player health dropped below 15% (3.0 HP on default 20 HP)
            if (!player.isDead() && (player.getHealth() / player.getMaxHealth()) <= 0.15F) {
                NEAR_DEATH_DOMAIN_CASTERS.add(player.getUuid());
            }
        } else {
            // Domain is no longer active or is already an open domain; reset state for this caster
            NEAR_DEATH_DOMAIN_CASTERS.remove(player.getUuid());
        }
    }

    /**
     * Evaluates the full unlock criteria for Open-Barrier Domain:
     * 1. Caster must be a player who has NOT yet unlocked Open Domain.
     * 2. Caster must currently own an active CLOSED Domain Expansion barrier.
     * 3. Target struck by Black Flash must be INSIDE that domain's radius.
     * 4. Caster must have experienced near-death (<= 15% HP) during this domain (or currently be <= 15% HP).
     */
    private static void checkOpenDomainUnlock(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;

        SpellComponent spellComponent = ModEntityComponents.SPELL_DATA.get(player);

        // Step 1: Already unlocked check
        if (spellComponent.unlockedOpenDomain()) return;

        // Step 2: Must currently own an active domain
        ActiveBarrier barrier = BarrierManager.getActiveDomainBarrier(player.getUuid());
        if (barrier == null) return;

        // Step 3: Must be a CLOSED domain (cannot unlock open domain while already inside an open domain)
        if (barrier.isOpenBarrier()) return;

        // Step 4: Target must be inside the domain
        if (target != null) {
            double distanceToCenter = target.getPos().distanceTo(barrier.getPosition());
            if (distanceToCenter > barrier.getRadius()) {
                return;
            }
        }

        // Step 5: Near-death requirement (either recorded during this domain or currently <= 15% health)
        boolean hasExperiencedNearDeath = NEAR_DEATH_DOMAIN_CASTERS.contains(player.getUuid())
                || (player.getHealth() / player.getMaxHealth()) <= 0.15F;

        if (!hasExperiencedNearDeath) return;

        // ==================== UNLOCK EXECUTION ====================
        spellComponent.unlockOpenDomain();
        NEAR_DEATH_DOMAIN_CASTERS.remove(player.getUuid());

        // Visual & Audio Awakening Feedback
        player.sendMessage(
                Text.literal("§4§l[Awakening] §cThrough near-death insight within the domain... An Open-Barrier Domain has been realized!"),
                false
        );

        player.getWorld().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.PLAYERS,
                2.0F,
                0.5F
        );
        player.getWorld().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.PLAYERS,
                1.2F,
                0.8F
        );

        if (player.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2, 0.0, 0.0, 0.0, 0.0
            );
            serverWorld.spawnParticles(
                    ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.2, 0.2, 0.2, 0.0
            );
        }
    }
}
