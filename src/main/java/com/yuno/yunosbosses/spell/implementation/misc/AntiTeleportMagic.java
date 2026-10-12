package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class AntiTeleportMagic extends Spell {

    public AntiTeleportMagic(Identifier id, SpellRarity rarity) {
        super(id, true, rarity); // canCastWithoutStaff = true
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient()) return;

        if (world instanceof ServerWorld serverWorld) {
            double range = 80.0;
            int durationTicks = 20 * 60 * 20; // 24000 ticks = 20 minutes

            // Subtle sound cues: deep resonant chime and anchor lock
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.2F, 0.7F);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.9F, 1.6F);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.4F, 0.6F);

            // Subtle visual particle effect: expanding thin ring of deep purple dust and portal sparks
            DustParticleEffect deepPurpleDust = new DustParticleEffect(0x4A148C, 1.2F);
            for (int r = 2; r <= 8; r += 2) {
                for (int i = 0; i < 24; i++) {
                    double angle = (2.0 * Math.PI * i) / 24.0;
                    double px = caster.getX() + Math.cos(angle) * r;
                    double pz = caster.getZ() + Math.sin(angle) * r;
                    serverWorld.spawnParticles(deepPurpleDust, px, caster.getY() + 0.3, pz, 1, 0, 0.05, 0, 0.01);
                    if (i % 4 == 0) {
                        serverWorld.spawnParticles(ParticleTypes.PORTAL, px, caster.getY() + 0.5, pz, 1, 0.05, 0.1, 0.05, 0.02);
                    }
                }
            }

            // Find all living entities in 80 block radius except caster and allies
            List<LivingEntity> targets = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    caster.getBoundingBox().expand(range),
                    target -> target != caster && target.isAlive() && !target.isTeammate(caster)
            );

            for (LivingEntity target : targets) {
                // Apply Anti-Teleport effect for 20 minutes (24000 ticks)
                target.addStatusEffect(new StatusEffectInstance(
                        ModEffects.ANTI_TELEPORT,
                        durationTicks,
                        0,
                        false, // ambient
                        true,  // showParticles
                        true   // showIcon
                ));

                // Small purple dust aura on affected entities
                serverWorld.spawnParticles(deepPurpleDust,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        8, 0.3, 0.5, 0.3, 0.02);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.8F, 0.7F);
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.anti_teleport_magic");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
