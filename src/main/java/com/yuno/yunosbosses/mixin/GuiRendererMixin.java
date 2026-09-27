package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.render.gui.MultiSlotSpecialRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Shadow @Final private Map<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>> specialElementRenderers;

    @Inject(method = "prepareSpecialElements", at = @At("HEAD"))
    private void yunos$resetSpecialRendererSlots(CallbackInfo ci) {
        for (SpecialGuiElementRenderer<?> renderer : this.specialElementRenderers.values()) {
            if (renderer instanceof MultiSlotSpecialRenderer multiSlot) {
                multiSlot.yunos$resetSlots();
            }
        }
    }
}
