package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.animation.ModAnimations;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.PlayerAnimationPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class DomainExpansion extends Spell {

    public DomainExpansion(Identifier id) {
        super(id, true);
    }

    public abstract Identifier getBarrierTexture();
    public abstract int getLifetimeTicks();
    public abstract float getRadius();
    public abstract void onDomainEffect(Entity affectedEntity);

    // Default mana cost
    public float manaCost = 100.0F;

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            double radius = 12.0; // Currently hardcoded in multiple places, change later
            Vec3d pos = caster.getPos().add(0, 1, 0); // Center of the sphere

            Identifier texture = getBarrierTexture();
            int lifetime = getLifetimeTicks();

            // --- PLAYER ANIMATION ---
            PlayerAnimationPayload animPayload = new PlayerAnimationPayload(
                    caster.getUuid(),
                    ModAnimations.DOMAIN_EXPANSION_SHRINE_ANIM
            );

            BarrierManager.ACTIVE_BARRIERS.add(
                    new ActiveBarrier(caster.getUuid(), pos, Vec3d.ZERO, lifetime, texture, this::onDomainEffect)
            );

            // Broadcast to ALL nearby players so they can see the barrier and animation
            for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld)world, pos, 64)) {
                ServerPlayNetworking.send(player, new BarrierPayload(caster.getUuid(), pos, Vec3d.ZERO, lifetime, texture));
                ServerPlayNetworking.send(player, animPayload);
            }
        }
    }

}
