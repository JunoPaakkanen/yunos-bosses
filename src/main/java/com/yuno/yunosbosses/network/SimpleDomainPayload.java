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

public record SimpleDomainPayload(UUID casterUuid, Vec3d center, float radius, int durationTicks, boolean active) implements CustomPayload {
    public static final Id<SimpleDomainPayload> ID = new Id<>(Identifier.of("yunosbosses", "simple_domain_packet"));

    public static final PacketCodec<RegistryByteBuf, SimpleDomainPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SimpleDomainPayload::casterUuid,
            BarrierPayload.VEC3D_CODEC, SimpleDomainPayload::center,
            PacketCodecs.FLOAT, SimpleDomainPayload::radius,
            PacketCodecs.VAR_INT, SimpleDomainPayload::durationTicks,
            PacketCodecs.BOOLEAN, SimpleDomainPayload::active,
            SimpleDomainPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
