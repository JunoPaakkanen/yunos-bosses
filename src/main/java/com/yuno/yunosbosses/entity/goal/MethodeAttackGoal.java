package com.yuno.yunosbosses.entity.goal;

import com.yuno.yunosbosses.entity.character.MethodeEntity;
import com.yuno.yunosbosses.entity.goal.ability.BossAbility;
import com.yuno.yunosbosses.entity.goal.ability.DefensiveProjectileShieldAbility;
import com.yuno.yunosbosses.entity.goal.ability.MeleeAttackAbility;
import com.yuno.yunosbosses.entity.goal.ability.SpellCastAbility;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.util.DuoModeCheckHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class MethodeAttackGoal extends AbstractBossAttackGoal {
    private final MethodeEntity methode;
    private int restraintCooldown = 0; // Cooldown for close-quarters repulsion / restraint magic

    public MethodeAttackGoal(MethodeEntity methode, double speed) {
        super(methode, speed);
        this.methode = methode;

        // Ideal distance: maintains 7.5 blocks spacing (tolerance 2.0 -> 5.5 to 9.5 blocks)
        this.setIdealDistance(7.5);

        // 1. Reactive Defensive Barrier (intercepts incoming projectiles within 12 blocks, 100-tick / 5-second cooldown)
        this.registerAbility(new DefensiveProjectileShieldAbility(12.0, 0, 100, () -> ModSpells.DEFENSIVE_MAGIC));

        // 2. Point Blank Melee Staff Strike (0.0 to 3.5 blocks) - 14-tick windup, 16-tick recovery (matches 1.5s animation)
        this.registerAbility(new MeleeAttackAbility(3.5, 14, 16, 9.0F));

        // 3. Medium Range Killing Magic Barrage (3.0 to 24.0 blocks) - 14-tick windup, 16-tick recovery
        this.registerAbility(new SpellCastAbility(3.0, 24.0, 14, 16, () -> ModSpells.KILLING_MAGIC_BARRAGE));

        // 4. Long Range Zoltraak Beam (16.0 to 38.0 blocks — snipes retreating targets) - 15-tick windup, 25-tick recovery
        this.registerAbility(new SpellCastAbility(16.0, 38.0, 15, 25, () -> ModSpells.KILLING_MAGIC));
    }

    public int getRestraintCooldown() {
        return this.restraintCooldown;
    }

    @Override
    public void tick() {
        if (this.restraintCooldown > 0) {
            this.restraintCooldown--;
        }

        // Proactive check: if 2+ players are within 4.0 blocks of Methode, trigger restraint repulsion
        if (this.restraintCooldown <= 0) {
            checkAndExecuteRestraintRepulsion();
        }

        super.tick();
    }

    /**
     * Checks if 2 or more players are crowding Methode in close quarters (<= 4.0 blocks).
     * If detected, unleashes an omnidirectional golden wave: binds the secondary attacker with golden ribbons
     * while repelling the primary attacker.
     */
    public boolean checkAndExecuteRestraintRepulsion() {
        if (this.restraintCooldown > 0) return false;
        World world = this.methode.getWorld();
        if (!(world instanceof ServerWorld sw)) return false;

        List<PlayerEntity> closePlayers = sw.getEntitiesByClass(
                PlayerEntity.class,
                this.methode.getBoundingBox().expand(4.0),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && this.methode.distanceTo(p) <= 4.0
        );

        if (closePlayers.size() < 2) return false;

        executeRestraintRepulsion(sw, closePlayers);
        return true;
    }

    private void executeRestraintRepulsion(ServerWorld sw, List<PlayerEntity> closePlayers) {
        this.restraintCooldown = DuoModeCheckHelper.isDuoMode(sw) ? 160 : 220; // 8s Duo, 11s standard
        this.globalCooldown = 15;

        // Interrupt active ability cleanly
        if (this.activeAbility != null) {
            this.activeAbility.stop(this.methode);
            this.activeAbility = null;
            this.attackTimer = 0;
        }

        // 1. Animation & Audio
        this.methode.triggerAttackAnim();

        sw.playSound(null, this.methode.getX(), this.methode.getY(), this.methode.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.8F, 1.2F);
        sw.playSound(null, this.methode.getX(), this.methode.getY(), this.methode.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.6F, 1.3F);
        sw.playSound(null, this.methode.getX(), this.methode.getY(), this.methode.getZ(),
                SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1.5F, 1.6F);

        // 2. Omnidirectional Golden Wave particles
        Vec3d center = this.methode.getEyePos().subtract(0, 0.3, 0);
        int ringParticles = 36;
        for (int i = 0; i < ringParticles; i++) {
            double angle = (2.0 * Math.PI * i) / ringParticles;
            double dx = Math.cos(angle) * 4.0;
            double dz = Math.sin(angle) * 4.0;
            sw.spawnParticles(ParticleTypes.WAX_ON, center.x + dx, center.y, center.z + dz, 2, 0.1, 0.1, 0.1, 0.02);
            sw.spawnParticles(ParticleTypes.GLOW, center.x + dx * 0.7, center.y, center.z + dz * 0.7, 1, 0, 0, 0, 0);
            sw.spawnParticles(ParticleTypes.END_ROD, center.x + dx * 0.5, center.y, center.z + dz * 0.5, 1, 0.02, 0.02, 0.02, 0.05);
            sw.spawnParticles(ParticleTypes.ENCHANT, center.x + dx * 0.3, center.y, center.z + dz * 0.3, 3, 0.2, 0.2, 0.2, 0.1);
        }

        // 3. Determine primary target vs secondary target(s)
        PlayerEntity primary = (this.target instanceof PlayerEntity pt && closePlayers.contains(pt)) ? pt : closePlayers.get(0);
        DamageSource magicSource = this.methode.getDamageSources().magic();

        for (PlayerEntity p : closePlayers) {
            // Shield break for anyone blocking close to Methode
            if (p.isBlocking()) {
                p.getItemCooldownManager().set(p.getActiveItem(), 60);
                p.clearActiveItem();
                sw.sendEntityStatus(p, (byte) 30);
                sw.playSound(null, p.getX(), p.getY(), p.getZ(),
                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.2F, 1.0F);
            }

            p.damage(sw, magicSource, 10.0F);

            if (p.equals(primary)) {
                // Primary attacker: Repelled backwards with powerful magic wave
                Vec3d toP = p.getPos().subtract(this.methode.getPos());
                Vec3d horizPush = new Vec3d(toP.x, 0, toP.z);
                if (horizPush.lengthSquared() > 1e-4) {
                    horizPush = horizPush.normalize();
                } else {
                    horizPush = this.methode.getRotationVec(1.0F).normalize();
                }

                p.takeKnockback(2.2, -horizPush.x, -horizPush.z);
                p.velocityModified = true;
                sw.spawnParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0, 0, 0, 0);
            } else {
                // Secondary attacker: Bound with Golden Restraint Ribbons (2.25s / 45 ticks)
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 45, 6, false, false, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 45, 2, false, false, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 45, 128, false, false, false)); // Disables jumping in MC
                p.setVelocity(0, 0, 0);
                p.velocityModified = true;

                // Visual binding ribbons coiled around the player
                for (int b = 0; b < 24; b++) {
                    double by = p.getY() + (b * 0.08);
                    double angle = (b * Math.PI) / 3.0;
                    double bx = p.getX() + Math.cos(angle) * 0.6;
                    double bz = p.getZ() + Math.sin(angle) * 0.6;
                    sw.spawnParticles(ParticleTypes.WAX_ON, bx, by, bz, 1, 0, 0, 0, 0);
                    sw.spawnParticles(ParticleTypes.GLOW, bx, by, bz, 1, 0, 0, 0, 0);
                }

                sw.playSound(null, p.getX(), p.getY(), p.getZ(),
                        SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 1.4F, 1.2F);
                sw.playSound(null, p.getX(), p.getY(), p.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.6F, 1.4F);
            }
        }
    }

    @Override
    protected void onAbilityStarted(BossAbility ability) {
        this.methode.triggerAttackAnim();
    }
}
