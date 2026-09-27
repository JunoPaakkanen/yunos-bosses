package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record DomainClashEndPayload(
        UUID clashId,
        UUID winnerUuid,
        UUID loserUuid,
        Vec3d clashPos
) implements CustomPayload {

    public static final CustomPayload.Id<DomainClashEndPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "domain_clash_end"));

    public static final PacketCodec<RegistryByteBuf, DomainClashEndPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.clashId());
                buf.writeUuid(value.winnerUuid());
                buf.writeUuid(value.loserUuid());
                buf.writeDouble(value.clashPos().x);
                buf.writeDouble(value.clashPos().y);
                buf.writeDouble(value.clashPos().z);
            },
            buf -> {
                UUID clashId = buf.readUuid();
                UUID winnerUuid = buf.readUuid();
                UUID loserUuid = buf.readUuid();
                Vec3d clashPos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
                return new DomainClashEndPayload(clashId, winnerUuid, loserUuid, clashPos);
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
