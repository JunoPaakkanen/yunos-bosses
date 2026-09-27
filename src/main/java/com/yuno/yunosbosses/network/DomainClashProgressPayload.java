package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DomainClashProgressPayload(
        UUID clashId,
        float balance,
        float score1,
        float score2
) implements CustomPayload {

    public static final CustomPayload.Id<DomainClashProgressPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "domain_clash_progress"));

    public static final PacketCodec<RegistryByteBuf, DomainClashProgressPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.clashId());
                buf.writeFloat(value.balance());
                buf.writeFloat(value.score1());
                buf.writeFloat(value.score2());
            },
            buf -> {
                UUID clashId = buf.readUuid();
                float balance = buf.readFloat();
                float score1 = buf.readFloat();
                float score2 = buf.readFloat();
                return new DomainClashProgressPayload(clashId, balance, score1, score2);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
