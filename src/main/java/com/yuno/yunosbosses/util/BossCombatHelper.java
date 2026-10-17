package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class BossCombatHelper {

    /**
     * Checks if an incoming damage source represents magic or spell damage.
     */
    public static boolean isMagicDamage(DamageSource source) {
        if (source == null) return false;
        if (source.isOf(ModDamageTypes.CUTTING_MAGIC)
                || source.isOf(ModDamageTypes.CUTTING_MAGIC_SHALLOW)
                || source.isOf(ModDamageTypes.FIRE_MAGIC)
                || source.isOf(ModDamageTypes.DOMAIN_EXPANSION)) {
            return true;
        }
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            return true;
        }
        if (source.isIn(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the attack represents a heavy spell capable of shattering cursed energy reinforcement.
     * Heavy spells include powerful direct spell hits dealing >= 18.0F damage or fire magic.
     */
    public static boolean isHeavySpell(DamageSource source, float amount) {
        if (!isMagicDamage(source)) {
            return false;
        }
        if (source.isOf(ModDamageTypes.FIRE_MAGIC)) {
            return true;
        }
        return amount >= 16.0F;
    }

    /**
     * Applies Cursed Energy Reinforcement (80% physical damage reduction)
     * and caps single-hit vanilla damage (e.g. maces, fall crits).
     */
    public static float applyReinforcementAndCap(YunosBossEntity boss, LivingEntity bossEntity, DamageSource source, float amount) {
        if (!isMagicDamage(source)) {
            boolean isDuo = DuoModeCheckHelper.isDuoMode(bossEntity.getWorld());
            float hardCap = isDuo ? 18.0F : 25.0F;

            // Cap non-magic attack damage per hit
            amount = Math.min(amount, hardCap);

            // Apply 80% cursed energy reinforcement damage reduction if intact
            if (!boss.isReinforcementBroken()) {
                amount *= 0.20F;

                if (bossEntity.getWorld() instanceof ServerWorld sw) {
                    sw.playSound(null, bossEntity.getX(), bossEntity.getY(), bossEntity.getZ(),
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 0.7F, 1.6F);
                    sw.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                            bossEntity.getX(), bossEntity.getY() + 1.0, bossEntity.getZ(),
                            5, 0.25, 0.35, 0.25, 0.05);
                }
            }
        }
        return amount;
    }

    /**
     * Executes boss reinforcement guard break.
     */
    public static void executeReinforcementBreak(YunosBossEntity boss, LivingEntity bossEntity, DamageSource source, int durationTicks) {
        boss.setReinforcementBrokenTicks(durationTicks);

        World world = bossEntity.getWorld();
        if (world instanceof ServerWorld sw) {
            sw.playSound(null, bossEntity.getX(), bossEntity.getY(), bossEntity.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.0F, 0.9F);
            sw.playSound(null, bossEntity.getX(), bossEntity.getY(), bossEntity.getZ(),
                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1.8F, 1.1F);
            sw.playSound(null, bossEntity.getX(), bossEntity.getY(), bossEntity.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.5F, 1.4F);

            sw.spawnParticles(ParticleTypes.CRIT,
                    bossEntity.getX(), bossEntity.getY() + 1.2, bossEntity.getZ(),
                    16, 0.3, 0.4, 0.3, 0.1);

            if (source.getAttacker() instanceof PlayerEntity player) {
                player.sendMessage(Text.literal("§6§l[GUARD BREAK]§r §eReinforcement broken! (" + (durationTicks / 20) + "s)§r"), true);
            }
        }
    }

    /**
     * Applies partial armor-piercing damage for signature boss abilities:
     * 50% normal damage mitigated by armor/toughness/protection,
     * 50% true armor-piercing damage (CUTTING_MAGIC).
     */
    public static void applySignatureDamage(ServerWorld world, LivingEntity target, Entity attacker, float totalDamage) {
        if (target == null || !target.isAlive() || totalDamage <= 0.0F) return;

        float half = totalDamage * 0.5F;
        DamageSource normalSource = attacker instanceof MobEntity mob
                ? world.getDamageSources().mobAttack(mob)
                : (attacker instanceof LivingEntity living ? world.getDamageSources().indirectMagic(living, attacker) : world.getDamageSources().generic());

        // 1. Deal first half through normal armor mitigation
        target.damage(world, normalSource, half);

        // 2. Clear hurt immunity ticks so piercing component registers immediately
        target.timeUntilRegen = 0;

        // 3. Deal second half through armor-piercing damage source
        DamageSource piercingSource = ModDamageTypes.of(world, ModDamageTypes.CUTTING_MAGIC, attacker);
        target.damage(world, piercingSource, half);
    }

    /**
     * Overload accepting base damage source.
     */
    public static void applySignatureDamage(ServerWorld world, LivingEntity target, DamageSource baseSource, float totalDamage) {
        if (target == null || !target.isAlive() || totalDamage <= 0.0F) return;

        float half = totalDamage * 0.5F;
        target.damage(world, baseSource, half);

        target.timeUntilRegen = 0;

        DamageSource piercingSource = ModDamageTypes.of(world, ModDamageTypes.CUTTING_MAGIC, baseSource.getAttacker());
        target.damage(world, piercingSource, half);
    }

    /**
     * Determines whether incoming damage should cap the victim's Resistance effect to Resistance II (amplifier 1, 40% reduction).
     * Applies to:
     * 1. All domain expansion sure-hit damage (DomainExpansionShrine, DomainExpansionTCMP).
     * 2. All attacks originating directly from a boss (YunosBossEntity) or boss projectiles/minions.
     */
    public static boolean shouldCapResistance(DamageSource source) {
        if (source == null) return false;

        // 1. Domain expansion sure-hit damage
        if (source.isOf(ModDamageTypes.DOMAIN_EXPANSION)) {
            return true;
        }

        // 2. Boss attacker (melee, direct spell, projectile owner)
        Entity attacker = source.getAttacker();
        if (attacker instanceof YunosBossEntity) {
            return true;
        }

        // 3. Direct damage source is a boss or projectile owned by a boss
        Entity directSource = source.getSource();
        if (directSource instanceof YunosBossEntity) {
            return true;
        }
        if (directSource instanceof ProjectileEntity proj && proj.getOwner() instanceof YunosBossEntity) {
            return true;
        }

        // 4. Boss summoned minion (e.g. Methode's summoned vexes)
        if (attacker instanceof LivingEntity living) {
            for (String tag : living.getCommandTags()) {
                if (tag.startsWith("Caster_")) {
                    return true;
                }
            }
        }

        return false;
    }
}
