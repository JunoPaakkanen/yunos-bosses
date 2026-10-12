package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class AntiEscapeMagic extends Spell {

    public AntiEscapeMagic(Identifier id, SpellRarity rarity) {
        super(id, true, rarity);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient()) return;

        if (world instanceof ServerWorld serverWorld) {
            double range = 80.0;
            int durationTicks = 20 * 60 * 20; // 24000 ticks = 20 minutes

            // Subtle sound cues: heavy chain clanking and distant anvil locking
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.HOSTILE, 1.5F, 0.5F);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.8F, 0.5F);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 1.4F, 0.7F);

            // Expanding rings of heavy dark slate particles
            DustParticleEffect heavyDust = new DustParticleEffect(0x2B3A42, 1.3F);
            for (int r = 2; r <= 8; r += 2) {
                for (int i = 0; i < 24; i++) {
                    double angle = (2.0 * Math.PI * i) / 24.0;
                    double px = caster.getX() + Math.cos(angle) * r;
                    double pz = caster.getZ() + Math.sin(angle) * r;
                    serverWorld.spawnParticles(heavyDust, px, caster.getY() + 0.3, pz, 1, 0, 0.05, 0, 0.01);
                    if (i % 6 == 0) {
                        serverWorld.spawnParticles(ParticleTypes.CRIT, px, caster.getY() + 0.4, pz, 1, 0.02, 0.05, 0.02, 0.02);
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
                // Apply Anti-Escape effect for 20 minutes (24000 ticks)
                target.addStatusEffect(new StatusEffectInstance(
                        ModEffects.ANTI_ESCAPE,
                        durationTicks,
                        0,
                        false, // ambient
                        true,  // showParticles
                        true   // showIcon
                ));

                // Force stop active gliding immediately
                if (target.isGliding()) {
                    target.stopGliding();
                }

                // Small heavy dust aura on affected entities
                serverWorld.spawnParticles(heavyDust,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        8, 0.3, 0.5, 0.3, 0.02);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.HOSTILE, 1.0F, 0.7F);

                if (target instanceof ServerPlayerEntity player) {
                    player.sendMessage(Text.literal("§6§l[Anti-Escape]§r §cFlight and aquatic mobility are sealed for 20 minutes!§r"), true);
                }
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.anti_escape_magic");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
