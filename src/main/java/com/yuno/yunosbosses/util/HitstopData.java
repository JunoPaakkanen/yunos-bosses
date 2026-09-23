package com.yuno.yunosbosses.util;

public interface HitstopData {
    int yunos$getHitstopTicks();
    void yunos$setHitstopTicks(int ticks);

    default boolean yunos$isHitstopped() {
        return yunos$getHitstopTicks() > 0;
    }
}