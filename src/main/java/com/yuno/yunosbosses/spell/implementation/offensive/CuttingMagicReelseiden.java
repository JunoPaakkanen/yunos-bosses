package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.entity.projectile.SlashProjectileEntity;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CuttingMagicReelseiden extends Spell {

    public CuttingMagicReelseiden(Identifier id, SpellRarity rarity) {
        super(id, rarity);
    }

    public float baseDamage = 11.0F;
    public float manaCost = 42.0F;

    private static class ComboTracker {
        int step;
        long lastCastTick;

        ComboTracker(int step, long lastCastTick) {
            this.step = step;
            this.lastCastTick = lastCastTick;
        }
    }

    private static final Map<UUID, ComboTracker> COMBOS = new ConcurrentHashMap<>();

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient) return;

        // Calculate power multiplier
        float multiplier = 1.0F;
        if (staff.getItem() instanceof StaffItem staffItem) {
            multiplier = staffItem.getPowerMultiplier();
        }

        Vec3d look = caster.getRotationVector();
        Vec3d eyePos = caster.getEyePos();
        ServerWorld serverWorld = (ServerWorld) world;

        // Swing hand
        caster.swingHand(Hand.MAIN_HAND, true);

        // --- SNEAK CAST: TACTICAL DUST SCREEN (Episode 24 Reference) ---
        if (caster.isSneaking()) {
            castTacticalScreen(serverWorld, caster, staff, look, eyePos, multiplier);
            return;
        }

        // --- NORMAL CAST: 3-HIT FLUID COMBO CHAIN ---
        long currentTick = world.getTime();
        ComboTracker tracker = COMBOS.get(caster.getUuid());
        int step = 0;

        if (tracker != null && (currentTick - tracker.lastCastTick) <= 24 && (currentTick - tracker.lastCastTick) >= 2) {
            step = (tracker.step + 1) % 3;
        }
        COMBOS.put(caster.getUuid(), new ComboTracker(step, currentTick));

        float rollAngle;
        float damage;
        float width;
        boolean isFinisher;
        int cooldownTicks;

        switch (step) {
            case 1 -> {
                // Step 1: Diagonal Right-to-Left slash (\)
                rollAngle = 35.0f;
                damage = this.baseDamage * multiplier;
                width = 1.35f;
                isFinisher = false;
                cooldownTicks = 5;

                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.4f, 1.75f);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.0f, 1.3f);
            }
            case 2 -> {
                // Step 2: Finisher Cross-Cleave / Heavy Execution (X)
                rollAngle = 0.0f;
                damage = (this.baseDamage * 1.55f) * multiplier;
                width = 1.85f;
                isFinisher = true;
                cooldownTicks = 10;

                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.2f, 1.9f);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.ITEM_SHEARS_SNIP, SoundCategory.PLAYERS, 1.4f, 1.3f);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.3f, 1.55f);

                // Muzzle burst of scissors sparks
                Vec3d muzzlePos = eyePos.add(look.multiply(0.8));
                serverWorld.spawnParticles(ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                        muzzlePos.x, muzzlePos.y, muzzlePos.z, 2, 0.1, 0.1, 0.1, 0.0);
            }
            default -> {
                // Diagonal Left-to-Right slash (/)
                rollAngle = -35.0f;
                damage = this.baseDamage * multiplier;
                width = 1.35f;
                isFinisher = false;
                cooldownTicks = 5;

                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.3f, 1.55f);
                world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.0f, 1.15f);
            }
        }

        // Spawn cutting blade projectile
        Vec3d spawnPos = eyePos.add(look.multiply(0.25));
        SlashProjectileEntity projectile = new SlashProjectileEntity(
                ModEntities.SLASH_PROJECTILE,
                world,
                damage,
                rollAngle,
                step,
                width,
                isFinisher
        );

        projectile.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        projectile.setVelocity(look.multiply(2.5)); // 2.5 blocks/tick for 2 ticks = strict 5m range
        projectile.setOwner(caster);
        world.spawnEntity(projectile);

        // Apply short item cooldown for fluid rhythm
        if (caster instanceof PlayerEntity player && !staff.isEmpty()) {
            player.getItemCooldownManager().set(staff, cooldownTicks);
        }
    }

    private void castTacticalScreen(ServerWorld serverWorld, LivingEntity caster, ItemStack staff,
                                   Vec3d look, Vec3d eyePos, float multiplier) {
        // Tactical Terrain Slash: Slices into floor/walls to kick up stone debris and dust
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.4f, 1.4f);
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.25f, 1.3f);
        serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.1f, 1.25f);

        // Spawn a low sweeping horizontal slash
        Vec3d spawnPos = eyePos.add(look.multiply(0.2));
        SlashProjectileEntity projectile = new SlashProjectileEntity(
                ModEntities.SLASH_PROJECTILE,
                serverWorld,
                (this.baseDamage * 0.9f) * multiplier,
                0.0f,
                0,
                1.6f,
                false
        );
        projectile.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        projectile.setVelocity(look.multiply(2.2));
        projectile.setOwner(caster);
        serverWorld.spawnEntity(projectile);

        // Tactical dust & debris eruption 2 blocks in front of caster
        Vec3d dustCenter = caster.getPos().add(look.x * 2.0, 0.1, look.z * 2.0);
        BlockPos groundPos = BlockPos.ofFloored(dustCenter);
        BlockState groundState = serverWorld.getBlockState(groundPos.down());
        if (groundState.isAir()) groundState = serverWorld.getBlockState(groundPos);

        if (!groundState.isAir()) {
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, groundState),
                    dustCenter.x, dustCenter.y + 0.3, dustCenter.z, 28, 1.0, 0.4, 1.0, 0.1);
        }
        serverWorld.spawnParticles(ParticleTypes.POOF,
                dustCenter.x, dustCenter.y + 0.4, dustCenter.z, 22, 1.1, 0.5, 1.1, 0.04);
        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                dustCenter.x, dustCenter.y + 0.2, dustCenter.z, 8, 0.8, 0.3, 0.8, 0.02);

        // Caster gains temporary concealment and agility to reposition
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20, 0, false, false, true));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 1, false, false, true));

        if (caster instanceof PlayerEntity player && !staff.isEmpty()) {
            player.getItemCooldownManager().set(staff, 12);
        }
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return manaCost;
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.cutting_magic_reelseiden");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }
}
