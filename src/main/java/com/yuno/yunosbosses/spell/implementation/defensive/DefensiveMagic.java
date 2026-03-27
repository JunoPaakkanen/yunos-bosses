package com.yuno.yunosbosses.spell.implementation.defensive;

import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DefensiveMagic extends Spell {

    public DefensiveMagic(Identifier id) { super(id); }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            // Spell implementation
            float blockingRadius = 0.6F;
            float health = 50.0F;
            float cooldown = 15.0F;
            int lifetime = 40;

            // Get the caster look vector and determine barrier position
            Vec3d look = caster.getRotationVector();
            Vec3d barrierPos = caster.getEyePos().add(look.multiply(1.5));

            // Add to BarrierManager
            BarrierManager.addBarrier(caster.getUuid(), barrierPos, look, lifetime, false);

            // Send Packet to Client for rendering
            ServerPlayNetworking.send(
                    (ServerPlayerEntity) caster,
                    new BarrierPayload(caster.getUuid(), barrierPos, look, lifetime)
            );
        }
    }
}
