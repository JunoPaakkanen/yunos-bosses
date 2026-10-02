package com.yuno.yunosbosses.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayerBindingVowComponent implements BindingVowComponent {

    private final PlayerEntity player;
    private final Set<Identifier> activeVows = new HashSet<>();

    public PlayerBindingVowComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean hasVow(Identifier vowId) {
        return activeVows.contains(vowId);
    }

    @Override
    public void addVow(Identifier vowId) {
        if (activeVows.add(vowId)) {
            sync();
        }
    }

    @Override
    public void removeVow(Identifier vowId) {
        if (activeVows.remove(vowId)) {
            sync();
        }
    }

    @Override
    public void clearVows() {
        if (!activeVows.isEmpty()) {
            activeVows.clear();
            sync();
        }
    }

    @Override
    public Set<Identifier> getActiveVows() {
        return Collections.unmodifiableSet(activeVows);
    }

    public void sync() {
        if (!player.getWorld().isClient) {
            ModEntityComponents.BINDING_VOWS.sync(player);
        }
    }

    @Override
    public void writeData(WriteView writeView) {
        var appender = writeView.getListAppender("ActiveVows", Identifier.CODEC);
        for (Identifier id : this.activeVows) {
            appender.add(id);
        }
    }

    @Override
    public void readData(ReadView readView) {
        this.activeVows.clear();
        readView.getOptionalTypedListView("ActiveVows", Identifier.CODEC).ifPresent(list -> {
            for (Identifier id : list) {
                this.activeVows.add(id);
            }
        });
    }
}
