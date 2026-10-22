package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record FlameShockwavePayload(Vec3d center, float maxRadius, boolean isThermobaric) implements CustomPayload {
    public static final Id<FlameShockwavePayload> ID = new Id<>(Identifier.of("yunosbosses", "flame_shockwave_packet"));

    public static final PacketCodec<RegistryByteBuf, FlameShockwavePayload> CODEC = PacketCodec.tuple(
            BarrierPayload.VEC3D_CODEC, FlameShockwavePayload::center,
            PacketCodecs.FLOAT, FlameShockwavePayload::maxRadius,
            PacketCodecs.BOOLEAN, FlameShockwavePayload::isThermobaric,
            FlameShockwavePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
