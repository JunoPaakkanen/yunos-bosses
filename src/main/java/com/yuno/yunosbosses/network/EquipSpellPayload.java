package com.yuno.yunosbosses.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EquipSpellPayload(int slot, String spellId) implements CustomPayload {

    public static final CustomPayload.Id<EquipSpellPayload> ID = new CustomPayload.Id<>(Identifier.of("yunosbosses", "equip_spell"));
    public static final PacketCodec<RegistryByteBuf, EquipSpellPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, EquipSpellPayload::slot,
            PacketCodecs.STRING, EquipSpellPayload::spellId,
            EquipSpellPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
