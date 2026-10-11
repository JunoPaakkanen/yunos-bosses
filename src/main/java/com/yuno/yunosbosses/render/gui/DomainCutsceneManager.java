package com.yuno.yunosbosses.render.gui;

import java.util.UUID;

public class DomainCutsceneManager {
    public static int ticksRemaining = 0;
    public static int maxTicks = 0;
    public static UUID casterUuid = null;
    public static String domainName = "";
    public static boolean isOpenBarrier = false;

    public static void startCutscene(UUID uuid, String name, int ticks, boolean openBarrier) {
        casterUuid = uuid;
        domainName = name;
        ticksRemaining = ticks;
        maxTicks = ticks;
        isOpenBarrier = openBarrier;
    }

    public static void cancelCutscene() {
        ticksRemaining = 0;
        maxTicks = 0;
        casterUuid = null;
    }

    public static boolean isInvertedFlashActive() {
        if (ticksRemaining > 0 && domainName != null) {
            String lower = domainName.toLowerCase();
            if (lower.contains("malevolent") || lower.contains("moon palace") || lower.contains("time cell")) {
                // Flash on the climax of the hand sign / voice chant (last 12 ticks of cutscene)
                return ticksRemaining <= 12 && ticksRemaining >= 2;
            }
        }
        return false;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }
}
