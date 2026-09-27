package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DomainClashInputPayload(UUID clashId) implements CustomPayload {

    public static final CustomPayload.Id<DomainClashInputPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yunosbosses", "domain_clash_input"));

    public static final PacketCodec<RegistryByteBuf, DomainClashInputPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeUuid(value.clashId()),
            buf -> new DomainClashInputPayload(buf.readUuid())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }
}
