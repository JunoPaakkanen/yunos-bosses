package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationProcessor;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.Map;

@Mixin(GeoModel.class)
public abstract class GeoModelMixin<T extends GeoAnimatable> {

    @Shadow
    private double lastGameTickTime;

    @Shadow
    private double animationTicks;

    @Shadow
    public abstract void addAdditionalStateData(T animatable, GeoRenderState renderState);

    @Shadow
    public abstract AnimationProcessor<T> getAnimationProcessor();

    @Inject(method = "prepareForRenderPass", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$freezeRenderPass(T animatable, GeoRenderState renderState, CallbackInfo ci) {
        if (renderState instanceof FrameFreezeStateAccess access && access.yunosbosses$isFrameFrozen()) {
            AnimatableManager<T> animatableManager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
            if (animatableManager != null) {
                this.lastGameTickTime = animatableManager.getLastUpdateTime();
            }
            renderState.addGeckolibData(DataTickets.ANIMATION_TICKS, this.animationTicks);
            addAdditionalStateData(animatable, renderState);
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimations", at = @At("HEAD"), cancellable = true)
    private void yunosbosses$freezeAnimations(AnimationState<T> animationState, CallbackInfo ci) {
        if (animationState.renderState() instanceof FrameFreezeStateAccess access && access.yunosbosses$isFrameFrozen()) {
            AnimatableManager<T> animatableManager = animationState.manager();
            Map<String, BoneSnapshot> boneSnapshots = animatableManager != null ? animatableManager.getBoneSnapshotCollection() : null;
            if (boneSnapshots != null && !boneSnapshots.isEmpty()) {
                AnimationProcessor<T> processor = getAnimationProcessor();
                if (processor != null) {
                    for (Map.Entry<String, BoneSnapshot> entry : boneSnapshots.entrySet()) {
                        GeoBone bone = processor.getBone(entry.getKey());
                        if (bone != null) {
                            BoneSnapshot snapshot = entry.getValue();
                            bone.setRotX(snapshot.getRotX());
                            bone.setRotY(snapshot.getRotY());
                            bone.setRotZ(snapshot.getRotZ());
                            bone.setPosX(snapshot.getOffsetX());
                            bone.setPosY(snapshot.getOffsetY());
                            bone.setPosZ(snapshot.getOffsetZ());
                            bone.setScaleX(snapshot.getScaleX());
                            bone.setScaleY(snapshot.getScaleY());
                            bone.setScaleZ(snapshot.getScaleZ());
                        }
                    }
                }
                ci.cancel();
            }
        }
    }
}
