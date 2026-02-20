package com.yuno.yunosbosses.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpellCyclePayload() implements CustomPayload {
    public static final Id<SpellCyclePayload> ID = new Id<>(Identifier.of("yunosbosses", "cycle_spell"));
    public static final PacketCodec<RegistryByteBuf, SpellCyclePayload> CODEC = PacketCodec.unit(new SpellCyclePayload());

    @Override
    public Id<? extends CustomPayload> getId() {return ID;}
}
