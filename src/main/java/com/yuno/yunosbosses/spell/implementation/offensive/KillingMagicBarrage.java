package com.yuno.yunosbosses.spell.implementation.offensive;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.yuno.yunosbosses.util.DelayedServerEffects;

public class KillingMagicBarrage extends KillingMagic {
    private static final int BEAM_COUNT = 5;
    private static final int DELAY_BETWEEN_BEAMS = 3;

    public KillingMagicBarrage(Identifier id) {
        super(id);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (!world.isClient) {
            // Calculate the convergence point where all beams will aim
            Vec3d playerLookVector = caster.getRotationVector();
            double convergenceDistance = 12.0; // Distance to the focal point
            Vec3d targetPoint = caster.getEyePos().add(playerLookVector.multiply(convergenceDistance));
            
            // Fire multiple beams with staggered delays
            for (int beamIndex = 0; beamIndex < BEAM_COUNT; beamIndex++) {
                int delayForThisBeam = beamIndex * DELAY_BETWEEN_BEAMS;
            
                DelayedServerEffects.delay(delayForThisBeam, () -> {
                    // Randomize the beam origin around the caster
                    double angleOffset = world.random.nextDouble() * Math.PI * 2;
                    double radiusOffset = 2.0 + world.random.nextDouble() * 1.5;
                
                    Vec3d offset = new Vec3d(
                        Math.cos(angleOffset) * radiusOffset,
                        world.random.nextDouble() * 2.0 - 1.0,
                        Math.sin(angleOffset) * radiusOffset
                    );
                
                    Vec3d start = caster.getEyePos().add(offset);
                    
                    // Calculate direction from start position to target point
                    Vec3d direction = targetPoint.subtract(start).normalize();
                
                    // Fire beam toward the convergence point
                    fireBeamTowardTarget(world, caster, start, direction, 12, 15, 1.0F, 0.6F, 15.0F);
                });
            }
        }
    }
}
