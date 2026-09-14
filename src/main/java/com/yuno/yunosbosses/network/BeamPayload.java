package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public record BeamPayload(
        UUID ownerUuid,
        Vec3d start,
        int range,
        boolean useCustomStart,
        Vec3d direction,
        int chargeTicks,
        int durationTicks,
        float radius
) implements CustomPayload {

    public static final CustomPayload.Id<BeamPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "beam_payload"));

    public static final PacketCodec<RegistryByteBuf, BeamPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.ownerUuid());
                buf.writeDouble(value.start().x);
                buf.writeDouble(value.start().y);
                buf.writeDouble(value.start().z);
                buf.writeInt(value.range());
                buf.writeBoolean(value.useCustomStart());
                if (value.direction() != null) {
                    buf.writeBoolean(true);
                    buf.writeDouble(value.direction().x);
                    buf.writeDouble(value.direction().y);
                    buf.writeDouble(value.direction().z);
                } else {
                    buf.writeBoolean(false);
                }
                buf.writeInt(value.chargeTicks());
                buf.writeInt(value.durationTicks());
                buf.writeFloat(value.radius());
            },
            buf -> {
                UUID ownerUuid = buf.readUuid();
                Vec3d start = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
                int range = buf.readInt();
                boolean useCustomStart = buf.readBoolean();
                Vec3d direction = buf.readBoolean() ? new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
                int chargeTicks = buf.readInt();
                int durationTicks = buf.readInt();
                float radius = buf.readFloat();
                return new BeamPayload(ownerUuid, start, range, useCustomStart, direction, chargeTicks, durationTicks, radius);
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
