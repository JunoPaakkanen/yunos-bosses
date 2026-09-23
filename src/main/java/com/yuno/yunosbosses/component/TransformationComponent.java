package com.yuno.yunosbosses.component;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public interface TransformationComponent extends Component, AutoSyncedComponent, ServerTickingComponent {
    boolean isTransformed();

    void setTransformed(boolean transformed);

    void kick();

    boolean isInTheZone();

    void setInTheZone(boolean inTheZone, int ticks);

    int getBlackFlashChain();

    void setBlackFlashChain(int chain);

    void resetBlackFlashChain();

    @Override
    default void serverTick() {}
}
