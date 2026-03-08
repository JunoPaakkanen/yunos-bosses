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

public record BarrierPayload(UUID ownerUuid, Vec3d position, Vec3d direction, int maxTicks) implements CustomPayload {
    public static final Id<BarrierPayload> ID = new Id<>(Identifier.of("yunosbosses", "barrier_packet"));

    public static final PacketCodec<RegistryByteBuf, Vec3d> VEC3D_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, Vec3d::getX,
            PacketCodecs.DOUBLE, Vec3d::getY,
            PacketCodecs.DOUBLE, Vec3d::getZ,
            Vec3d::new
    );

    public static final PacketCodec<RegistryByteBuf, BarrierPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, BarrierPayload::ownerUuid,
            VEC3D_CODEC, BarrierPayload::position,
            VEC3D_CODEC, BarrierPayload::direction,
            PacketCodecs.VAR_INT, BarrierPayload::maxTicks,
            BarrierPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    // Register method
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
