package com.yuno.yunosbosses.mixin;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yuno.yunosbosses.render.gui.MultiSlotSpecialRenderer;
import com.yuno.yunosbosses.render.gui.TextureSlot;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SpecialGuiElementRenderer.class)
public abstract class SpecialGuiElementRendererMixin<T extends SpecialGuiElementRenderState> implements MultiSlotSpecialRenderer, AutoCloseable {

    @Shadow @Final protected VertexConsumerProvider.Immediate vertexConsumers;
    @Shadow @Final private ProjectionMatrix2 projectionMatrix;
    @Shadow protected abstract float getYOffset(int height, int windowScaleFactor);
    @Shadow protected abstract void render(T element, MatrixStack matrixStack);
    @Shadow protected abstract String getName();

    @Unique
    private final List<TextureSlot> yunos$slots = new ArrayList<>();
    @Unique
    private int yunos$currentSlotIndex = 0;

    @Override
    public void yunos$resetSlots() {
        this.yunos$currentSlotIndex = 0;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/render/state/special/SpecialGuiElementRenderState;Lnet/minecraft/client/gui/render/state/GuiRenderState;I)V", at = @At("HEAD"), cancellable = true)
    private void yunos$renderMultiSlot(T element, GuiRenderState guiRenderState, int windowScaleFactor, CallbackInfo ci) {
        int width = (element.x2() - element.x1()) * windowScaleFactor;
        int height = (element.y2() - element.y1()) * windowScaleFactor;
        if (width <= 0 || height <= 0) {
            ci.cancel();
            return;
        }

        int slotIdx = this.yunos$currentSlotIndex++;
        while (this.yunos$slots.size() <= slotIdx) {
            this.yunos$slots.add(new TextureSlot());
        }
        TextureSlot slot = this.yunos$slots.get(slotIdx);
        slot.prepare(width, height, this.getName() + " slot " + slotIdx);

        RenderSystem.setProjectionMatrix(this.projectionMatrix.set((float) width, (float) height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.outputColorTextureOverride = slot.getTextureView();
        RenderSystem.outputDepthTextureOverride = slot.getDepthTextureView();

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate((float) width / 2.0F, this.getYOffset(height, windowScaleFactor), 0.0F);
        float scale = (float) windowScaleFactor * element.scale();
        matrixStack.scale(scale, scale, -scale);

        try {
            this.render(element, matrixStack);
            this.vertexConsumers.draw();
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
        }

        guiRenderState.addSimpleElementToCurrentLayer(
                new TexturedQuadGuiElementRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.withoutGlTexture(slot.getTextureView()),
                        element.pose(),
                        element.x1(), element.y1(),
                        element.x2(), element.y2(),
                        0.0F, 1.0F, 1.0F, 0.0F,
                        -1,
                        element.scissorArea(),
                        null
                )
        );

        ci.cancel();
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void yunos$closeSlots(CallbackInfo ci) {
        for (TextureSlot slot : this.yunos$slots) {
            slot.close();
        }
        this.yunos$slots.clear();
    }
}
