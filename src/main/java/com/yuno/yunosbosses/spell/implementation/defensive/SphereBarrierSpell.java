package com.yuno.yunosbosses.spell.implementation.defensive;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.BarrierManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SphereBarrierSpell extends Spell {
    public SphereBarrierSpell(Identifier id, SpellRarity rarity) {
        super(id, rarity);
    }

    private static final float BASE_MANA_COST = 35.0F;
    public static final Identifier SPHERE_BARRIER_TEXTURE = Identifier.of("yunosbosses", "textures/effect/barrier.png");

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            // Fail if the caster has an active Domain
            if (BarrierManager.hasActiveDomain(caster.getUuid())) {
                ModEntityComponents.MANA.get(caster).addMana(getManaCost(caster));
                return;
            }

            // Charge Level 1: Quick protective bubble (4.5 blocks, 10s)
            // Charge Level 2: Medium combat dome (9.0 blocks, 16s)
            // Charge Level 3: Sanctuary dome (16.0 blocks, 25s)
            float radius = switch (chargeLevel) {
                case 3 -> 16.0F;
                case 2 -> 9.0F;
                default -> 4.5F;
            };

            int lifetime = switch (chargeLevel) {
                case 3 -> 500; // 25s
                case 2 -> 320; // 16s
                default -> 200; // 10s
            };

            Vec3d pos = caster.getPos().add(0, 1.2, 0); // Center around caster torso

            // Add server barrier tracking with the specific sphere barrier texture
            BarrierManager.addBarrier(caster.getUuid(), pos, Vec3d.ZERO, lifetime, SPHERE_BARRIER_TEXTURE, radius, false);

            // Auditory feedback: impactful barrier activation pulse
            float pitch = switch (chargeLevel) {
                case 3 -> 0.75F;
                case 2 -> 1.0F;
                default -> 1.35F;
            };
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0F, pitch);
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.5F, pitch * 1.2F);
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2.5F, 0.9F);

            // Burst particle ring at activation point
            int particleCount = (int) (radius * 12);
            for (int i = 0; i < particleCount; i++) {
                double angle = (2.0 * Math.PI * i) / particleCount;
                double px = pos.x + Math.cos(angle) * (radius * 0.35);
                double pz = pos.z + Math.sin(angle) * (radius * 0.35);
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        px, pos.y, pz, 1, Math.cos(angle) * 0.25, 0.05, Math.sin(angle) * 0.25, 0.15);
            }

            // Broadcast to all nearby players
            for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, pos, 64 + (int) radius)) {
                ServerPlayNetworking.send(player, new BarrierPayload(caster.getUuid(), pos, Vec3d.ZERO, lifetime, SPHERE_BARRIER_TEXTURE, radius));
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.sphere_barrier");
    }

    @Override
    public boolean canBeCharged() {
        return true;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return BASE_MANA_COST;
    }
}
