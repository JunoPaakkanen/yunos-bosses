package com.yuno.yunosbosses.render.gui;

import java.util.UUID;

public class DomainCutsceneManager {
    public static int ticksRemaining = 0;
    public static int maxTicks = 0;
    public static UUID casterUuid = null;
    public static String domainName = "";

    public static void startCutscene(UUID uuid, String name, int ticks) {
        casterUuid = uuid;
        domainName = name;
        ticksRemaining = ticks;
        maxTicks = ticks;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }
}