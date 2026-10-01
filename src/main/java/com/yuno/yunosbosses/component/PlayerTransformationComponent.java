package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.util.BlackFlash;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import com.yuno.yunosbosses.util.HitstopData;
import com.yuno.yunosbosses.util.WallSlamData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;

public class PlayerTransformationComponent implements TransformationComponent {
    private final PlayerEntity player;
    private boolean transformed = false;

    private int blackFlashChain = 0;
    private boolean inTheZone = false;
    private int zoneTimer = 0;

    public PlayerTransformationComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean isTransformed() { return this.transformed; }

    @Override
    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
        // Syncs the change to all players nearby so they see the model change
        ModEntityComponents.TRANSFORMATION_DATA.sync(player);

        // Drop items upon transformation
        if (transformed && !this.player.getWorld().isClient()) {

            // Forcefully drop Chestplate
            ItemStack chest = this.player.getEquippedStack(EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                this.player.dropItem(chest, true, false);
                this.player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
            // Forcefully drop Helmet
            ItemStack head = this.player.getEquippedStack(EquipmentSlot.HEAD);
            if (!head.isEmpty()) {
                this.player.dropItem(head, true, false);
                this.player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            // Drop absolutely everything in the standard inventory/hotbar
            this.player.getInventory().dropAll();
        }
    }

    @Override
    public boolean isInTheZone() {
        return this.inTheZone;
    }

    @Override
    public void setInTheZone(boolean inTheZone, int ticks) {
        this.inTheZone = inTheZone;
        this.zoneTimer = ticks;
    }

    @Override
    public int getBlackFlashChain() {
        return this.blackFlashChain;
    }

    @Override
    public void setBlackFlashChain(int chain) {
        this.blackFlashChain = chain;
    }

    @Override
    public void resetBlackFlashChain() {
        this.blackFlashChain = 0;
    }

    @Override
    public void serverTick() {
        // Count down "The Zone" timer
        if (this.inTheZone) {
            this.zoneTimer--;
            if (this.zoneTimer <= 0) {
                this.inTheZone = false;
                this.blackFlashChain = 0;
            }
        }
    }

    @Override
    public void kick() {
        if (player == null) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        Vec3d lookDir = player.getRotationVector();
        Vec3d offset = lookDir.multiply(1.6);
        Box hitbox = player.getBoundingBox().offset(offset).expand(1.1);

        double effectX = player.getX() + offset.x;
        double effectY = player.getY() + 1.0;
        double effectZ = player.getZ() + offset.z;
        Vec3d center = new Vec3d(effectX, effectY, effectZ);

        int selectedSlot = player.getInventory().getSelectedSlot();

        // Big sweep curve particle for physical kick across all variants
        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, effectX, effectY, effectZ, 1, 0.0, 0.0, 0.0, 0.0);

        // --- VARIANT SELECTION (Slot 0 = Normal / Slot 1 = Blue / Slot 2 = Red) ---
        if (selectedSlot == 1) {
            performBlueKick(serverWorld, center, lookDir, hitbox);
        } else if (selectedSlot == 2) {
            performRedKick(serverWorld, center, lookDir, hitbox);
        } else {
            performNormalKick(serverWorld, center, lookDir, hitbox);
        }
    }

    /**
     * NORMAL KICK VARIANT (Slot 0):
     * Heavy physical kick with wind-up, blunt bone impact sound, dust clouds,
     * and a chance to land Black Flash (scales when in The Zone).
     */
    private void performNormalKick(ServerWorld world, Vec3d center, Vec3d lookDir, Box hitbox) {
        // Audio: Heavy sweep and punch whoosh
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 0.75f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 0.9f);

