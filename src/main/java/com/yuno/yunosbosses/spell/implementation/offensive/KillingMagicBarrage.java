package com.yuno.yunosbosses.spell.implementation.offensive;

import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class KillingMagicBarrage extends KillingMagic {
    private static final int BEAM_COUNT = 10;
    private static final int DELAY_BETWEEN_BEAMS = 2; // Every 2 ticks a new beam fires

    public KillingMagicBarrage(Identifier id, SpellRarity rarity) {
        super(id, rarity);
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 35.0F;
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        this.cast(world, caster, staff, 1);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient) return;

        float damageMultiplier = 1.0F;
        if (staff.getItem() instanceof StaffItem staffItem) {
            damageMultiplier = staffItem.getPowerMultiplier();
        }
        final float trueDamage = 14.0F * damageMultiplier;

        // Visual hand swing
        caster.swingHand(Hand.MAIN_HAND, true);

        // Rapid machine-gun killing magic stream: 10 beams fired in rapid succession
        for (int i = 0; i < BEAM_COUNT; i++) {
            final int shotIndex = i;
            Runnable shotAction = () -> {
                if (!caster.isAlive()) return;

                Vec3d look = caster.getRotationVector().normalize();
                Vec3d tempUp = Math.abs(look.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
                Vec3d right = look.crossProduct(tempUp).normalize();
                Vec3d up = right.crossProduct(look).normalize();

                // Subtle lateral barrel jitter (beams shoot out in a tight rapid stream)
                double jitterX = (world.random.nextDouble() - 0.5) * 0.28;
                double jitterY = (world.random.nextDouble() - 0.5) * 0.28;
                Vec3d start = caster.getEyePos()
                        .add(look.multiply(0.85))
                        .add(right.multiply(jitterX))
                        .add(up.multiply(jitterY));

                // Target acquisition: track actual entity target if present, otherwise shoot toward current crosshair
                LivingEntity target = caster.getAttacking();
                if ((target == null || !target.isAlive()) && caster instanceof MobEntity mob) {
                    target = mob.getTarget();
                }

                Vec3d direction;
                if (target != null && target.isAlive()) {
                    Vec3d targetAim = target.getBoundingBox().getCenter();
                    Vec3d spread = right.multiply((world.random.nextDouble() - 0.5) * 0.15)
                            .add(up.multiply((world.random.nextDouble() - 0.5) * 0.15));
                    direction = targetAim.add(spread).subtract(start).normalize();
                } else {
                    Vec3d spread = right.multiply((world.random.nextDouble() - 0.5) * 0.04)
                            .add(up.multiply((world.random.nextDouble() - 0.5) * 0.04));
                    direction = look.add(spread).normalize();
                }

                // Rapid-fire audio cues per shot: crisp, high-frequency laser zaps
                world.playSound(null, start.x, start.y, start.z,
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.85F, 1.95F);
                world.playSound(null, start.x, start.y, start.z,
                        SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2F, 1.9F);
                world.playSound(null, start.x, start.y, start.z,
                        SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.9F, 1.8F);

                fireBeamTowardTarget(
                        world,
                        caster,
                        start,
                        direction,
                        32,       // maxRange
                        0,        // delay (0 ticks = instant beam creation per salvo shot)
                        6,        // durationTicks (fast 6-tick laser burst)
                        0.22F,    // beamRadius (thinner needle beam)
                        0.60F,    // damageRadius
                        trueDamage,
                        0.60F,    // tunnelRadius
                        1.1F,     // impactRadius
                        1.90F + (world.random.nextFloat() * 0.15F), // soundPitch
                        0.04F,    // knockbackHoriz (gentle suppression, keeps target in firing line)
                        0.0F      // knockbackVert (no vertical launch)
                );
            };

            if (shotIndex == 0) {
                shotAction.run(); // Immediate shot 0
            } else {
                DelayedServerEffects.delay(shotIndex * DELAY_BETWEEN_BEAMS, shotAction);
            }
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.killing_magic_barrage");
    }
}
