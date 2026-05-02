package com.yuno.yunosbosses.sound;

import com.yuno.yunosbosses.YunosBosses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent REELSEIDEN_HIT = registerSoundEvent("reelseiden_hit");
    public static final SoundEvent STILL_ALIVE = registerSoundEvent("still_alive");
    public static final SoundEvent ANGRY_CHICKEN_AMBIENT = registerSoundEvent("angry_chicken_ambient");
    public static final SoundEvent ANGRY_CHICKEN_HURT = registerSoundEvent("angry_chicken_hurt");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of("yunosbosses", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        YunosBosses.LOGGER.info("Registering Mod Sounds for " + YunosBosses.MOD_ID);
    }
}
