package com.yuno.yunosbosses.spell.implementation.defensive;

import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DefensiveMagic extends Spell {

    public DefensiveMagic(Identifier id, SpellRarity rarity) { super(id, rarity); }

    Identifier hexTexture = Identifier.of("yunosbosses", "textures/effect/magical_hexagon.png");

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            float shieldRadius = 1.6F;
            int lifetime = 40;

            // Get the caster look vector and determine barrier position
            Vec3d look = caster.getRotationVector();
            Vec3d barrierPos = caster.getEyePos().add(look.multiply(1.5));

            // Sound cue for barrier activation
            world.playSound(null, barrierPos.x, barrierPos.y, barrierPos.z,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.8F, 1.4F);
            world.playSound(null, barrierPos.x, barrierPos.y, barrierPos.z,
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8F, 1.8F);

            // Add to BarrierManager
            BarrierManager.addBarrier(caster.getUuid(), barrierPos, look, lifetime, hexTexture, shieldRadius, false);

            // Send Packet to Client for rendering if the caster is a player
            if (caster instanceof ServerPlayerEntity player) {
                ServerPlayNetworking.send(
                        player,
                        new BarrierPayload(caster.getUuid(), barrierPos, look, lifetime, hexTexture, shieldRadius)
                );
            }

            // Send the packet to all players tracking the caster entity (So everyone can see a boss's barrier render)
            for (ServerPlayerEntity player : PlayerLookup.tracking(caster)) {
                if (player != caster) {
                    ServerPlayNetworking.send(
                            player,
                            new BarrierPayload(caster.getUuid(), barrierPos, look, lifetime, hexTexture, shieldRadius)
                    );
                }
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.defensive_magic");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 30.0F;
    }
}
