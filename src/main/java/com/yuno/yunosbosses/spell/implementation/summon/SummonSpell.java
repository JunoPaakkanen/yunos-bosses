package com.yuno.yunosbosses.spell.implementation.summon;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.Objects;

public class SummonSpell extends Spell {

    public SummonSpell(Identifier id) {
        super(id);
    }

    private static Team getOrCreateVexTeam(Scoreboard scoreboard) {

        Team team = scoreboard.getTeam("PeacefulVex");

        if (team == null) {
            team = scoreboard.addTeam("PeacefulVex");
            team.setFriendlyFireAllowed(false);
        }
        return team;
    }

    @Override
    public void cast(World world, PlayerEntity player, ItemStack staff) {
        if (!world.isClient) {
            // Raycast to find the entity the player is looking at and set them as target.
            Entity target = Objects.requireNonNull(ProjectileUtil.raycast(player,
                    player.getCameraPosVec(1.0f),
                    player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(30.0D)),
                    player.getBoundingBox().stretch(player.getRotationVec(1.0f).multiply(30.0D)).expand(1.0D, 1.0D, 1.0D),
                    entity -> !entity.isSpectator() && entity.canHit(),
                    900.0D
            )).getEntity();

            VexEntity vex = new VexEntity(EntityType.VEX, world);

            // Create team
            Scoreboard scoreboard = world.getScoreboard();
            Team vexTeam = getOrCreateVexTeam(scoreboard);

            // Add Player and Vex to the team
            scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), vexTeam);
            scoreboard.addScoreHolderToTeam(vex.getUuidAsString(), vexTeam);

            vex.setLifeTicks(1200);

            vex.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), 0, 0);

            if (target instanceof LivingEntity livingTarget) {
                vex.setTarget(livingTarget);
            }

            world.spawnEntity(vex);
        }
    }
}
