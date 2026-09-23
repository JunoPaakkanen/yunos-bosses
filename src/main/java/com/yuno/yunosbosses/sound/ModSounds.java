package com.yuno.yunosbosses.sound;

import com.yuno.yunosbosses.YunosBosses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModSounds {
    public static final SoundEvent REELSEIDEN_HIT = registerSoundEvent("reelseiden_hit");
    public static final SoundEvent STILL_ALIVE = registerSoundEvent("still_alive");
    public static final SoundEvent HONORED_ONE = registerSoundEvent("honored_one");
    public static final SoundEvent ANGRY_CHICKEN_AMBIENT = registerSoundEvent("angry_chicken_ambient");
    public static final SoundEvent ANGRY_CHICKEN_HURT = registerSoundEvent("angry_chicken_hurt");
    public static final SoundEvent DOMAIN_EXPANSION_SHRINE_1 = registerSoundEvent("domain_expansion_shrine_1");
    public static final SoundEvent DOMAIN_EXPANSION_SHRINE_2 = registerSoundEvent("domain_expansion_shrine_2");
    public static final SoundEvent FRAME_FREEZE = registerSoundEvent("frame_freeze");
    public static final SoundEvent FRAME_SHATTER = registerSoundEvent("frame_shatter");
    public static final SoundEvent FRAME_SHATTER_FROM_DAMAGE = registerSoundEvent("frame_shatter_from_damage");
    public static final SoundEvent FRAME_SHATTER_FINISHER = registerSoundEvent("frame_shatter_finisher");
    public static final SoundEvent FUGA = registerSoundEvent("fuga");

    // Black Flash base events
    public static final SoundEvent BLACK_FLASH = registerSoundEvent("black_flash");
    public static final SoundEvent BLACK_FLASH_FINISHER = registerSoundEvent("black_flash_finisher");

    // Black Flash regular variant sound events (1-8)
    public static final SoundEvent BLACK_FLASH_1 = registerSoundEvent("black_flash_1");
    public static final SoundEvent BLACK_FLASH_2 = registerSoundEvent("black_flash_2");
    public static final SoundEvent BLACK_FLASH_3 = registerSoundEvent("black_flash_3");
    public static final SoundEvent BLACK_FLASH_4 = registerSoundEvent("black_flash_4");
    public static final SoundEvent BLACK_FLASH_5 = registerSoundEvent("black_flash_5");
    public static final SoundEvent BLACK_FLASH_6 = registerSoundEvent("black_flash_6");
    public static final SoundEvent BLACK_FLASH_7 = registerSoundEvent("black_flash_7");
    public static final SoundEvent BLACK_FLASH_8 = registerSoundEvent("black_flash_8");

    // Black Flash finisher variant sound events (1-3)
    public static final SoundEvent BLACK_FLASH_FINISHER_1 = registerSoundEvent("black_flash_finisher_1");
    public static final SoundEvent BLACK_FLASH_FINISHER_2 = registerSoundEvent("black_flash_finisher_2");
    public static final SoundEvent BLACK_FLASH_FINISHER_3 = registerSoundEvent("black_flash_finisher_3");

    public static final List<SoundEvent> BLACK_FLASH_VARIANTS = List.of(
            BLACK_FLASH_1, BLACK_FLASH_2, BLACK_FLASH_3, BLACK_FLASH_4,
            BLACK_FLASH_5, BLACK_FLASH_6, BLACK_FLASH_7, BLACK_FLASH_8
    );

    public static final List<SoundEvent> BLACK_FLASH_FINISHER_VARIANTS = List.of(
            BLACK_FLASH_FINISHER_1, BLACK_FLASH_FINISHER_2, BLACK_FLASH_FINISHER_3
    );

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of("yunosbosses", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        YunosBosses.LOGGER.info("Registering Mod Sounds for " + YunosBosses.MOD_ID);
    }
}
