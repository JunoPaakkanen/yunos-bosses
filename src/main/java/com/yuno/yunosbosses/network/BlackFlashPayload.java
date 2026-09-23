package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record BlackFlashPayload(
        double x,
        double y,
        double z,
        UUID attackerUuid,
        UUID targetUuid,
        boolean isFinisher,
        int chainCount
) implements CustomPayload {

    public static final CustomPayload.Id<BlackFlashPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "black_flash"));

    public static final PacketCodec<RegistryByteBuf, BlackFlashPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, BlackFlashPayload::x,
            PacketCodecs.DOUBLE, BlackFlashPayload::y,
            PacketCodecs.DOUBLE, BlackFlashPayload::z,
            Uuids.PACKET_CODEC, BlackFlashPayload::attackerUuid,
            Uuids.PACKET_CODEC, BlackFlashPayload::targetUuid,
            PacketCodecs.BOOLEAN, BlackFlashPayload::isFinisher,
            PacketCodecs.INTEGER, BlackFlashPayload::chainCount,
            BlackFlashPayload::new
    );

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
