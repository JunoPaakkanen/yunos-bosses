package com.yuno.yunosbosses.spell.implementation.summon;

import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class SummonVexSpell extends Spell {

    public SummonVexSpell(Identifier id, SpellRarity rarity) {
        super(id, rarity);
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
    public float getManaCost(LivingEntity caster) {
        if (caster.getMainHandStack().getItem() instanceof StaffItem staffItem) {
            return staffItem.getPowerMultiplier() >= 1.2F ? 70.0F : 50.0F;
        } else if (caster.getOffHandStack().getItem() instanceof StaffItem staffItem) {
            return staffItem.getPowerMultiplier() >= 1.2F ? 70.0F : 50.0F;
        }
        return 50.0F;
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            Vec3d eyePos = caster.getCameraPosVec(1.0f);
            Vec3d look = caster.getRotationVec(1.0f);
            Vec3d reach = eyePos.add(look.multiply(30.0D));

            // Raycast safely without throwing NPE when casting into empty air
            EntityHitResult hitResult = ProjectileUtil.raycast(
                    caster,
                    eyePos,
                    reach,
                    caster.getBoundingBox().stretch(look.multiply(30.0D)).expand(1.0D, 1.0D, 1.0D),
                    entity -> !entity.isSpectator() && entity.canHit() && entity != caster && !entity.isTeammate(caster),
                    900.0D
            );

            LivingEntity target = null;
            if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
                target = living;
            } else {
                // Fallback: search for hostile mobs within 16 blocks targeting the caster or closest hostile
                Box searchBox = caster.getBoundingBox().expand(16.0);
                List<HostileEntity> nearbyHostiles = serverWorld.getEntitiesByClass(
                        HostileEntity.class,
                        searchBox,
                        e -> e.isAlive() && !e.isTeammate(caster)
                );
                if (!nearbyHostiles.isEmpty()) {
                    // Prioritize hostile targeting caster, otherwise closest
                    target = nearbyHostiles.stream()
                            .min((a, b) -> {
                                boolean aAggro = (a.getTarget() == caster);
                                boolean bAggro = (b.getTarget() == caster);
                                if (aAggro != bAggro) return aAggro ? -1 : 1;
                                return Double.compare(a.squaredDistanceTo(caster), b.squaredDistanceTo(caster));
                            })
                            .orElse(null);
                }
            }

            // Power scaling: upgraded staves (power >= 1.2x) summon an extra vex
            float power = 1.0F;
            if (staff.getItem() instanceof StaffItem staffItem) {
                power = staffItem.getPowerMultiplier();
            }
            int summonCount = (power >= 1.2F) ? 2 : 1;

            Scoreboard scoreboard = world.getScoreboard();
            Team vexTeam = getOrCreateVexTeam(scoreboard);
            scoreboard.addScoreHolderToTeam(caster.getNameForScoreboard(), vexTeam);

            // Perpendicular vector for spreading out multiple summons
            Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.001) {
                right = new Vec3d(1, 0, 0);
            }

            for (int i = 0; i < summonCount; i++) {
                double offset = (summonCount > 1) ? ((i == 0) ? -0.75 : 0.75) : 0.0;
                Vec3d spawnPos = caster.getEyePos()
                        .add(look.multiply(1.5))
                        .add(right.multiply(offset))
                        .add(0, 0.2, 0);

                VexEntity vex = new VexEntity(EntityType.VEX, world);
                vex.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, caster.getYaw(), caster.getPitch());

                // Friendly team setup
                scoreboard.addScoreHolderToTeam(vex.getUuidAsString(), vexTeam);

                // 60-second lifespan (1200 ticks)
                vex.setLifeTicks(1200);

                // Equip Iron Sword with 0% drop chance to guarantee offensive capability
                vex.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                vex.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                if (target != null) {
                    vex.setTarget(target);
                }

                world.spawnEntity(vex);

                // Visual and audio effects per vex
                serverWorld.spawnParticles(ParticleTypes.PORTAL, spawnPos.x, spawnPos.y, spawnPos.z, 20, 0.25, 0.25, 0.25, 0.08);
                serverWorld.spawnParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 15, 0.3, 0.3, 0.3, 0.5);
                serverWorld.spawnParticles(ParticleTypes.SOUL, spawnPos.x, spawnPos.y, spawnPos.z, 8, 0.2, 0.2, 0.2, 0.02);
                serverWorld.spawnParticles(ParticleTypes.POOF, spawnPos.x, spawnPos.y, spawnPos.z, 6, 0.15, 0.15, 0.15, 0.02);
            }

            world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.2F, 1.1F);
            world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.summon_vex");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
