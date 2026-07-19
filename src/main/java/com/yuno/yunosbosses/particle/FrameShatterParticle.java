package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class FrameShatterParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    public FrameShatterParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteProvider = spriteProvider;

        // Add a tiny random offset to the start position
        this.x += (this.random.nextDouble() - 0.5) * 0.4;
        this.y += (this.random.nextDouble() - 0.5) * 0.4;
        this.z += (this.random.nextDouble() - 0.5) * 0.4;

        this.maxAge = 10;
        this.setSpriteForAge(spriteProvider);

        this.red = 1.0F;
        this.green = 1.0F;
        this.blue = 1.0F;

        this.velocityX = (this.random.nextDouble() - 0.5) * 0.02;
        this.velocityY = (this.random.nextDouble() - 0.5) * 0.02;
        this.velocityZ = (this.random.nextDouble() - 0.5) * 0.02;

        this.gravityStrength = 0.0f;

        this.scale = 1.5f;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_LIT;
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

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new FrameShatterParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
        }
    }
}
