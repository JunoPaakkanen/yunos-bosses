package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record RemoveBarrierPayload(UUID ownerUuid) implements CustomPayload {
    public static final Id<RemoveBarrierPayload> ID = new Id<>(Identifier.of("yunosbosses", "remove_barrier_packet"));

    public static final PacketCodec<RegistryByteBuf, RemoveBarrierPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, RemoveBarrierPayload::ownerUuid,
            RemoveBarrierPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
