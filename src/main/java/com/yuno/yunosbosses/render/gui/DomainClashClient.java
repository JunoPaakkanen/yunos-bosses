package com.yuno.yunosbosses.render.gui;

import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.network.DomainClashEndPayload;
import com.yuno.yunosbosses.network.DomainClashProgressPayload;
import com.yuno.yunosbosses.network.DomainClashStartPayload;
import com.yuno.yunosbosses.render.BlackFlashClientHelper;
import com.yuno.yunosbosses.render.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Client-side coordinator for active Domain Clash rendering and user interaction.
 */
public class DomainClashClient {

    private static boolean active = false;
    private static UUID clashId = null;

    private static UUID caster1Uuid = null;
    private static String caster1Name = "";
    private static String domain1Name = "";
    private static boolean caster1OpenBarrier = false;

    private static UUID caster2Uuid = null;
    private static String caster2Name = "";
    private static String domain2Name = "";
    private static boolean caster2OpenBarrier = false;

    private static Vec3d clashPos = Vec3d.ZERO;
    private static int durationTicks = 100;
    private static int elapsedTicks = 0;

    private static float targetBalance = 0.5f;
    private static float currentRenderBalance = 0.5f;
    private static float score1 = 100.0f;
    private static float score2 = 100.0f;

    // End transition ticks for displaying victory/defeat before clearing
    private static int endDisplayTicks = 0;
    private static UUID winnerUuid = null;
    private static UUID loserUuid = null;

    public static void startClash(DomainClashStartPayload payload) {
        clashId = payload.clashId();
        caster1Uuid = payload.caster1Uuid();
        caster1Name = payload.caster1Name();
        domain1Name = payload.domain1Name();
        caster1OpenBarrier = payload.caster1OpenBarrier();

        caster2Uuid = payload.caster2Uuid();
        caster2Name = payload.caster2Name();
        domain2Name = payload.domain2Name();
        caster2OpenBarrier = payload.caster2OpenBarrier();

        clashPos = payload.clashPos();
        durationTicks = payload.durationTicks();
        elapsedTicks = 0;
        targetBalance = 0.5f;
        currentRenderBalance = 0.5f;
        score1 = 100.0f;
        score2 = 100.0f;
        endDisplayTicks = 0;
        winnerUuid = null;
        loserUuid = null;
        active = true;

        // Cancel any solo cutscene so the clash cutscene takes over
        DomainCutsceneManager.cancelCutscene();

        // Initial camera punch
        BlackFlashClientHelper.triggerFovKick(0.85f, 6);
    }

    public static void updateProgress(DomainClashProgressPayload payload) {
        if (!active || clashId == null || !clashId.equals(payload.clashId())) return;
        targetBalance = payload.balance();
        score1 = payload.score1();
        score2 = payload.score2();
    }

    public static void endClash(DomainClashEndPayload payload) {
        if (!active && endDisplayTicks <= 0) return;

        winnerUuid = payload.winnerUuid();
        loserUuid = payload.loserUuid();
        endDisplayTicks = 25; // Brief post-clash visual resolution
        active = false;

        // Ensure final balance reflects the winner side
        if (winnerUuid != null) {
            if (winnerUuid.equals(caster1Uuid) && targetBalance < 0.5f) {
                targetBalance = 0.51f;
            } else if (winnerUuid.equals(caster2Uuid) && targetBalance > 0.5f) {
                targetBalance = 0.49f;
            }
        }

        // Climax inverted shader flash & camera kick
        ShaderManager.triggerFlash(8);
        BlackFlashClientHelper.triggerFovKick(0.72f, 10);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (client.player.getUuid().equals(loserUuid)) {
                // Sharp camera tremor for loser
                client.player.changeLookDirection((Math.random() - 0.5) * 8.0, 3.5);
            }
        }
    }

    public static void registerLocalInput() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !active) return;

        UUID localUuid = client.player.getUuid();
        // Client-side prediction using the exact server formula for taps
        if (localUuid.equals(caster1Uuid)) {
            score1 += DomainClashManager.MASH_SCORE_PER_TAP;
        } else if (localUuid.equals(caster2Uuid)) {
            score2 += DomainClashManager.MASH_SCORE_PER_TAP;
        }

        float total = score1 + score2;
        if (total > 0.01f) {
            targetBalance = MathHelper.clamp(score1 / total, 0.05f, 0.95f);
        }

        // Crisp mechanical audio response
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.8f));
    }

    public static void tick() {
        if (endDisplayTicks > 0) {
            endDisplayTicks--;
            if (endDisplayTicks <= 0) {
                reset();
                return;
            }
        }

        if (!active && endDisplayTicks <= 0) return;

        elapsedTicks++;

        // Smooth lerping of balance gauge directly to targetBalance
        float goal = MathHelper.clamp(targetBalance, 0.05f, 0.95f);
        currentRenderBalance = MathHelper.lerp(0.25f, currentRenderBalance, goal);

        // Subtle camera rumble during intense struggle
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && clashPos != null) {
            double distSq = client.player.squaredDistanceTo(clashPos);
            if (distSq <= 36 * 36) {
                float intensity = (float) Math.max(0.1, 1.0 - (Math.sqrt(distSq) / 36.0));
                if (elapsedTicks % 3 == 0) {
                    client.player.changeLookDirection((Math.random() - 0.5) * 0.45 * intensity,
                            (Math.random() - 0.5) * 0.35 * intensity);
                }
            }
        }
    }

    public static void reset() {
        active = false;
        clashId = null;
        caster1Uuid = null;
        caster2Uuid = null;
        endDisplayTicks = 0;
        winnerUuid = null;
        loserUuid = null;
    }

    // --- GETTERS ---

    public static boolean isActive() {
        return active || endDisplayTicks > 0;
    }

    public static boolean isLocalPlayerParticipant() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        UUID localUuid = client.player.getUuid();
        return localUuid.equals(caster1Uuid) || localUuid.equals(caster2Uuid);
    }

    public static UUID getClashId() { return clashId; }
    public static UUID getCaster1Uuid() { return caster1Uuid; }
    public static String getCaster1Name() { return caster1Name; }
    public static String getDomain1Name() { return domain1Name; }
    public static boolean isCaster1OpenBarrier() { return caster1OpenBarrier; }

    public static UUID getCaster2Uuid() { return caster2Uuid; }
    public static String getCaster2Name() { return caster2Name; }
    public static String getDomain2Name() { return domain2Name; }
    public static boolean isCaster2OpenBarrier() { return caster2OpenBarrier; }

    public static Vec3d getClashPos() { return clashPos; }
    public static int getDurationTicks() { return durationTicks; }
    public static int getElapsedTicks() { return elapsedTicks; }
    public static float getCurrentRenderBalance() { return currentRenderBalance; }
    public static float getScore1() { return score1; }
    public static float getScore2() { return score2; }
    public static int getEndDisplayTicks() { return endDisplayTicks; }
    public static UUID getWinnerUuid() { return winnerUuid; }
    public static UUID getLoserUuid() { return loserUuid; }
}
