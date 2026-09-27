package com.yuno.yunosbosses.render.gui;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

public class TextureSlot implements AutoCloseable {
    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuTexture depthTexture;
    private GpuTextureView depthTextureView;
    private int width = 0;
    private int height = 0;

    public void prepare(int width, int height, String name) {
        GpuDevice device = RenderSystem.getDevice();
        if (this.texture == null || this.width != width || this.height != height) {
            close();
            this.width = width;
            this.height = height;

            this.texture = device.createTexture(name + " color", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, width, height, 1, 1);
            this.texture.setTextureFilter(FilterMode.NEAREST, false);
            this.textureView = device.createTextureView(this.texture);

            this.depthTexture = device.createTexture(name + " depth", GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, width, height, 1, 1);
            this.depthTexture.setTextureFilter(FilterMode.NEAREST, false);
            this.depthTextureView = device.createTextureView(this.depthTexture);
        }

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
    }

    public GpuTextureView getTextureView() {
        return textureView;
    }

    public GpuTextureView getDepthTextureView() {
        return depthTextureView;
    }

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }
        if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
        }
        this.width = 0;
        this.height = 0;
    }
}
