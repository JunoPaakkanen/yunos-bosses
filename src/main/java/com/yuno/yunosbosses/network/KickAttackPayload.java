package com.yuno.yunosbosses.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record KickAttackPayload() implements CustomPayload {

    public static final Id<KickAttackPayload> ID = new Id<>(Identifier.of("yunosbosses", "kick_attack"));

    public static final PacketCodec<RegistryByteBuf, KickAttackPayload> CODEC = PacketCodec.unit(new KickAttackPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
