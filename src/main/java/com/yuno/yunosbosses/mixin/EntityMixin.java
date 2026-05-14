package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements WallSlamData {

    @Shadow
    public boolean horizontalCollision;

    @Unique
    private int yunos$wallSlamTimer = 0;

    @Override
    public int yunos$getWallSlamTimer() { return this.yunos$wallSlamTimer; }

    @Override
    public void yunos$setWallSlamTimer(int ticks) { this.yunos$wallSlamTimer = ticks; }

    @Inject(method = "tick", at = @At("TAIL"))
    private void yunos$decrementTimer(CallbackInfo ci) {
        if (this.yunos$wallSlamTimer > 0) {
            this.yunos$wallSlamTimer--;
        }
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void detectWallSlam(MovementType type, Vec3d movement, CallbackInfo ci) {
        // Only apply wall slam to living entities
        if ((Object)this instanceof LivingEntity living) {

            // --- WALL SLAM ---
            if (this.horizontalCollision && this.yunos$getWallSlamTimer() > 0) {
                double velocitySq = movement.horizontalLengthSquared();

                // Check if moving fast enough (adjust 0.02 as needed)
                if (velocitySq > 0.02) {
                    float speed = (float) Math.sqrt(velocitySq);

                    // Damage calculation
                    float damage = speed * 11.0f;

                    living.damage(living.getDamageSources().flyIntoWall(), damage);

                    // Explosion sound
                    living.getWorld().playSound(null, living.getX(), living.getY(), living.getZ(),
                            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 1.2f);

                    // Explosion particles
                    if (living.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                                living.getX(), living.getY() + 1.0, living.getZ(),
                                3,
                                0.3, 0.3, 0.3,
                                0.0);
                    }

                    this.yunos$wallSlamTimer = 0;
                }
            }
        }
    }
}