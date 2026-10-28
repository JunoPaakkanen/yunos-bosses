package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animation.state.BoneSnapshot;

import java.util.Map;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements FrameFreezeStateAccess {
    @Unique
    private boolean yunosbosses$frameFrozen;

    @Unique
    @Nullable
    private Map<String, BoneSnapshot> yunosbosses$capturedGeoBones;

    @Unique
    @Nullable
    private Map<String, PlayerAnimBone> yunosbosses$capturedPlayerAnimBones;

    @Override
    public boolean yunosbosses$isFrameFrozen() {
        return this.yunosbosses$frameFrozen;
    }

    @Override
    public void yunosbosses$setFrameFrozen(boolean frozen) {
        this.yunosbosses$frameFrozen = frozen;
    }

    @Override
    @Nullable
    public Map<String, BoneSnapshot> yunosbosses$getCapturedGeoBones() {
        return this.yunosbosses$capturedGeoBones;
    }

    @Override
    public void yunosbosses$setCapturedGeoBones(@Nullable Map<String, BoneSnapshot> bones) {
        this.yunosbosses$capturedGeoBones = bones;
    }

    @Override
    @Nullable
    public Map<String, PlayerAnimBone> yunosbosses$getCapturedPlayerAnimBones() {
        return this.yunosbosses$capturedPlayerAnimBones;
    }

    @Override
    public void yunosbosses$setCapturedPlayerAnimBones(@Nullable Map<String, PlayerAnimBone> bones) {
        this.yunosbosses$capturedPlayerAnimBones = bones;
    }
}
