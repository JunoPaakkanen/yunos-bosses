package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record SpawnImagePayload(int entityId, Vec3d position, int ticks) implements CustomPayload {
    public static final CustomPayload.Id<SpawnImagePayload> ID = new CustomPayload.Id<>(Identifier.of("yunosbosses", "spawn_image"));

    public static final PacketCodec<RegistryByteBuf, SpawnImagePayload> CODEC = new PacketCodec<>() {
        @Override
        public SpawnImagePayload decode(RegistryByteBuf buf) {
            return new SpawnImagePayload(
                    buf.readInt(),
                    new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readInt()
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, SpawnImagePayload value) {
            buf.writeInt(value.entityId());
            buf.writeDouble(value.position().x);
            buf.writeDouble(value.position().y);
            buf.writeDouble(value.position().z);
            buf.writeInt(value.ticks());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}