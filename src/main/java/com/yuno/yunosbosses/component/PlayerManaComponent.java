package com.yuno.yunosbosses.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class PlayerManaComponent implements ManaComponent, AutoSyncedComponent, ServerTickingComponent {
    private final PlayerEntity player;
    private float mana;
    private float maxMana = 100f;
    private static final float MANA_REGEN = 0.5f; // per tick

    public PlayerManaComponent(PlayerEntity player) {
        this.player = player;
        this.mana = maxMana;
    }

    @Override
    public void serverTick() {
        if (mana < maxMana) {
            mana = Math.min(mana + MANA_REGEN, maxMana);
            syncToClient();
        }
    }
    
    private void syncToClient() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModEntityComponents.MANA.sync(serverPlayer);
        }
    }
    
    @Override
    public float getMana() {
        return mana;
    }
    
    @Override
    public float getMaxMana() {
        return maxMana;
    }
    
    @Override
    public boolean useMana(float amount) {
        if (mana >= amount) {
            mana -= amount;
            syncToClient();
            return true;
        }
        return false;
    }
    
    @Override
    public void setMana(float value) {
        this.mana = Math.max(0, Math.min(value, maxMana));
        syncToClient();
    }
    
    @Override
    public void addMana(float amount) {
        setMana(mana + amount);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup wrapperLookup) {
        mana = tag.getFloat("mana");
        maxMana = tag.getFloat("maxMana");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup wrapperLookup) {
        tag.putFloat("mana", mana);
        tag.putFloat("maxMana", maxMana);
    }
}
