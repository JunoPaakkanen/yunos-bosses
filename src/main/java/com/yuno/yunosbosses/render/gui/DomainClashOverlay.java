package com.yuno.yunosbosses.render.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

public class DomainClashOverlay implements HudRenderCallback {

    public static boolean isRenderingGuiOverlay = false;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!DomainClashClient.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        try {
            isRenderingGuiOverlay = true;

            int halfWidth = screenWidth / 2;
            int elapsed = DomainClashClient.getElapsedTicks();

            // 1. Cinematic letterboxing
            int letterboxHeight = screenHeight / 12;
            drawContext.fill(0, 0, screenWidth, letterboxHeight, 0xD0000000);
            drawContext.fill(0, screenHeight - letterboxHeight, screenWidth, screenHeight, 0xD0000000);

            // 2. Center Duel Banner
            int bannerHeight = 76;
            int bannerY = (screenHeight / 4) - (bannerHeight / 2);

            // Banner background split: Red on Left, Blue on Right
            drawContext.fill(0, bannerY, halfWidth, bannerY + bannerHeight, 0xEE2A050A);
            drawContext.fill(halfWidth, bannerY, screenWidth, bannerY + bannerHeight, 0xEE08162E);

            // Center border / divider line
            drawContext.fill(halfWidth - 1, bannerY, halfWidth + 1, bannerY + bannerHeight, 0xEEFFFFFF);
            // Top and Bottom Trim lines
            drawContext.fill(0, bannerY - 1, halfWidth, bannerY, 0xFFFF2233);
            drawContext.fill(halfWidth, bannerY - 1, screenWidth, bannerY, 0xFF3388FF);
            drawContext.fill(0, bannerY + bannerHeight, halfWidth, bannerY + bannerHeight + 1, 0xFFFF2233);
            drawContext.fill(halfWidth, bannerY + bannerHeight, screenWidth, bannerY + bannerHeight + 1, 0xFF3388FF);

            // Resolve Entities
            LivingEntity caster1 = resolveLivingEntity(client, DomainClashClient.getCaster1Uuid());
            LivingEntity caster2 = resolveLivingEntity(client, DomainClashClient.getCaster2Uuid());

            // Model positioning (flanking outer sides to prevent any overlap with text)
            int modelWidth = 72;
            int modelMargin = Math.min(220, halfWidth - 10);
            int m1X1 = Math.max(10, halfWidth - modelMargin);
            int m1X2 = m1X1 + modelWidth;

            int m2X2 = Math.min(screenWidth - 10, halfWidth + modelMargin);
            int m2X1 = m2X2 - modelWidth;

            int mY1 = bannerY - 10;
            int mY2 = bannerY + bannerHeight + 20;

            // Scissor for 3D model rendering inside banner
            drawContext.enableScissor(0, bannerY, screenWidth, bannerY + bannerHeight);

            // Render Caster 1 (Left - looking towards center)
            if (caster1 != null) {
                try {
                    InventoryScreen.drawEntity(
                            drawContext,
                            m1X1, mY1,
                            m1X2, mY2,
                            42,
                            0.0625f,
                            halfWidth, (mY1 + mY2) / 2.0f,
                            caster1
                    );
                } catch (Throwable ignored) {
                }
            }

            // Render Caster 2 (Right - looking towards center)
            if (caster2 != null) {
                try {
                    InventoryScreen.drawEntity(
                            drawContext,
                            m2X1, mY1,
                            m2X2, mY2,
                            42,
                            0.0625f,
                            halfWidth, (mY1 + mY2) / 2.0f,
                            caster2
                    );
                } catch (Throwable ignored) {
                }
            }

            drawContext.disableScissor();

            // Banner Text (Left: Caster 1 Info) - right aligned to the left of the VS center
            String c1Name = DomainClashClient.getCaster1Name();
            String d1Name = DomainClashClient.getDomain1Name();
            int c1NameWidth = client.textRenderer.getWidth(c1Name);
            int d1NameWidth = client.textRenderer.getWidth(d1Name);
            int leftTextAnchor = halfWidth - 35;
            int c1X = leftTextAnchor - c1NameWidth;
            int d1X = leftTextAnchor - d1NameWidth;
            drawContext.drawTextWithShadow(client.textRenderer, c1Name, c1X, bannerY + 18, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(client.textRenderer, d1Name, d1X, bannerY + 32, 0xFFFF3344);
            if (DomainClashClient.isCaster1OpenBarrier()) {
                String ob1 = "\u00a76[Open Barrier]";
                int ob1W = client.textRenderer.getWidth(ob1);
                drawContext.drawTextWithShadow(client.textRenderer, ob1, leftTextAnchor - ob1W, bannerY + 46, 0xFFFF9933);
            }

            // Banner Text (Right: Caster 2 Info) - left aligned to the right of the VS center
            String c2Name = DomainClashClient.getCaster2Name();
            String d2Name = DomainClashClient.getDomain2Name();
            int rightTextX = halfWidth + 35;
            drawContext.drawTextWithShadow(client.textRenderer, c2Name, rightTextX, bannerY + 18, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(client.textRenderer, d2Name, rightTextX, bannerY + 32, 0xFF33AAFF);
            if (DomainClashClient.isCaster2OpenBarrier()) {
                drawContext.drawTextWithShadow(client.textRenderer, "\u00a76[Open Barrier]", rightTextX, bannerY + 46, 0xFFFF9933);
            }

            // Center "VS" / "DOMAIN CLASH" Emblem
            float pulse = 1.0f + (float) Math.sin(elapsed * 0.35f) * 0.06f;
            String vsTitle = "DOMAIN CLASH";
            int vsWidth = client.textRenderer.getWidth(vsTitle);
            int vsX = (int) ((halfWidth / pulse) - (vsWidth / 2.0f));
            int vsY = (int) ((bannerY + (bannerHeight / 2) - 5) / pulse);

            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().scale(pulse, pulse);
            drawContext.drawTextWithShadow(client.textRenderer, "\u00a7e\u00a7l" + vsTitle, vsX, vsY, 0xFFFFDD33);
            drawContext.getMatrices().popMatrix();

            // 3. Tug-of-War Interactive Barrier Struggle Bar
            int barWidth = 260;
            int barHeight = 14;
            int barX = halfWidth - (barWidth / 2);
            int barY = (int) (screenHeight * 0.68f);

            // Bar container background & border
            drawContext.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF222228);
            drawContext.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF0D0D12);

            // Current needle position
            float balance = DomainClashClient.getCurrentRenderBalance();
            int needleOffset = (int) (barWidth * balance);
            needleOffset = MathHelper.clamp(needleOffset, 4, barWidth - 4);
            int needleX = barX + needleOffset;

            // Left Fill (Crimson)
            drawContext.fill(barX, barY, needleX, barY + barHeight, 0xFFCC1828);
            // Right Fill (Azure)
            drawContext.fill(needleX, barY, barX + barWidth, barY + barHeight, 0xFF1866CC);

            // Needle core marker (White hot)
            drawContext.fill(needleX - 2, barY - 2, needleX + 2, barY + barHeight + 2, 0xFFFFFFFF);
            drawContext.fill(needleX - 1, barY - 3, needleX + 1, barY + barHeight + 3, 0xFFFFEE55);

            // Action Prompt above bar
            if (DomainClashClient.isLocalPlayerParticipant()) {
                float promptPulse = 1.0f + (float) Math.sin(elapsed * 0.5f) * 0.04f;
                String prompt = "\u00a7e\u00a7l[MASH ATTACK] OVERPOWER THE BARRIER!\u00a7r";
                int promptWidth = client.textRenderer.getWidth(prompt);
                int pX = (int) ((halfWidth / promptPulse) - (promptWidth / 2.0f));
                int pY = (int) ((barY - 14) / promptPulse);

                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().scale(promptPulse, promptPulse);
                drawContext.drawTextWithShadow(client.textRenderer, prompt, pX, pY, 0xFFFFFFFF);
                drawContext.getMatrices().popMatrix();
            } else {
                String spectatorPrompt = "\u00a76\u00a7lBARRIER EQUILIBRIUM STRUGGLE\u00a7r";
                int sWidth = client.textRenderer.getWidth(spectatorPrompt);
                drawContext.drawTextWithShadow(client.textRenderer, spectatorPrompt, halfWidth - (sWidth / 2), barY - 13, 0xFFFFAA00);
            }

            // Subtext under bar: Caster Names and Balance
            drawContext.drawTextWithShadow(client.textRenderer, "\u00a7c" + c1Name, barX, barY + barHeight + 4, 0xFFFF4455);
            int c2W = client.textRenderer.getWidth(c2Name);
            drawContext.drawTextWithShadow(client.textRenderer, "\u00a79" + c2Name, barX + barWidth - c2W, barY + barHeight + 4, 0xFF4488FF);
        } finally {
            isRenderingGuiOverlay = false;
        }
    }

    private LivingEntity resolveLivingEntity(MinecraftClient client, UUID uuid) {
        if (uuid == null || client.world == null) return null;
        if (client.player != null && client.player.getUuid().equals(uuid)) {
            return client.player;
        }
        LivingEntity entity = client.world.getPlayerByUuid(uuid);
        if (entity != null) return entity;

        for (var e : client.world.getEntities()) {
            if (e.getUuid().equals(uuid) && e instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
