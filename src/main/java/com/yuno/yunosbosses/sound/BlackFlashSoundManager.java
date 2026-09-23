package com.yuno.yunosbosses.sound;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlackFlashSoundManager {

    public record SoundProfile(UUID uuid, List<SoundEvent> regularSounds, SoundEvent finisherSound) {}

    private static final Map<UUID, SoundProfile> PROFILE_CACHE = new ConcurrentHashMap<>();

    private static final SoundProfile DEFAULT_PROFILE = new SoundProfile(
            new UUID(0L, 0L),
            List.of(ModSounds.BLACK_FLASH_1, ModSounds.BLACK_FLASH_2, ModSounds.BLACK_FLASH_3),
            ModSounds.BLACK_FLASH_FINISHER_1
    );

    /**
     * Resolves or creates a deterministic combinatorial sound profile for an entity using their UUID.
     * Each entity is assigned exactly 3 distinct regular sounds from the 8 available, plus 1 finisher.
     */
    public static SoundProfile getProfileForEntity(LivingEntity entity) {
        if (entity == null || entity.getUuid() == null) {
            return DEFAULT_PROFILE;
        }
        return PROFILE_CACHE.computeIfAbsent(entity.getUuid(), BlackFlashSoundManager::buildCombinatorialProfile);
    }

    /**
     * Deterministically builds a unique profile:
     * - 1 finisher picked from the 3 finishers.
     * - 3 distinct regular sounds picked from the 8 regular sounds.
     */
    private static SoundProfile buildCombinatorialProfile(UUID uuid) {
        long seed = uuid.getMostSignificantBits() ^ (uuid.getLeastSignificantBits() * 31L);
        Random rng = new Random(seed);

        // Pick 1 deterministic finisher from the 3 variants
        int finisherIndex = Math.floorMod(rng.nextInt(), ModSounds.BLACK_FLASH_FINISHER_VARIANTS.size());
        SoundEvent finisher = ModSounds.BLACK_FLASH_FINISHER_VARIANTS.get(finisherIndex);

        // Pick 3 distinct regular sounds from the 8 variants without replacement
        List<SoundEvent> pool = new ArrayList<>(ModSounds.BLACK_FLASH_VARIANTS);
        List<SoundEvent> assignedRegulars = new ArrayList<>();
        for (int i = 0; i < 3 && !pool.isEmpty(); i++) {
            int pick = Math.floorMod(rng.nextInt(), pool.size());
            assignedRegulars.add(pool.remove(pick));
        }

        return new SoundProfile(uuid, List.copyOf(assignedRegulars), finisher);
    }

    /**
     * Plays the entity-specific Black Flash sound at the specified position.
     * In Minecraft's OpenAL audio engine, pitch directly controls the audio playback speed / rate.
     * We apply a slight randomized speed multiplier (0.91x to 1.09x) on every hit so each voiceline has organic speed & pitch variation.
     */
    public static void playBlackFlashSound(ServerWorld world, Vec3d pos, LivingEntity user, boolean isFinisher) {
        SoundProfile profile = getProfileForEntity(user);
        SoundEvent soundToPlay;

        if (isFinisher) {
            soundToPlay = profile.finisherSound();
        } else {
            List<SoundEvent> regulars = profile.regularSounds();
            if (regulars.isEmpty()) {
                soundToPlay = ModSounds.BLACK_FLASH;
            } else {
                int randomIndex = world.random.nextInt(regulars.size());
                soundToPlay = regulars.get(randomIndex);
            }
        }

        // Slight randomized speed/pitch variation (0.91x - 1.09x playback speed) on every hit
        float speedVariation = 0.91F + (world.random.nextFloat() * 0.18F);

        world.playSound(
                null,
                pos.x, pos.y, pos.z,
                soundToPlay,
                SoundCategory.PLAYERS,
                1.8F,
                speedVariation
        );
    }
}
