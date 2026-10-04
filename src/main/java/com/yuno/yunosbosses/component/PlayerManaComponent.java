package com.yuno.yunosbosses.component;

import com.mojang.serialization.Codec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerManaComponent implements ManaComponent, AutoSyncedComponent, ServerTickingComponent {
    public static final float DEFAULT_ABSOLUTE_CAP = 200.0F;
    public static final float CAP_BONUS_PER_BOSS = 50.0F;

    private final PlayerEntity player;
    private float mana;
    private float maxMana = 100f;
    private float manaRegen = 0.25f; // per tick (5.0 mana / sec; 20s for a full 100 mana refill)
    private int syncCooldown = 0;

    // Set of unique boss identifiers defeated with significant contribution
    private final Set<String> defeatedBosses = new HashSet<>();

    public PlayerManaComponent(PlayerEntity player) {
        this.player = player;
        this.mana = maxMana;
    }

    @Override
    public void serverTick() {
        if (this.manaRegen > 0.0f && mana < maxMana) {
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
        float allowedCap = getAbsoluteMaxManaCap();
        this.maxMana = Math.max(0, Math.min(mana, allowedCap));
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
        syncToClient();
        syncCooldown = 0;
    }

    @Override
    public float getManaRegen() {
        return this.manaRegen;
    }

    @Override
    public float getAbsoluteMaxManaCap() {
        return DEFAULT_ABSOLUTE_CAP + (defeatedBosses.size() * CAP_BONUS_PER_BOSS);
    }

    @Override
    public boolean hasDefeatedBoss(String bossId) {
        return defeatedBosses.contains(bossId);
    }

    @Override
    public boolean recordBossDefeat(String bossId) {
        if (defeatedBosses.add(bossId)) {
            syncToClient();
            syncCooldown = 0;
            return true;
        }
        return false;
    }

    @Override
    public Set<String> getDefeatedBosses() {
        return new HashSet<>(defeatedBosses);
    }

    @Override
    public void readData(ReadView readView) {
        this.mana = readView.getFloat("mana", this.mana);
        this.maxMana = readView.getFloat("maxMana", this.maxMana);
        this.manaRegen = readView.getFloat("manaRegen", this.manaRegen);

        this.defeatedBosses.clear();
        readView.getOptionalTypedListView("DefeatedBosses", Codec.STRING).ifPresent(list -> {
            for (String bossId : list) {
                this.defeatedBosses.add(bossId);
            }
        });
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.putFloat("mana", this.mana);
        writeView.putFloat("maxMana", this.maxMana);
        writeView.putFloat("manaRegen", this.manaRegen);

        var appender = writeView.getListAppender("DefeatedBosses", Codec.STRING);
        for (String bossId : this.defeatedBosses) {
            appender.add(bossId);
        }
    }
}
