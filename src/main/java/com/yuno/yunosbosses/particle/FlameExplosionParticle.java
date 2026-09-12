package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class FlameExplosionParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected FlameExplosionParticle(ClientWorld world, double x, double y, double z,
                                    double velocityX, double velocityY, double velocityZ,
                                    SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        this.maxAge = 18 + this.random.nextInt(12);
        this.baseScale = 4.5F + this.random.nextFloat() * 3.5F;
        this.scale = this.baseScale;
        this.gravityStrength = -0.018F; // Slowly rises like a hot fireball

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = 0.94F;

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);

        // Billboard scale expands slightly as the smoke cloud billows outward
        float progress = (float) this.age / (float) this.maxAge;
        this.scale = this.baseScale * (1.0F + progress * 0.45F);
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
            return new FlameExplosionParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
