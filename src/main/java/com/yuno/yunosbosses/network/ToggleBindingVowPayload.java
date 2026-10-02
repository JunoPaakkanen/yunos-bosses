package com.yuno.yunosbosses.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleBindingVowPayload(String vowId) implements CustomPayload {

    public static final CustomPayload.Id<ToggleBindingVowPayload> ID = new CustomPayload.Id<>(Identifier.of("yunosbosses", "toggle_binding_vow"));
    public static final PacketCodec<RegistryByteBuf, ToggleBindingVowPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ToggleBindingVowPayload::vowId,
            ToggleBindingVowPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
