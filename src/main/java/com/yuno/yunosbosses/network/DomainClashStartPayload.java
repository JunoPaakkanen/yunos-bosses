package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record DomainClashStartPayload(
        UUID clashId,
        UUID caster1Uuid,
        String caster1Name,
        String domain1Name,
        boolean caster1OpenBarrier,
        UUID caster2Uuid,
        String caster2Name,
        String domain2Name,
        boolean caster2OpenBarrier,
        Vec3d clashPos,
        int durationTicks
) implements CustomPayload {

    public static final CustomPayload.Id<DomainClashStartPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "domain_clash_start"));

    public static final PacketCodec<RegistryByteBuf, DomainClashStartPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.clashId());
                buf.writeUuid(value.caster1Uuid());
                buf.writeString(value.caster1Name());
                buf.writeString(value.domain1Name());
                buf.writeBoolean(value.caster1OpenBarrier());

                buf.writeUuid(value.caster2Uuid());
                buf.writeString(value.caster2Name());
                buf.writeString(value.domain2Name());
                buf.writeBoolean(value.caster2OpenBarrier());

                buf.writeDouble(value.clashPos().x);
                buf.writeDouble(value.clashPos().y);
                buf.writeDouble(value.clashPos().z);
                buf.writeInt(value.durationTicks());
            },
            buf -> {
                UUID clashId = buf.readUuid();
                UUID c1Uuid = buf.readUuid();
                String c1Name = buf.readString();
                String d1Name = buf.readString();
                boolean c1Open = buf.readBoolean();

                UUID c2Uuid = buf.readUuid();
                String c2Name = buf.readString();
                String d2Name = buf.readString();
                boolean c2Open = buf.readBoolean();

                Vec3d clashPos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
                int duration = buf.readInt();
                return new DomainClashStartPayload(clashId, c1Uuid, c1Name, d1Name, c1Open, c2Uuid, c2Name, d2Name, c2Open, clashPos, duration);
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
