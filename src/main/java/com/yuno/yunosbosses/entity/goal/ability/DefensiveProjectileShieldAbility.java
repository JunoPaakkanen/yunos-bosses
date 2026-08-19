package com.yuno.yunosbosses.entity.goal.ability;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Supplier;

public class DefensiveProjectileShieldAbility implements BossAbility {
    private final double detectionRadius;
    private final int windupTicks;
    private final int recoveryTicks;
    private final Supplier<Spell> spellSupplier;

    public DefensiveProjectileShieldAbility(double detectionRadius, int windupTicks, int recoveryTicks, Supplier<Spell> spellSupplier) {
        this.detectionRadius = detectionRadius;
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.spellSupplier = spellSupplier;
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        // Search area around the boss
        Box searchBox = boss.getBoundingBox().expand(detectionRadius);
        List<ProjectileEntity> incomingProjectiles = boss.getWorld().getEntitiesByClass(
                ProjectileEntity.class,
                searchBox,
                projectile -> isProjectileHeadingTowardsBoss(boss, projectile)
        );

        return !incomingProjectiles.isEmpty();
    }

    private boolean isProjectileHeadingTowardsBoss(MobEntity boss, ProjectileEntity projectile) {
        // Ignore projectiles shot by the boss itself
        if (projectile.getOwner() == boss) return false;

        Vec3d projVelocity = projectile.getVelocity();
        if (projVelocity.lengthSquared() < 0.01) return false;

        // Vector pointing from projectile to boss center
        Vec3d toBoss = boss.getEyePos().subtract(projectile.getPos()).normalize();
        Vec3d projDir = projVelocity.normalize();

        // Dot product check: > 0.7 means projectile is moving roughly toward the boss
        return projDir.dotProduct(toBoss) > 0.7;
    }

    @Override public int getWindupTicks() { return windupTicks; }
    @Override public int getRecoveryTicks() { return recoveryTicks; }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        Spell spell = spellSupplier.get();
        if (spell != null) {
            spell.cast(boss.getWorld(), boss, boss.getMainHandStack());
        }
    }
}