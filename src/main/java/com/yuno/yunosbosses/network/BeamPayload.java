package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record BeamPayload(Vec3d start, Vec3d end) implements CustomPayload {

    public static final CustomPayload.Id<BeamPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "beam_payload"));

    // Custom Codec to manually read/write Vec3d doubles to the buffer
    public static final PacketCodec<RegistryByteBuf, Vec3d> VEC3D_CODEC = new PacketCodec<>() {
        @Override
        public Vec3d decode(RegistryByteBuf buf) {
            return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(RegistryByteBuf buf, Vec3d value) {
            buf.writeDouble(value.x);
            buf.writeDouble(value.y);
            buf.writeDouble(value.z);
        }
    };

    // Use custom VEC3D_CODEC for the start and end points
    public static final PacketCodec<RegistryByteBuf, BeamPayload> CODEC = PacketCodec.tuple(
            VEC3D_CODEC, BeamPayload::start,
            VEC3D_CODEC, BeamPayload::end,
            BeamPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    // Register method
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}