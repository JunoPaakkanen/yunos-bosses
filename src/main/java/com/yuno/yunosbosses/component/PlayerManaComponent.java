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
    private static float MANA_REGEN = 0.5f; // per tick

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
    public void setMaxMana(float mana) {
        this.maxMana = Math.max(0, mana);
        // Cap current mana if max mana drops below it
        if (this.mana > this.maxMana) {
            this.mana = this.maxMana;
        }
        syncToClient();
    }

    @Override
    public void addMana(float amount) {
        setMana(mana + amount);
    }

    @Override
    public void setManaRegen(float regen) {
        MANA_REGEN = regen;
    }

    @Override
    public float getManaRegen() {
        return MANA_REGEN;
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
