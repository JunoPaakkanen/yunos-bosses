package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record PlayerAnimationPayload(UUID playerUuid, Identifier animationId) implements CustomPayload {
    public static final Id<PlayerAnimationPayload> ID = new Id<>(Identifier.of("yunosbosses", "play_animation"));

    public static final PacketCodec<RegistryByteBuf, PlayerAnimationPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PlayerAnimationPayload::playerUuid,
            Identifier.PACKET_CODEC, PlayerAnimationPayload::animationId,
            PlayerAnimationPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
