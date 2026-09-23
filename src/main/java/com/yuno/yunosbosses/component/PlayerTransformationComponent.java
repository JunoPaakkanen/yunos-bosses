package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.util.BlackFlash;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class PlayerTransformationComponent implements TransformationComponent {
    private final PlayerEntity player;
    private boolean transformed = false;

    private int blackFlashChain = 0;
    private boolean inTheZone = false;
    private int zoneTimer = 0;

    public PlayerTransformationComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean isTransformed() { return this.transformed; }

    @Override
    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
        // Syncs the change to all players nearby so they see the model change
        ModEntityComponents.TRANSFORMATION_DATA.sync(player);

        // Drop items upon transformation
        if (transformed && !this.player.getWorld().isClient()) {

            // Forcefully drop Chestplate
            ItemStack chest = this.player.getEquippedStack(EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                this.player.dropItem(chest, true, false);
                this.player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
            // Forcefully drop Helmet
            ItemStack head = this.player.getEquippedStack(EquipmentSlot.HEAD);
            if (!head.isEmpty()) {
                this.player.dropItem(head, true, false);
                this.player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            // Drop absolutely everything in the standard inventory/hotbar
            this.player.getInventory().dropAll();
        }
    }

    @Override
    public boolean isInTheZone() {
        return this.inTheZone;
    }

    @Override
    public void setInTheZone(boolean inTheZone, int ticks) {
        this.inTheZone = inTheZone;
        this.zoneTimer = ticks;
    }

    @Override
    public int getBlackFlashChain() {
        return this.blackFlashChain;
    }

    @Override
    public void setBlackFlashChain(int chain) {
        this.blackFlashChain = chain;
    }

    @Override
    public void resetBlackFlashChain() {
        this.blackFlashChain = 0;
    }

    @Override
    public void serverTick() {
        // Count down "The Zone" timer
        if (this.inTheZone) {
            this.zoneTimer--;
            if (this.zoneTimer <= 0) {
                this.inTheZone = false;
                this.blackFlashChain = 0;
            }
        }
    }

    @Override
    public void kick() {
        if (player == null) return;

        var rotationVector = player.getRotationVector();
        var offset = rotationVector.multiply(1.5);
        var hitbox = player.getBoundingBox().offset(offset).expand(1.0);
        var enlargedHitbox = hitbox.expand(1.25);

        // Calculate the center of the hitbox for particle effects
        double effectX = player.getX() + offset.x;
        double effectY = player.getY() + 1.0;
        double effectZ = player.getZ() + offset.z;

        // Play sound
        player.getWorld().playSound(
                null,
                effectX, effectY, effectZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, // The sound
                SoundCategory.PLAYERS,
                1.0f, // Volume
                0.8f  // Pitch (Less than 1.0 makes it sound deeper and heavier!)
        );

        // Spawn particles
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            // Big sweep curve particle (All variants)
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    effectX, effectY, effectZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
            // --- NORMAL VARIANT ---
            if (player.getInventory().getSelectedSlot() == 0) {
                // --- NORMAL VARIANT PARTICLES ---
                // Dust effect for extra impact for the regular variant
                serverWorld.spawnParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        effectX, player.getY(), effectZ,
                        5,
                        0.3, 0.1, 0.3,
                        0.02
                );
                // --- NORMAL VARIANT EFFECT ---
                // Apply Black Flash to the primary target
                var targets = player.getWorld().getOtherEntities(player, hitbox);
                for (var target : targets) {
                    if (target instanceof LivingEntity livingTarget) {
                        BlackFlash.blackFlashChance(player, livingTarget, 0.05f);
                        break;
                    }
                }
            }
            // --- BLUE VARIANT ---
            if (player.getInventory().getSelectedSlot() == 1) {
                // --- BLUE VARIANT PARTICLES ---
                // Spawn small gust particles
                serverWorld.spawnParticles(
                        ParticleTypes.SMALL_GUST,
                        effectX, effectY, effectZ,
                        8,
                        0.5, 0.3, 0.5,
                        0.04
                );
                // Spawn custom lapse blue particle
                serverWorld.spawnParticles(
                        ModParticles.LAPSE_BLUE_PARTICLE,
                        effectX, effectY, effectZ,
                        1,
                         0.0, 0.0, 0.0,
                         0.0
                );
                // --- BLUE VARIANT EFFECT ---
                // Pull targets towards the player
                var targetsToPull = player.getWorld().getOtherEntities(player, enlargedHitbox);
                for (var target : targetsToPull) {
                    if (target instanceof LivingEntity livingTarget) {
                        // Calculate the vector pointing from the target to the player
                        Vec3d pullDirection = player.getPos().subtract(target.getPos()).normalize();
                        double pullStrength = 0.5;

                        double pullX = pullDirection.x * pullStrength;
                        double pullZ = pullDirection.z * pullStrength;
                        double liftY = 0.6;

                        livingTarget.setVelocity(pullX, liftY, pullZ);
                        livingTarget.velocityModified = true;
                    }
                }
            }
            // --- RED VARIANT ---
            if (player.getInventory().getSelectedSlot() == 2) {
                // --- RED VARIANT PARTICLES ---
                // Spawn red particles for the red variant
                int pureRed = 0xFF0000; // Red color
                DustParticleEffect redDust = new DustParticleEffect(pureRed, 1.2f);
                serverWorld.spawnParticles(
                        redDust,
                        effectX, effectY, effectZ,
                        5,
                        0.3, 0.1, 0.3,
                        0.04
                );
                // Spawn custom reversal red particle
                serverWorld.spawnParticles(
                        ModParticles.REVERSAL_RED_PARTICLE,
                        effectX, effectY, effectZ,
                        1,
                         0.0, 0.0, 0.0,
                         0.0
                );
                // --- RED VARIANT EFFECT ---
                // Push targets away from the player
                var targetsToPush = player.getWorld().getOtherEntities(player, hitbox);
                for (var target : targetsToPush) {
                    if (target instanceof LivingEntity livingTarget) {
                        // Calculate the vector pointing from the player to the target
                        Vec3d pushDirection = target.getPos().subtract(player.getPos()).normalize();
                        double pushStrength = 4.0;

                        livingTarget.setVelocity(pushDirection.x * pushStrength, 0.0, pushDirection.z * pushStrength);
                        livingTarget.velocityModified = true;
                    }
                }
            }

            // Deal damage
            var targets = player.getWorld().getOtherEntities(player, hitbox);
            for (var target : targets) {
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.damage((ServerWorld) player.getWorld(),player.getDamageSources().playerAttack(player), 10.0f);
                }
            }
        }
    }

    @Override
    public void readData(ReadView tag) {
        this.transformed = tag.getBoolean("IsTransformed", false);
    }

    @Override
    public void writeData(WriteView tag) {
        tag.putBoolean("IsTransformed", this.transformed);
    }
}
