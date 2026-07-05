package com.yuno.yunosbosses.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    // This forcefully exposes the private loadPostProcessor method
    @Invoker("loadPostProcessor")
    void invokeLoadPostProcessor(Identifier identifier);

}