package com.yuno.yunosbosses.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ProjectionSorceryRenderer {

    // List of images to render
    private static final List<ImageData> IMAGES = new ArrayList<>();

    public static void addImage(LivingEntity caster, Vec3d pos, int ticks) {
        IMAGES.add(new ImageData(caster, pos, ticks));
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

            // Get the buffer so we can tell the game to draw our translucent layer
            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

            for (ImageData image : IMAGES) {
                LivingEntity entity = image.entity;
                if (entity == null) continue;

                matrixStack.push();

                // Move the paintbrush to the target Vec3d (offset by the player's camera)
                double x = image.position.x - cameraPos.x;
                double y = image.position.y - cameraPos.y;
                double z = image.position.z - cameraPos.z;
                matrixStack.translate(x, y, z);

                // Fetch the entity's actual texture
                Identifier texture = client.getEntityRenderDispatcher().getRenderer(entity).getTexture(entity);

                // Force the layer to be Translucent
                RenderLayer translucentLayer = RenderLayer.getEntityTranslucent(texture);
                VertexConsumer originalConsumer = immediate.getBuffer(translucentLayer);

                // Wrap the consumer with the custom Blue Tint
                VertexConsumer wrappedConsumer = new BlueImageConsumer(originalConsumer);

                VertexConsumerProvider wrappedProvider = layer -> {
                    String layerName = layer.toString().toLowerCase();
                    // If the game asks to draw a shadow or nametag, give it the blank dummy canvas
                    if (layerName.contains("shadow") || (layerName.contains("text") && !layerName.contains("texture"))) {
                        return new DummyVertexConsumer();
                    }
                    return wrappedConsumer;
                };

                // --- THE SNAP TRICK ---
                // Save the entity's real state so we don't break their physics
                Vec3d oldPos = entity.getPos();
                float oldYaw = entity.getYaw();
                float oldBodyYaw = entity.getBodyYaw();
                float oldPitch = entity.getPitch();
                float oldHeadYaw = entity.getHeadYaw();

                // Snap the entity to the image position
                entity.setPosition(image.position);

                // Render the entity full-bright
                int maxLight = 15728880;

                client.getEntityRenderDispatcher().render(
                        entity,
                        0, 0, 0,
                        0.0F,
                        context.tickCounter().getTickDelta(false),
                        matrixStack,
                        wrappedProvider,
                        maxLight
                );

                // Instantly snap the entity back to reality
                entity.setPosition(oldPos);
                entity.setYaw(oldYaw);
                entity.setBodyYaw(oldBodyYaw);
                entity.setPitch(oldPitch);
                entity.setHeadYaw(oldHeadYaw);
                // ----------------------

                matrixStack.pop();
            }

            // Force the game to draw our transparent layer immediately
            immediate.draw();
        });
    }


    // --- DATA CLASS ---
    private static class ImageData {
        LivingEntity entity;
        Vec3d position;
        int ticksLeft;

        ImageData(LivingEntity entity, Vec3d position, int ticksLeft) {
            this.entity = entity;
            this.position = position;
            this.ticksLeft = ticksLeft;
        }
    }

    // --- CUSTOM VERTEX CONSUMER ---
    // This intercepts every triangle drawn by the entity and forces it to be blue and transparent
    private static class BlueImageConsumer implements VertexConsumer {
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
        @Override public VertexConsumer texture(float u, float v) { return this; }
        @Override public VertexConsumer overlay(int u, int v) { return this; }
        @Override public VertexConsumer light(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    }
}