        // Visuals: Impact dust and small shock particles
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, player.getY() + 0.1, center.z,
                7, 0.35, 0.05, 0.35, 0.02);
        world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                6, 0.3, 0.3, 0.3, 0.1);

        List<LivingEntity> targets = world.getOtherEntities(player, hitbox, e -> e instanceof LivingEntity && !e.isSpectator())
                .stream().map(e -> (LivingEntity) e).toList();

        for (LivingEntity target : targets) {
            // Check Black Flash chance first
            boolean landedBlackFlash = BlackFlash.blackFlashChance(player, target, 0.05f);

            if (!landedBlackFlash) {
                // Regular hit
                target.damage(world, player.getDamageSources().playerAttack(player), 12.0f);
                Vec3d knockback = lookDir.normalize().multiply(0.85).add(0, 0.22, 0);
                target.setVelocity(knockback);
                target.velocityModified = true;

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.1f, 0.85f);
            }
        }
    }

    /**
     * LAPSE: BLUE KICK VARIANT (Slot 1):
     * Mechanics:
     * 1. Pulls all surrounding enemies inward toward the vortex point with accelerating gravity.
     * 2. Player is pulled rapidly into the vortex as high-speed dash step.
     * 3. Implosion crush damage on enemies caught in the center, applying hitstop and slowness.
     * 4. Multi-stage spiral visuals, electric sparks, and custom animated LAPSE_BLUE particles.
     */
    private void performBlueKick(ServerWorld world, Vec3d center, Vec3d lookDir, Box baseHitbox) {
        Box vortexBox = Box.of(center, 9.0, 7.0, 9.0);

        // Audio: High-frequency vacuum suction sound & charged distortion
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.8f, 1.75f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 1.6f, 1.25f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.4f, 1.9f);

        // Visuals Stage 1: Spawn animated LAPSE_BLUE particles at the vortex core
        world.spawnParticles(ModParticles.LAPSE_BLUE_PARTICLE, center.x, center.y, center.z,
                3, 0.15, 0.15, 0.15, 0.01);

        // Visuals Stage 2: Inward suction spiral particles (cyan dust & breeze wind)
        DustParticleEffect blueDust = new DustParticleEffect(0x00A6FF, 1.6f);
        DustParticleEffect deepBlueDust = new DustParticleEffect(0x0044FF, 2.0f);
        int spiralPoints = 28;
        for (int i = 0; i < spiralPoints; i++) {
            double angle = (i / (double) spiralPoints) * Math.PI * 4;
            double radius = 1.0 + (i / (double) spiralPoints) * 3.5;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            double py = center.y + ((world.random.nextDouble() - 0.5) * 1.2);

            // Inward velocity towards center
            Vec3d toCenter = center.subtract(px, py, pz).normalize().multiply(0.45);
            world.spawnParticles(blueDust, px, py, pz, 1, toCenter.x, toCenter.y, toCenter.z, 0.4);
            if (i % 2 == 0) {
                world.spawnParticles(deepBlueDust, px, py, pz, 1, toCenter.x, toCenter.y, toCenter.z, 0.4);
            }
            if (i % 4 == 0) {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, toCenter.x * 0.5, toCenter.y * 0.5, toCenter.z * 0.5, 0.15);
            }
        }
        world.spawnParticles(ParticleTypes.SMALL_GUST, center.x, center.y, center.z, 12, 0.8, 0.5, 0.8, 0.15);

        // Player high-speed step into the kick point (teleport/glide feel)
        Vec3d dashVec = center.subtract(player.getPos()).multiply(0.38);
        player.addVelocity(dashVec.x, Math.max(0.08, dashVec.y * 0.3), dashVec.z);
        player.velocityModified = true;

        // Pull nearby targets inward into the vortex
        List<LivingEntity> pulledEntities = world.getOtherEntities(player, vortexBox, e -> e instanceof LivingEntity && !e.isSpectator())
                .stream().map(e -> (LivingEntity) e).toList();

        for (LivingEntity target : pulledEntities) {
            Vec3d toCenter = center.subtract(target.getPos());
            double dist = toCenter.length();
            if (dist > 0.1) {
                Vec3d pullDir = toCenter.normalize();
                double strength = Math.min(1.8, Math.max(0.65, dist * 0.35));
                target.setVelocity(pullDir.multiply(strength).add(0, 0.25, 0));
                target.velocityModified = true;
            }
        }

        // Delayed Implosion Crush (3 ticks later when enemies converge into center)
        DelayedServerEffects.delay(3, () -> {
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.4f, 1.8f);
            world.playSound(null, center.x, center.y, center.z,
                    ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.2f, 1.7f);

            // Implosion shockwave particles
            world.spawnParticles(ModParticles.LAPSE_BLUE_PARTICLE, center.x, center.y, center.z, 4, 0.2, 0.2, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 20, 0.5, 0.5, 0.5, 0.25);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 15, 0.6, 0.6, 0.6, 0.2);

            Box crushBox = Box.of(center, 4.0, 3.5, 4.0);
            List<LivingEntity> crushed = world.getOtherEntities(player, crushBox, e -> e instanceof LivingEntity && !e.isSpectator())
                    .stream().map(e -> (LivingEntity) e).toList();

            for (LivingEntity target : crushed) {
                target.damage(world, player.getDamageSources().playerAttack(player), 15.0f);

                // Hitstop micro-pause: locks them in the vacuum impact for 3 ticks
                if (target instanceof HitstopData hitstop) {
                    hitstop.yunos$setHitstopTicks(3);
                    target.setVelocity(Vec3d.ZERO);
                    target.velocityModified = true;
                }
            }
        });
    }

    /**
     * REVERSAL: RED KICK VARIANT (Slot 2):
     * Mechanics:
     * 1. 1-tick charged compression spark at the kick point.
     * 2. Violent explosive repulsion: targets in front are blasted backward with massive knockback.
     * 3. Activates Wall Slam collision damage on targets so slamming into terrain causes bonus damage.
     * 4. Multi-layered sonic explosion, red ember shockwave, and animated REVERSAL_RED particles.
     */
    private void performRedKick(ServerWorld world, Vec3d center, Vec3d lookDir, Box hitbox) {
        Vec3d blastDir = lookDir.normalize();

        // Audio: Pre-ignition spark + heavy detonating blast
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.8f, 1.4f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 1.6f, 1.6f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.2f, 1.8f);

        // Visuals Stage 1: Animated REVERSAL_RED particles centered at kick point
        world.spawnParticles(ModParticles.REVERSAL_RED_PARTICLE, center.x, center.y, center.z,
                3, 0.1, 0.1, 0.1, 0.0);

        // Visuals Stage 2: Repulsive shockwave cone & fiery crimson blast
        world.spawnParticles(ModParticles.FLAME_SHOCKWAVE_PARTICLE, center.x, center.y, center.z,
                1, blastDir.x * 0.5, blastDir.y * 0.5, blastDir.z * 0.5, 0.0);
        world.spawnParticles(ModParticles.FLAME_EMBER_PARTICLE, center.x, center.y, center.z,
                14, 0.4, 0.4, 0.4, 0.18);

        // Crimson and bright red divergence particles
        DustParticleEffect brightRed = new DustParticleEffect(0xFF0033, 2.2f);
        DustParticleEffect deepCrimson = new DustParticleEffect(0xAA0011, 2.0f);

        for (int i = 0; i < 20; i++) {
            double spreadX = blastDir.x + (world.random.nextDouble() - 0.5) * 0.7;
            double spreadY = blastDir.y + (world.random.nextDouble() - 0.5) * 0.5;
            double spreadZ = blastDir.z + (world.random.nextDouble() - 0.5) * 0.7;
            double speed = 0.8 + world.random.nextDouble() * 1.4;
            world.spawnParticles(brightRed, center.x, center.y, center.z, 1,
                    spreadX * speed, spreadY * speed, spreadZ * speed, speed);
            if (i % 2 == 0) {
                world.spawnParticles(deepCrimson, center.x, center.y, center.z, 1,
                        spreadX * speed * 0.7, spreadY * speed * 0.7, spreadZ * speed * 0.7, speed * 0.7);
            }
        }
        world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 12, 0.3, 0.3, 0.3, 0.25);

        // Forward cone blast box for hit detection (widens and extends forward)
        Box redBlastBox = hitbox.expand(1.2, 0.8, 1.2).offset(blastDir.multiply(0.8));
        List<LivingEntity> targets = world.getOtherEntities(player, redBlastBox, e -> e instanceof LivingEntity && !e.isSpectator())
                .stream().map(e -> (LivingEntity) e).toList();

        for (LivingEntity target : targets) {
            // Heavy direct damage
            target.damage(world, player.getDamageSources().playerAttack(player), 18.0f);

            // Calculate directional push vector
            Vec3d toTarget = target.getPos().subtract(player.getPos());
            Vec3d pushDir = toTarget.lengthSquared() > 0.01 ? toTarget.normalize() : blastDir;

            double pushStrength = 3.6;
            double verticalLift = 0.36;

            target.setVelocity(pushDir.x * pushStrength, verticalLift, pushDir.z * pushStrength);
            target.velocityModified = true;

            // Activate Wall Slam: if they slam into blocks, deal massive bonus damage
            if (target instanceof WallSlamData wallSlam) {
                wallSlam.yunos$setWallSlamTimer(35);
            }

            // Target impact visual
            world.spawnParticles(ModParticles.REVERSAL_RED_PARTICLE,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void readData(ReadView tag) {
        this.transformed = tag.getBoolean("IsTransformed", false);
    }

    @Override
    public void writeData(WriteView tag) {
        tag.putBoolean("IsTransformed", this.transformed);
    }
}
