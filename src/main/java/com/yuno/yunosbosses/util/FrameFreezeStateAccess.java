package com.yuno.yunosbosses.util;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.state.BoneSnapshot;

import java.util.Map;

public interface FrameFreezeStateAccess {
    boolean yunosbosses$isFrameFrozen();
    void yunosbosses$setFrameFrozen(boolean frozen);

    @Nullable
    Map<String, BoneSnapshot> yunosbosses$getCapturedGeoBones();
    void yunosbosses$setCapturedGeoBones(@Nullable Map<String, BoneSnapshot> bones);

    @Nullable
    Map<String, PlayerAnimBone> yunosbosses$getCapturedPlayerAnimBones();
    void yunosbosses$setCapturedPlayerAnimBones(@Nullable Map<String, PlayerAnimBone> bones);
}
