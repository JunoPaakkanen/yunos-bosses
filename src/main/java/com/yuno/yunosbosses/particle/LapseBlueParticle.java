package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class LapseBlueParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected LapseBlueParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        this.maxAge = 14;
        this.baseScale = 1.3F + (float) (world.random.nextFloat() * 0.4F);
        this.scale = this.baseScale;
        this.gravityStrength = 0.0F;

        this.velocityX = velocityX * 0.4;
        this.velocityY = velocityY * 0.4;
        this.velocityZ = velocityZ * 0.4;
        this.velocityMultiplier = 0.88f;

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);

        // Smooth pulse and fade
        float progress = (float) this.age / (float) this.maxAge;
        if (progress > 0.65F) {
            this.alpha = Math.max(0.0F, 1.0F - (progress - 0.65F) / 0.35F);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880;
    }

    // --- THE FACTORY ---
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new LapseBlueParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
