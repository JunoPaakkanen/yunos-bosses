package com.yuno.yunosbosses.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.VexEntity;

import java.util.EnumSet;

/**
 * AI target goal for summoned Vexes:
 * Targets the first entity that attacks the summoner.
 */
public class VexDefendSummonerGoal extends TrackTargetGoal {
    private final VexEntity vex;
    private final LivingEntity summoner;
    private LivingEntity attacker;
    private int lastAttackedTime;

    public VexDefendSummonerGoal(VexEntity vex, LivingEntity summoner) {
        super(vex, false);
        this.vex = vex;
        this.summoner = summoner;
        this.setControls(EnumSet.of(Goal.Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (this.summoner == null || !this.summoner.isAlive()) {
            return false;
        }

        // Do not interrupt while current target is still alive and engaged
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            return false;
        }

        LivingEntity summonerAttacker = this.summoner.getAttacker();
        int summonerLastAttackedTime = this.summoner.getLastAttackedTime();

        if (summonerAttacker != null && summonerAttacker.isAlive()
                && summonerAttacker != this.summoner
                && !summonerAttacker.isTeammate(this.summoner)
                && summonerAttacker != this.mob
                && !summonerAttacker.isTeammate(this.mob)) {
            if (summonerLastAttackedTime != this.lastAttackedTime) {
                this.attacker = summonerAttacker;
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacker);
        this.vex.setBounds(null); // Release patrol bounds so the vex can chase freely
        if (this.summoner != null) {
            this.lastAttackedTime = this.summoner.getLastAttackedTime();
        }
        super.start();
    }

    @Override
    public void stop() {
        if (this.mob.getTarget() == null || !this.mob.getTarget().isAlive()) {
            this.mob.setTarget(null);
            if (this.summoner != null && this.summoner.isAlive()) {
                this.vex.setBounds(this.summoner.getBlockPos());
            }
        }
        super.stop();
    }
}
