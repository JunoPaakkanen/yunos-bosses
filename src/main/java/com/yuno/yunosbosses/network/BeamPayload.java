package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public record BeamPayload(UUID ownerUuid, Vec3d start, int range) implements CustomPayload {

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
            Uuids.PACKET_CODEC, BeamPayload::ownerUuid,
            VEC3D_CODEC, BeamPayload::start,
            PacketCodecs.VAR_INT, BeamPayload::range,
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