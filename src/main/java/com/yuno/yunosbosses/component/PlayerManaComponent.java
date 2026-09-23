package com.yuno.yunosbosses.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class PlayerManaComponent implements ManaComponent, AutoSyncedComponent, ServerTickingComponent {
    private final PlayerEntity player;
    private float mana;
    private float maxMana = 100f;
    private float manaRegen = 0.5f; // per tick
    private int syncCooldown = 0;

    public PlayerManaComponent(PlayerEntity player) {
        this.player = player;
        this.mana = maxMana;
    }

    @Override
    public void serverTick() {
        if (mana < maxMana) {
            mana = Math.min(mana + this.manaRegen, maxMana);
            if (mana >= maxMana) {
                syncToClient();
                syncCooldown = 0;
            } else if (++syncCooldown >= 10) {
                syncToClient();
                syncCooldown = 0;
            }
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
            syncCooldown = 0;
            return true;
        }
        return false;
    }
    
    @Override
    public void setMana(float value) {
        this.mana = Math.max(0, Math.min(value, maxMana));
        syncToClient();
        syncCooldown = 0;
    }

    @Override
    public void setMaxMana(float mana) {
        this.maxMana = Math.max(0, mana);
        // Cap current mana if max mana drops below it
        if (this.mana > this.maxMana) {
            this.mana = this.maxMana;
        }
        syncToClient();
        syncCooldown = 0;
    }

    @Override
    public void addMana(float amount) {
        setMana(mana + amount);
    }

    @Override
    public void setManaRegen(float regen) {
        this.manaRegen = regen;
    }

    @Override
    public float getManaRegen() {
        return this.manaRegen;
    }

    @Override
    public void readData(ReadView readView) {
        this.mana = readView.getFloat("mana", this.mana);
        this.maxMana = readView.getFloat("maxMana", this.maxMana);
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.putFloat("mana", this.mana);
        writeView.putFloat("maxMana", this.maxMana);
    }
}
