package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class FlameShockwaveParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float startScale;
    private final float endScale;

    protected FlameShockwaveParticle(ClientWorld world, double x, double y, double z,
                                     double velocityX, double velocityY, double velocityZ,
                                     SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        this.maxAge = 16;
        this.startScale = 2.0F;
        this.endScale = 8.5F + this.random.nextFloat() * 4.5F;
        this.scale = this.startScale;
        this.gravityStrength = 0.0F;

        this.velocityX = velocityX * 0.2;
        this.velocityY = velocityY * 0.1;
        this.velocityZ = velocityZ * 0.2;

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);

        // Expand rapidly outward
        float progress = (float) this.age / (float) this.maxAge;
        this.scale = this.startScale + (this.endScale - this.startScale) * progress;

        // Smooth alpha fade
        if (progress > 0.5F) {
            this.alpha = Math.max(0.0F, 1.0F - (progress - 0.5F) * 2.0F);
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

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new FlameShockwaveParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
