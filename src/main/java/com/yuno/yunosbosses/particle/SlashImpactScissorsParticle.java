package com.yuno.yunosbosses.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class SlashImpactScissorsParticle extends SpriteBillboardParticle {

    public SlashImpactScissorsParticle(ClientWorld clientWorld, double x, double y, double z,
                                       SpriteProvider spriteProvider, double xSpeed, double ySpeed, double zSpeed) {
        super(clientWorld, x, y, z, 0.0, 0.0, 0.0);

        // Add a tiny random offset to the start position
        this.x += (this.random.nextDouble() - 0.5) * 0.4;
        this.y += (this.random.nextDouble() - 0.5) * 0.4;
        this.z += (this.random.nextDouble() - 0.5) * 0.4;

        this.maxAge = 20;
        this.setSpriteForAge(spriteProvider);

        this.red = 1.0F;
        this.green = 1.0F;
        this.blue = 1.0F;

        this.velocityX = (this.random.nextDouble() - 0.5) * 0.02;
        this.velocityY = (this.random.nextDouble() - 0.5) * 0.02;
        this.velocityZ = (this.random.nextDouble() - 0.5) * 0.02;

        this.gravityStrength = 0.0f;

        this.scale = 0.5f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // This makes the particle fade out as it reaches its maxAge
        this.setAlpha(1.0f - ((float)this.age / (float)this.maxAge));
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
            return new SlashImpactScissorsParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
        }
    }
}
