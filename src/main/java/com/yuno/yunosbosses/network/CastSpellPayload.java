package com.yuno.yunosbosses.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CastSpellPayload(Identifier spellId) implements CustomPayload {
    public static final Id<CastSpellPayload> ID = new Id<>(Identifier.of("yunosbosses", "cast_spell"));
    public static final PacketCodec<RegistryByteBuf, CastSpellPayload> CODEC = new PacketCodec<>() {
        @Override
        public CastSpellPayload decode(RegistryByteBuf buf) {
            return new CastSpellPayload(buf.readIdentifier());
        }

        @Override
        public void encode(RegistryByteBuf buf, CastSpellPayload value) {
            buf.writeIdentifier(value.spellId);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void sendCastSpellPacket(Identifier spellId) {
        ClientPlayNetworking.send(new CastSpellPayload(spellId));
    }
}
