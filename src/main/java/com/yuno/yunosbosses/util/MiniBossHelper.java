package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.entity.YunosBossEntity;
import com.yuno.yunosbosses.entity.YunosMiniBossEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class MiniBossHelper {

    public static final float SPAWN_CHANCE = 0.03F; // 3% chance
    public static final float BLACK_FLASH_CHANCE = 0.02F; // 2% chance on melee hit

    public static final Identifier HEALTH_MODIFIER_ID = Identifier.of(YunosBosses.MOD_ID, "miniboss_health_boost");
    public static final Identifier DAMAGE_MODIFIER_ID = Identifier.of(YunosBosses.MOD_ID, "miniboss_damage_boost");
    public static final Identifier SPEED_MODIFIER_ID = Identifier.of(YunosBosses.MOD_ID, "miniboss_speed_boost");
    public static final Identifier SCALE_MODIFIER_ID = Identifier.of(YunosBosses.MOD_ID, "miniboss_scale_boost");

    private static final DustParticleEffect CRIMSON_CURSED_DUST = new DustParticleEffect(0xB80028, 1.15f);
    private static final DustParticleEffect DARK_CURSED_DUST = new DustParticleEffect(0x12051A, 1.35f);

    /**
     * Checks if a MobEntity is eligible to roll for Mini-Boss promotion.
     * Excludes YunosBossEntity (mod bosses), vanilla bosses, and non-hostile mobs.
     */
    public static boolean isEligibleHostileMob(MobEntity mob) {
        if (mob == null) return false;
        if (mob instanceof YunosBossEntity) return false;
        if (mob instanceof EnderDragonEntity || mob instanceof WitherEntity || mob instanceof WardenEntity || mob instanceof ElderGuardianEntity) {
            return false;
        }
        // Exclude player/boss-summoned minions (e.g., Methode/player summoned Vexes)
        for (String tag : mob.getCommandTags()) {
            if (tag.startsWith("Caster_")) {
                return false;
            }
        }
        return mob instanceof Monster || mob.getType().getSpawnGroup() == SpawnGroup.MONSTER;
    }

    /**
     * Evaluates a newly spawned mob once and promotes it to a Mini-Boss with a 1% chance.
     */
    public static void tryEvaluateMiniBossSpawn(MobEntity mob, ServerWorld world) {
        if (!(mob instanceof YunosMiniBossEntity miniBoss)) return;
        if (miniBoss.yunos$isMiniBossEvaluated()) return;

        miniBoss.yunos$setMiniBossEvaluated(true);

        if (!isEligibleHostileMob(mob)) {
            return;
        }

        if (world.getRandom().nextFloat() < SPAWN_CHANCE) {
            promoteToMiniBoss(mob, world);
        }
    }

    /**
     * Promotes the given mob to a Mini-Boss and applies Single/Duo mode stat scaling, scale, and nameplate.
     */
    public static void promoteToMiniBoss(MobEntity mob, ServerWorld world) {
        if (!(mob instanceof YunosMiniBossEntity miniBoss)) return;

        miniBoss.yunos$setMiniBoss(true);
        miniBoss.yunos$setMiniBossEvaluated(true);

        boolean isDuo = DuoModeCheckHelper.isDuoMode(world);
        applyMiniBossStats(mob, isDuo, true);

        // Apply custom nameplate if not already named
        if (!mob.hasCustomName()) {
            Text baseName = mob.getType().getName();
            Text prefix = Text.literal("[Cursed] ").formatted(Formatting.DARK_RED, Formatting.BOLD);
            Text coloredName = baseName.copy().formatted(Formatting.RED);
            mob.setCustomName(prefix.copy().append(coloredName));
            mob.setCustomNameVisible(false); // Shows when looking at or close to the mob
        }

        mob.setPersistent();
    }

    /**
     * Applies or updates the Mini-Boss attribute modifiers based on Single vs Duo mode.
     * Single Mode: 3.0x HP (+200%), 1.5x Damage (+50%), +15% Speed, +15% Scale
     * Duo Mode:    4.0x HP (+300%), 1.7x Damage (+70%), +20% Speed, +15% Scale
     */
    public static void applyMiniBossStats(MobEntity mob, boolean isDuo, boolean healToFull) {
        double healthMult = isDuo ? 3.0D : 2.0D; // ADD_MULTIPLIED_TOTAL: +2.0 = 3x, +3.0 = 4x
        double damageMult = isDuo ? 0.70D : 0.50D; // ADD_MULTIPLIED_TOTAL: +0.5 = 1.5x, +0.7 = 1.7x
        double speedMult = isDuo ? 0.20D : 0.15D; // ADD_MULTIPLIED_TOTAL: +0.15 = +15%, +0.20 = +20%
        double scaleMult = 0.15D; // +15% larger size

        float previousHealthRatio = mob.getMaxHealth() > 0 ? (mob.getHealth() / mob.getMaxHealth()) : 1.0F;

        updateModifier(mob.getAttributeInstance(EntityAttributes.MAX_HEALTH), HEALTH_MODIFIER_ID, healthMult, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(mob.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID, damageMult, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(mob.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID, speedMult, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(mob.getAttributeInstance(EntityAttributes.SCALE), SCALE_MODIFIER_ID, scaleMult, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (healToFull) {
            mob.setHealth(mob.getMaxHealth());
        } else {
            mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * previousHealthRatio));
        }
    }

    private static void updateModifier(EntityAttributeInstance instance, Identifier id, double value, EntityAttributeModifier.Operation operation) {
        if (instance == null) return;
        EntityAttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.value() - value) < 1e-4) {
                return;
            }
            instance.removeModifier(id);
        }
        instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
    }

    /**
     * Ticks Mini-Boss ambient Cursed Energy aura and reinforcement break recovery.
     */
    public static void tickMiniBoss(MobEntity mob, ServerWorld world) {
        if (!(mob instanceof YunosMiniBossEntity miniBoss) || !miniBoss.yunos$isMiniBoss()) return;

        // Periodically ensure stats match current Single/Duo gamerule (every 40 ticks)
        if (mob.age % 40 == 0) {
            boolean isDuo = DuoModeCheckHelper.isDuoMode(world);
            applyMiniBossStats(mob, isDuo, false);
        }

        int brokenTicks = miniBoss.yunos$getReinforcementBrokenTicks();
        if (brokenTicks > 0) {
            miniBoss.yunos$setReinforcementBrokenTicks(brokenTicks - 1);
            if (mob.age % 4 == 0) {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        mob.getX(), mob.getY() + (mob.getHeight() * 0.5), mob.getZ(),
                        2, mob.getWidth() * 0.35, mob.getHeight() * 0.35, mob.getWidth() * 0.35, 0.05);
            }
        } else {
            // Ambient Cursed Energy Aura while reinforcement is intact
            if (mob.age % 5 == 0) {
                double width = Math.max(0.4, mob.getWidth() * 0.6);
                double height = Math.max(0.5, mob.getHeight() * 0.5);
                double centerY = mob.getY() + height;

                world.spawnParticles(CRIMSON_CURSED_DUST,
                        mob.getX(), centerY, mob.getZ(),
                        2, width, height * 0.7, width, 0.01);
                world.spawnParticles(DARK_CURSED_DUST,
                        mob.getX(), centerY, mob.getZ(),
                        2, width, height * 0.7, width, 0.01);
                if (mob.age % 15 == 0) {
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            mob.getX(), centerY, mob.getZ(),
                            1, width * 0.8, height * 0.6, width * 0.8, 0.015);
                }
            }
        }
    }
}
