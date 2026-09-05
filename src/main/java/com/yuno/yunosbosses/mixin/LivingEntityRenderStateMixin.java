package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements FrameFreezeStateAccess {
    @Unique
    private boolean yunosbosses$frameFrozen;

    @Override
    public boolean yunosbosses$isFrameFrozen() {
        return this.yunosbosses$frameFrozen;
    }

    @Override
    public void yunosbosses$setFrameFrozen(boolean frozen) {
        this.yunosbosses$frameFrozen = frozen;
    }
}
