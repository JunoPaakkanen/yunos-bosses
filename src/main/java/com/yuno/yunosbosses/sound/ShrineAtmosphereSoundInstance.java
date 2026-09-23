package com.yuno.yunosbosses.sound;

import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

/**
 * Manages an ongoing atmospheric looping sound (e.g. Sub-bass hum, howling wind)
 * that smoothly fades in when inside Malevolent Shrine and smoothly fades out
 * when exiting or when the domain expires.
 */
public class ShrineAtmosphereSoundInstance extends MovingSoundInstance {
    private final ClientPlayerEntity player;
    private final float maxVolume;
    private final float basePitch;
    private int fadeOutTicks = 0;

    public ShrineAtmosphereSoundInstance(SoundEvent sound, ClientPlayerEntity player, float maxVolume, float pitch) {
        super(sound, SoundCategory.AMBIENT, SoundInstance.createRandom());
        this.player = player;
        this.maxVolume = maxVolume;
        this.basePitch = pitch;
        this.pitch = pitch;
        this.volume = 0.01f;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved() || this.player.getWorld() == null) {
            this.setDone();
            return;
        }

        boolean insideShrine = false;
        Vec3d playerPos = this.player.getPos();

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS_CLIENT) {
            if (!barrier.getDirection().equals(Vec3d.ZERO)) continue;
            boolean isShrine = barrier.getTexture() != null && barrier.getTexture().getPath().contains("shrine");
            if (isShrine) {
                double dist = playerPos.distanceTo(barrier.getPosition());
                if (dist <= barrier.getRadius() + 3.0) {
                    insideShrine = true;
                    break;
                }
            }
        }

        if (insideShrine) {
            fadeOutTicks = 0;
            // Smoothly ramp volume up to maxVolume over 30 ticks (1.5 seconds)
            if (this.volume < this.maxVolume) {
                this.volume = Math.min(this.maxVolume, this.volume + (this.maxVolume / 30.0f));
            }
            // Subtle slow undulating pitch creating an ominous breathing/throbbing drone
            this.pitch = this.basePitch + (float) Math.sin(this.player.age * 0.04) * 0.025f;
        } else {
            // Smoothly ramp volume down to 0 over 25 ticks (1.25 seconds)
            this.volume = Math.max(0.0f, this.volume - (this.maxVolume / 25.0f));
            fadeOutTicks++;
            if (this.volume <= 0.002f || fadeOutTicks > 35) {
                this.setDone();
            }
        }
    }
}
