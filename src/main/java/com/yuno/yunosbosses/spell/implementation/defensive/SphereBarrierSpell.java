package com.yuno.yunosbosses.spell.implementation.defensive;

import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SphereBarrierSpell extends Spell {
    public SphereBarrierSpell(Identifier id) { super(id); }

    float manaCost = 100.0F;

    private static final Identifier BARRIER_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier.png");

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            float radius = 12.0F; // Currently hardcoded in multiple places, change later
            int lifetime = 400; // 20 seconds
            Vec3d pos = caster.getPos().add(0, 1, 0); // Center of the sphere

            BarrierManager.addBarrier(caster.getUuid(), pos, Vec3d.ZERO, lifetime, radius, false);

            // Broadcast to ALL nearby players so they can see the barrier
            for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld)world, pos, 64)) {
                ServerPlayNetworking.send(player, new BarrierPayload(caster.getUuid(), pos, Vec3d.ZERO, lifetime, BARRIER_TEXTURE, radius));
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.sphere_barrier");
    }

    @Override
    public float getManaCost(LivingEntity caster) {return manaCost;}
}
