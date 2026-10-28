package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationProcessor;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectionSorceryRenderer {

    // List of images to render
    private static final List<ImageData> IMAGES = new ArrayList<>();
    private static final Pattern TEXTURE_ID_PATTERN = Pattern.compile("Optional\\[([a-z0-9_.-]+:[a-z0-9_./-]+)]");
    private static final String[] PLAYER_BONE_NAMES = {"body", "head", "torso", "right_arm", "left_arm", "right_leg", "left_leg"};

    public static void addImage(LivingEntity caster, Vec3d pos, int ticks) {
        addImage(caster, pos, ticks, false, 0.0F, 0.0F, 0.0F);
    }

    public static void addImage(LivingEntity caster, Vec3d pos, int ticks, boolean hasCustomOrientation, float yaw, float pitch, float animOffsetTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getEntityRenderDispatcher() == null) return;

        float tickProgress = client.getRenderTickCounter().getTickProgress(false);

        @SuppressWarnings("unchecked")
        EntityRenderer<Entity, EntityRenderState> renderer =
                (EntityRenderer<Entity, EntityRenderState>) client.getEntityRenderDispatcher().getRenderer(caster);

        // Save entity's current position before snapping to the frame position to capture its snapshot
        Vec3d oldPos = caster.getPos();
        double oldLastX = caster.lastX;
        double oldLastY = caster.lastY;
        double oldLastZ = caster.lastZ;
        double oldLastRenderX = caster.lastRenderX;
        double oldLastRenderY = caster.lastRenderY;
        double oldLastRenderZ = caster.lastRenderZ;

        caster.setPosition(pos);
        caster.lastX = pos.x;
        caster.lastY = pos.y;
        caster.lastZ = pos.z;
        caster.lastRenderX = pos.x;
        caster.lastRenderY = pos.y;
        caster.lastRenderZ = pos.z;

        // IMPORTANT: Allocate a brand new EntityRenderState instance via createRenderState()!
        // Do NOT call getAndUpdateRenderState(), because EntityRenderer reuses a single `this.state`
        // field across all render frames, which would cause live entity renders to overwrite our snapshot!
        EntityRenderState capturedState = renderer.createRenderState();
        renderer.updateRenderState(caster, capturedState, tickProgress);

        // Restore entity position immediately
        caster.setPosition(oldPos);
        caster.lastX = oldLastX;
        caster.lastY = oldLastY;
        caster.lastZ = oldLastZ;
        caster.lastRenderX = oldLastRenderX;
        caster.lastRenderY = oldLastRenderY;
        caster.lastRenderZ = oldLastRenderZ;

        // Clear any visual hurt/frame-freeze flags on the captured state so the hologram stays clean
        if (capturedState instanceof LivingEntityRenderState livingState) {
            livingState.hurt = false;
            if (hasCustomOrientation) {
                livingState.bodyYaw = yaw;
                livingState.relativeHeadYaw = 0.0F;
                livingState.pitch = pitch;
            }
            if (animOffsetTicks != 0.0F && livingState.limbSwingAmplitude > 0.01F) {
                livingState.limbSwingAnimationProgress += animOffsetTicks * 0.75F;
            }
        }

        if (capturedState instanceof FrameFreezeStateAccess access) {
            access.yunosbosses$setFrameFrozen(false);

            // Capture PlayerAnimationLib bone state if this is an animated player
            if (caster instanceof AbstractClientPlayerEntity clientPlayer && clientPlayer instanceof IAnimatedPlayer animatedPlayer) {
                PlayerAnimManager manager = animatedPlayer.playerAnimLib$getAnimManager();
                if (manager != null && manager.isActive()) {
                    Map<String, PlayerAnimBone> playerBones = new HashMap<>();
                    for (String boneName : PLAYER_BONE_NAMES) {
                        PlayerAnimBone bone = new PlayerAnimBone(boneName);
                        manager.get3DTransform(bone);
                        playerBones.put(boneName, bone);
                    }
                    access.yunosbosses$setCapturedPlayerAnimBones(playerBones);
                }
                // Detach the live PlayerAnimManager from the captured state so PlayerAnimationLib's mixins
                // don't overwrite the frozen frame if the live player plays an animation later!
                if (capturedState instanceof IPlayerAnimationState animState) {
                    animState.playerAnimLib$setAnimManager(new PlayerAnimManager(clientPlayer));
                }
            }

            // Capture GeckoLib bone snapshots if this is a GeoAnimatable entity (e.g. Naoya)
            if (caster instanceof GeoAnimatable geoAnimatable && renderer instanceof GeoEntityRenderer<?, ?> geoRenderer && capturedState instanceof GeoRenderState geoState) {
                if (hasCustomOrientation) {
                    geoState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
                    geoState.addGeckolibData(DataTickets.ENTITY_YAW, 0.0F);
                    geoState.addGeckolibData(DataTickets.ENTITY_PITCH, pitch);
                    geoState.addGeckolibData(DataTickets.IS_MOVING, true);
                }
                Map<String, BoneSnapshot> geoBones = captureGeoBones(geoAnimatable, geoRenderer, geoState, animOffsetTicks);
                if (geoBones != null && !geoBones.isEmpty()) {
                    access.yunosbosses$setCapturedGeoBones(geoBones);
                }
            }
        }

        IMAGES.add(new ImageData(caster, renderer, capturedState, pos, ticks));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, BoneSnapshot> captureGeoBones(GeoAnimatable animatable, GeoEntityRenderer renderer, GeoRenderState geoState, float animOffsetTicks) {
        GeoModel model = renderer.getGeoModel();
        if (model == null) return null;

        BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(geoState));
        if (bakedModel == null) return null;

        AnimationProcessor processor = model.getAnimationProcessor();
        if (processor == null || processor.getRegisteredBones().isEmpty()) return null;

        AnimatableManager manager = geoState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) return null;

        // If no animation offset is requested and BoneSnapshots are already populated from the most recent render pass, copy them directly
        Map<String, BoneSnapshot> existingSnapshots = manager.getBoneSnapshotCollection();
        if (animOffsetTicks == 0.0F && existingSnapshots != null && !existingSnapshots.isEmpty()) {
            Map<String, BoneSnapshot> copied = new HashMap<>(existingSnapshots.size());
            for (GeoBone bone : (Iterable<GeoBone>) processor.getRegisteredBones()) {
                copied.put(bone.getName(), BoneSnapshot.copy(bone.saveSnapshot()));
            }
            return copied;
        }

        // Otherwise, evaluate the processor at (currentRenderTime + animOffsetTicks) and snapshot the resulting bones,
        // while preserving and restoring the live GeoBone transforms so the live entity is unaffected.
        Map<String, BoneSnapshot> savedLiveBones = new HashMap<>();
        for (GeoBone bone : (Iterable<GeoBone>) processor.getRegisteredBones()) {
            savedLiveBones.put(bone.getName(), BoneSnapshot.copy(bone.saveSnapshot()));
        }

        Double currentAnimTicks = (Double) geoState.getGeckolibData(DataTickets.ANIMATION_TICKS);
        double baseTicks = currentAnimTicks != null ? currentAnimTicks : (geoState instanceof EntityRenderState ers ? ers.age : 0.0);
        double targetAnimTicks = baseTicks + animOffsetTicks;

        AnimationState animState = new AnimationState(geoState);
        processor.prepareForRenderPass(animatable, manager, geoState, targetAnimTicks, model);
        model.handleAnimations(animState);

        Map<String, BoneSnapshot> captured = new HashMap<>();
        for (GeoBone bone : (Iterable<GeoBone>) processor.getRegisteredBones()) {
            captured.put(bone.getName(), BoneSnapshot.copy(bone.saveSnapshot()));
        }

        // Restore live bones
        for (Map.Entry<String, BoneSnapshot> entry : savedLiveBones.entrySet()) {
            GeoBone bone = processor.getBone(entry.getKey());
            if (bone != null) {
                BoneSnapshot s = entry.getValue();
                bone.setRotX(s.getRotX());
                bone.setRotY(s.getRotY());
                bone.setRotZ(s.getRotZ());
                bone.setPosX(s.getOffsetX());
                bone.setPosY(s.getOffsetY());
                bone.setPosZ(s.getOffsetZ());
                bone.setScaleX(s.getScaleX());
                bone.setScaleY(s.getScaleY());
                bone.setScaleZ(s.getScaleZ());
            }
        }

        return captured;
    }

    public static void register() {
        // Tick Event: Countdown and remove expired images
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            IMAGES.removeIf(image -> {
                image.ticksLeft--;
                return image.ticksLeft <= 0;
            });
        });

        // Render Event: Draw the images to the screen
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (IMAGES.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getEntityRenderDispatcher() == null) return;

            Vec3d cameraPos = context.camera().getPos();
            MatrixStack matrixStack = context.matrixStack();

            // Get the buffer provider from context or client
            VertexConsumerProvider entityConsumers = context.consumers();
            if (entityConsumers == null) {
                entityConsumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            for (ImageData image : IMAGES) {
                if (image.entity == null || image.renderer == null || image.capturedState == null) continue;

                matrixStack.push();

                // Move the paintbrush to the target Vec3d (offset by the player's camera)
                double x = image.position.x - cameraPos.x;
                double y = image.position.y - cameraPos.y;
                double z = image.position.z - cameraPos.z;
                matrixStack.translate(x, y, z);

                // Apply any model position offset from the renderer (e.g. sneaking offset)
                Vec3d posOffset = image.renderer.getPositionOffset(image.capturedState);
                matrixStack.translate(posOffset.getX(), posOffset.getY(), posOffset.getZ());

                // Dynamically wrap whichever RenderLayer Mojang requests for this entity call
                final VertexConsumerProvider originalProvider = entityConsumers;
                VertexConsumerProvider wrappedProvider = layer -> {
                    String layerStr = layer.toString();
                    String layerName = layerStr.toLowerCase();
                    // Do not apply the blue image consumer to shadows, glint, or text
                    if (layerName.contains("shadow") || layerName.contains("glint")) {
                        return new DummyVertexConsumer();
                    }

                    // Convert opaque/cutout armor & entity layers into translucent layers so armor is also semi-transparent!
                    RenderLayer targetLayer = toTranslucentLayer(layer, layerStr, layerName);

                    // Dynamically get the consumer for this render layer
                    VertexConsumer rawConsumer = originalProvider.getBuffer(targetLayer);

                    if (layerName.contains("text") && !layerName.contains("entity")) {
                        return rawConsumer;
                    }

                    return new BlueImageConsumer(rawConsumer);
                };

                // Render the captured state full-bright without re-querying the live entity!
                int maxLight = 15728880;
                image.renderer.render(image.capturedState, matrixStack, wrappedProvider, maxLight);

                matrixStack.pop();
            }
        });
    }

    private static RenderLayer toTranslucentLayer(RenderLayer original, String layerStr, String layerName) {
        if (layerName.contains("translucent")) {
            return original;
        }
        Matcher matcher = TEXTURE_ID_PATTERN.matcher(layerStr);
        if (matcher.find()) {
            Identifier textureId = Identifier.tryParse(matcher.group(1));
            if (textureId != null) {
                if (layerName.contains("armor")) {
                    return RenderLayer.createArmorTranslucent(textureId);
                } else if (layerName.contains("entity") || layerName.contains("item")) {
                    return RenderLayer.getEntityTranslucent(textureId);
                }
            }
        }
        return original;
    }

    // --- DATA CLASS ---
    private static class ImageData {
        final LivingEntity entity;
        final EntityRenderer<Entity, EntityRenderState> renderer;
        final EntityRenderState capturedState;
        final Vec3d position;
        int ticksLeft;

        ImageData(LivingEntity entity, EntityRenderer<Entity, EntityRenderState> renderer, EntityRenderState capturedState, Vec3d position, int ticksLeft) {
            this.entity = entity;
            this.renderer = renderer;
            this.capturedState = capturedState;
            this.position = position;
            this.ticksLeft = ticksLeft;
        }
    }

    // --- CUSTOM VERTEX CONSUMER ---
    // This intercepts every triangle drawn by the entity and forces it to be blue and transparent
    private static class BlueImageConsumer implements VertexConsumer {
        private static final int HOLOGRAM_COLOR = ColorHelper.getArgb(128, 0, 120, 255);
        private final VertexConsumer delegate;

        BlueImageConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Apply blue tint
            // Alpha: 128/255 makes it exactly 50% transparent
            delegate.color(0, 120, 255, 128);
            return this;
        }

        @Override
        public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            // ModelPart$Cuboid calls this 11-argument fast path directly on VertexConsumer!
            delegate.vertex(x, y, z, HOLOGRAM_COLOR, u, v, 655370, light, normalX, normalY, normalZ);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(10, 10);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }
    }

    private static class DummyVertexConsumer implements VertexConsumer {
        @Override public VertexConsumer vertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) { return this; }
        @Override public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {}
        @Override public VertexConsumer texture(float u, float v) { return this; }
        @Override public VertexConsumer overlay(int u, int v) { return this; }
        @Override public VertexConsumer light(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    }
}
