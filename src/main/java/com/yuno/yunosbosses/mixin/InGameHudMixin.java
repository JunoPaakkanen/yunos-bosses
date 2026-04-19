package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.render.ManaHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderManaBarUnderneath(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.player != null && !this.client.options.hudHidden) {
            int width = this.client.getWindow().getScaledWidth();
            int height = this.client.getWindow().getScaledHeight();

            ManaHudRenderer.renderManaBar(context, width, height, this.client.player);
        }
    }
}