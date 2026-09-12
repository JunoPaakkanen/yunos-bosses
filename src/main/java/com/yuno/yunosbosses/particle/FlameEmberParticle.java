package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class FlameEmberParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final double wobbleSpeed;
    private final double wobbleAmplitude;

    protected FlameEmberParticle(ClientWorld world, double x, double y, double z,
                                double velocityX, double velocityY, double velocityZ,
                                SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        this.maxAge = 12 + this.random.nextInt(8);
        this.scale = 0.08F + this.random.nextFloat() * 0.06F;
        this.gravityStrength = -0.005F; // Gentle floating embers

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = 0.95F;

        this.wobbleSpeed = 0.15 + this.random.nextDouble() * 0.2;
        this.wobbleAmplitude = 0.008 + this.random.nextDouble() * 0.012;

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);

        // Turbulent rising motion
        this.velocityX += Math.sin(this.age * this.wobbleSpeed) * this.wobbleAmplitude;
        this.velocityZ += Math.cos(this.age * this.wobbleSpeed) * this.wobbleAmplitude;

        // Subtle shrink as ember burns out
        this.scale *= 0.985F;

        // Smooth fade out over the second half of life
        if (this.age > this.maxAge * 0.4) {
            float fadeProgress = (float) (this.age - (this.maxAge * 0.4)) / (float) (this.maxAge * 0.6);
            this.alpha = Math.max(0.0F, 1.0F - fadeProgress);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880; // Full emissive brightness
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                      double x, double y, double z,
                                      double velocityX, double velocityY, double velocityZ) {
            return new FlameEmberParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
