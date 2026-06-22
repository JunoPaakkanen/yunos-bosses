package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record DomainCutscenePayload(UUID casterUuid, String domainName, int durationTicks) implements CustomPayload {

    public static final CustomPayload.Id<DomainCutscenePayload> ID = new CustomPayload.Id<>(Identifier.of("yunosbosses", "domain_cutscene"));

    public static final PacketCodec<RegistryByteBuf, DomainCutscenePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, DomainCutscenePayload::casterUuid,
            PacketCodecs.STRING, DomainCutscenePayload::domainName,
            PacketCodecs.INTEGER, DomainCutscenePayload::durationTicks,
            DomainCutscenePayload::new
    );

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
