package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.particle.ModParticles;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ShriekParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class PlayerTransformationComponent implements TransformationComponent {
    private final PlayerEntity player;
    private boolean transformed = false;

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
    public void kick() {
        if (player == null) return;

        var rotationVector = player.getRotationVector();
        var offset = rotationVector.multiply(1.5);
        var hitbox = player.getBoundingBox().offset(offset).expand(1.0);
        var enlargedHitbox = hitbox.expand(1.0);

        // Calculate the center of the hitbox for particle effects
        double effectX = player.getX() + offset.x;
        double effectY = player.getY() + 1.0;
        double effectZ = player.getZ() + offset.z;

        // Play sound
        player.getWorld().playSound(
                null,
                effectX, effectY, effectZ,
                net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, // The sound
                net.minecraft.sound.SoundCategory.PLAYERS,
                1.0f, // Volume
                0.8f  // Pitch (Less than 1.0 makes it sound deeper and heavier!)
        );

        // Spawn particles
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            // Big sweep curve particle
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    effectX, effectY, effectZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
            // --- NORMAL VARIANT PARTICLES ---
            if (player.getInventory().selectedSlot == 0) {
                // Dust effect for extra impact for the regular variant
                serverWorld.spawnParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        effectX, player.getY(), effectZ,
                        5,
                        0.3, 0.1, 0.3,
                        0.02
                );
            }
            // --- BLUE VARIANT PARTICLES ---
            if (player.getInventory().selectedSlot == 1) {
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
            }
            // --- RED VARIANT PARTICLES ---
            if (player.getInventory().selectedSlot == 2) {
                // Spawn red particles for the red variant
                Vector3f pureRed = new Vector3f(1.0f, 0.0f, 0.0f); // Red color
                DustParticleEffect redDust = new DustParticleEffect(pureRed, 1.2f);
                serverWorld.spawnParticles(
                        redDust,
                        effectX, effectY, effectZ,
                        5,
                        0.3, 0.1, 0.3,
                        0.04
                );
            }

            // Deal damage
            var targets = player.getWorld().getOtherEntities(player, hitbox);
            for (var target : targets) {
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.damage(player.getDamageSources().playerAttack(player), 10.0f);
                }
            }
            // --- BLUE VARIANT EFFECT ---
            if (player.getInventory().selectedSlot == 1) {
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
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.transformed = tag.getBoolean("IsTransformed");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("IsTransformed", this.transformed);
    }
}
