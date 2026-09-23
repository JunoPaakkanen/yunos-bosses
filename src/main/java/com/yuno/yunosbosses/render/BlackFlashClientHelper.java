package com.yuno.yunosbosses.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class BlackFlashClientHelper {

    private static int fovKickTicks = 0;
    private static int maxFovKickTicks = 0;
    private static float targetFovMultiplier = 1.0F;

    public static void handleBlackFlashPacket(Vec3d pos, UUID attackerUuid, UUID targetUuid, boolean isFinisher, int chainCount) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 1. Inverted anime negative flash shader
        int flashDuration = isFinisher ? 8 : 4;
        ShaderManager.triggerFlash(flashDuration);

        // 2. Spawn 3D branching lightning arcs in the world
        Vec3d lookDir = client.player.getRotationVector();
        BlackFlashRenderer.spawnStrike(pos, lookDir, isFinisher, chainCount);

        // 3. Screen FOV punch for involved or nearby players
        double distSq = client.player.squaredDistanceTo(pos);
        if (distSq <= 48 * 48) {
            boolean isDirectActor = client.player.getUuid().equals(attackerUuid) || client.player.getUuid().equals(targetUuid);
            float distanceFactor = (float) Math.max(0.2, 1.0 - (Math.sqrt(distSq) / 48.0));
            float intensity = isDirectActor ? 1.0F : distanceFactor;

            float fovDrop = isFinisher ? 0.22F : 0.14F;
            int ticks = isFinisher ? 8 : 5;
            triggerFovKick(1.0F - (fovDrop * intensity), ticks);

            // Punchy camera recoil if the local player is the attacker
            if (client.player.getUuid().equals(attackerUuid)) {
                client.player.changeLookDirection(0.0, isFinisher ? -1.6 : -0.9);
            }
        }
    }

    public static void triggerFovKick(float targetMultiplier, int ticks) {
        targetFovMultiplier = targetMultiplier;
        fovKickTicks = ticks;
        maxFovKickTicks = ticks;
    }

    public static void tick() {
        if (fovKickTicks > 0) {
            fovKickTicks--;
        }
    }

    public static float getFovMultiplier() {
        if (fovKickTicks <= 0 || maxFovKickTicks <= 0) return 1.0F;
        float progress = (float) fovKickTicks / (float) maxFovKickTicks;
        // Ease out quadratic recovery
        float ease = progress * progress;
        return 1.0F - ((1.0F - targetFovMultiplier) * ease);
    }
}
