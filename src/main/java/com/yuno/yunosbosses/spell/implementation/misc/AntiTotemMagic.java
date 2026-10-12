package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AntiTotemMagic extends Spell {

    public AntiTotemMagic(Identifier id, SpellRarity rarity) {
        super(id, true, rarity);
    }

    /**
     * Guaranteed single-target cast on a specific entity.
     */
    public void castOnTarget(World world, LivingEntity caster, LivingEntity target) {
        if (world.isClient() || target == null || !target.isAlive()) return;

        if (world instanceof ServerWorld serverWorld) {
            int durationTicks = 20 * 60 * 20; // 24000 ticks = 20 minutes

            // Audio cues: distorted totem snap, anchor deplete, and ominous chime
            serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 1.4F, 0.4F);
            serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.5F, 0.7F);
            serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.8F, 1.6F);

            // Particle beam connecting caster to target
            Vec3d start = caster.getEyePos();
            Vec3d end = target.getEyePos();
            Vec3d diff = end.subtract(start);
            double dist = diff.length();
            Vec3d step = diff.normalize().multiply(0.5);
            DustParticleEffect crimsonDust = new DustParticleEffect(0x8B0000, 1.4F);

            for (double d = 0; d < dist; d += 0.5) {
                Vec3d pos = start.add(step.multiply(d / 0.5));
                serverWorld.spawnParticles(crimsonDust, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
                if (d % 2.0 < 0.5) {
                    serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }

            // Burst around target
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.5, 0.4, 0.05);
            serverWorld.spawnParticles(crimsonDust, target.getX(), target.getY() + 1.0, target.getZ(),
                    25, 0.4, 0.6, 0.4, 0.02);

            // Apply unremovable Anti-Totem effect for 20 minutes
            target.addStatusEffect(new StatusEffectInstance(
                    ModEffects.ANTI_TOTEM,
                    durationTicks,
                    0,
                    false, // ambient
                    true,  // showParticles
                    true   // showIcon
            ));

            if (target instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.literal("§4§l[Anti-Totem]§r §cThe boss has sealed your Totems of Undying for 20 minutes!§r"), false);
            }
        }
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient()) return;

        // If mob has an attack target, prioritize that target
        if (caster instanceof MobEntity mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            castOnTarget(world, caster, mob.getTarget());
            return;
        }

        // Otherwise raycast for looking entity
        Vec3d eyePos = caster.getEyePos();
        Vec3d lookVec = caster.getRotationVec(1.0F);
        double range = 48.0;
        Vec3d reach = eyePos.add(lookVec.multiply(range));
        Box box = caster.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
                caster,
                eyePos,
                reach,
                box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isTeammate(caster),
                range * range
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            castOnTarget(world, caster, living);
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.anti_totem_magic");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
