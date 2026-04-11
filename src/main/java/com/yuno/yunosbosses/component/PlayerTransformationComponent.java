package com.yuno.yunosbosses.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

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
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.transformed = tag.getBoolean("IsTransformed");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("IsTransformed", this.transformed);
    }
}
