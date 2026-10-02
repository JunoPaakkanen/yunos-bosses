package com.yuno.yunosbosses.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenBindingVowScreenPayload(int tabIndex) implements CustomPayload {

    public static final CustomPayload.Id<OpenBindingVowScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("yunosbosses", "open_binding_vow_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenBindingVowScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenBindingVowScreenPayload::tabIndex,
            OpenBindingVowScreenPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
