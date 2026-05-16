package com.yuno.yunosbosses.util;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpherePhysics {

    public static void apply(World world, ActiveBarrier barrier, double radius) {
        Vec3d center = barrier.getPosition();
        // Expand box to cover the whole sphere
        Box searchBox = new Box(center.subtract(radius + 1, radius + 1, radius + 1),
                center.add(radius + 1, radius + 1, radius + 1));

        world.getOtherEntities(null, searchBox).forEach(entity -> {
            double dist = entity.getPos().distanceTo(center);

            // --- DOMAIN EFFECT ---
            if (dist < radius && !entity.getUuid().equals(barrier.getOwnerUuid())) {
                barrier.getDomainEffect().accept(entity);
            }

            // If entity is crossing the boundary (adjust 0.4 for thickness/buffer)
            if (Math.abs(dist - radius) < 0.5) {
                Vec3d pushDir = entity.getPos().subtract(center).normalize();

                // Stop entities from entering or leaving
                if (dist < radius) {
                    // Inside: Push back towards center
                    entity.setVelocity(pushDir.multiply(-0.4));
                } else {
                    // Outside: Push away from center
                    entity.setVelocity(pushDir.multiply(0.4));
                }
                entity.velocityModified = true;
            }
        });
    }
}
