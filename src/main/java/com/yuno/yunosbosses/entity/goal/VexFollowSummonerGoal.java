package com.yuno.yunosbosses.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Keeps a summoned Vex following and hovering around its summoner while not engaged in combat.
 * Prevents summoned vexes from wandering off aimlessly into unloaded chunks or caves.
 */
public class VexFollowSummonerGoal extends Goal {
    private final VexEntity vex;
    private final LivingEntity summoner;

    public VexFollowSummonerGoal(VexEntity vex, LivingEntity summoner) {
        this.vex = vex;
        this.summoner = summoner;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.summoner == null || !this.summoner.isAlive()) {
            return false;
        }
        LivingEntity target = this.vex.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }
        return this.vex.squaredDistanceTo(this.summoner) > 25.0; // > 5 blocks
    }

    @Override
    public boolean shouldContinue() {
        if (this.summoner == null || !this.summoner.isAlive()) {
            return false;
        }
        LivingEntity target = this.vex.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }
        return this.vex.squaredDistanceTo(this.summoner) > 9.0; // > 3 blocks
    }

    @Override
    public void start() {
        if (this.summoner != null && this.summoner.isAlive()) {
            this.vex.setBounds(this.summoner.getBlockPos());
        }
    }

    @Override
    public void tick() {
        if (this.summoner == null || !this.summoner.isAlive()) {
            return;
        }

        // Anchor the idle hover bounds to the summoner's position
        this.vex.setBounds(this.summoner.getBlockPos());

        double distSq = this.vex.squaredDistanceTo(this.summoner);

        // Teleport if too far behind (> 32 blocks)
        if (distSq > 1024.0) {
            double offsetX = (this.vex.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetZ = (this.vex.getRandom().nextDouble() - 0.5) * 2.0;
            this.vex.refreshPositionAndAngles(
                    this.summoner.getX() + offsetX,
                    this.summoner.getY() + 1.2,
                    this.summoner.getZ() + offsetZ,
                    this.summoner.getYaw(),
                    this.summoner.getPitch()
            );
            this.vex.setVelocity(Vec3d.ZERO);
        } else if (distSq > 16.0) {
            // Fly smoothly towards summoner eye level
            this.vex.getMoveControl().moveTo(
                    this.summoner.getX(),
                    this.summoner.getY() + 1.5,
                    this.summoner.getZ(),
                    0.35
            );
            this.vex.getLookControl().lookAt(this.summoner, 20.0F, 20.0F);
        }
    }
}
