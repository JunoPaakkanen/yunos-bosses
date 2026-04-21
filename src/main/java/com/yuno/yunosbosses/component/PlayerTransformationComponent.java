package com.yuno.yunosbosses.component;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

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
    }

    @Override
    public void kick() {
        if (player == null) return;

        var rotationVector = player.getRotationVector();
        var offset = rotationVector.multiply(1.5);
        var hitbox = player.getBoundingBox().offset(offset).expand(1.0);

        var targets = player.getWorld().getOtherEntities(player, hitbox);

        for (var target : targets) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.damage(player.getDamageSources().playerAttack(player), 10.0f);
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
