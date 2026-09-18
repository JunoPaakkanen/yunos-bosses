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

    public ProjectileEntity findIncomingProjectile(MobEntity boss) {
        Box searchBox = boss.getBoundingBox().expand(detectionRadius);
        List<ProjectileEntity> incoming = boss.getWorld().getEntitiesByClass(
                ProjectileEntity.class,
                searchBox,
                projectile -> isProjectileHeadingTowardsBoss(boss, projectile)
        );

        if (incoming.isEmpty()) return null;

        ProjectileEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (ProjectileEntity p : incoming) {
            double d = p.squaredDistanceTo(boss);
            if (d < closestDistSq) {
                closestDistSq = d;
                closest = p;
            }
        }
        return closest;
    }

    public static boolean isProjectileHeadingTowardsBoss(MobEntity boss, ProjectileEntity projectile) {
        if (projectile == null || !projectile.isAlive() || projectile.isOnGround()) return false;
        if (projectile.getOwner() != null && projectile.getOwner().getUuid().equals(boss.getUuid())) return false;

        Vec3d projVelocity = projectile.getVelocity();
        if (projVelocity.lengthSquared() < 0.04) return false;

        // Vector pointing from projectile to boss bounding box center (covers head, torso, and feet)
        Vec3d toBoss = boss.getBoundingBox().getCenter().subtract(projectile.getPos());
        if (toBoss.lengthSquared() < 0.01) return true;

        Vec3d toBossNorm = toBoss.normalize();
        Vec3d projDir = projVelocity.normalize();

        // Dot product check: > 0.40 (roughly 66-degree cone) catches arcing arrows and tridents
        return projDir.dotProduct(toBossNorm) > 0.40;
    }

    @Override
    public boolean canUse(MobEntity boss, LivingEntity target, double distanceSq) {
        return findIncomingProjectile(boss) != null;
    }

    @Override public int getWindupTicks() { return windupTicks; }
    @Override public int getRecoveryTicks() { return recoveryTicks; }

    @Override
    public void execute(MobEntity boss, LivingEntity target) {
        ProjectileEntity incoming = findIncomingProjectile(boss);
        if (incoming != null) {
            Vec3d toProj = incoming.getPos().subtract(boss.getEyePos()).normalize();
            double dx = toProj.x;
            double dy = toProj.y;
            double dz = toProj.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-(Math.atan2(dy, horizDist) * (180.0 / Math.PI)));
            boss.setHeadYaw(targetYaw);
            boss.setBodyYaw(targetYaw);
            boss.setYaw(targetYaw);
            boss.setPitch(targetPitch);
        }

        Spell spell = spellSupplier.get();
        if (spell != null) {
            spell.cast(boss.getWorld(), boss, boss.getMainHandStack());
        }
    }
}
