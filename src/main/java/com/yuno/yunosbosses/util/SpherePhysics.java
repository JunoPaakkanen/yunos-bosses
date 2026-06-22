package com.yuno.yunosbosses.util;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpherePhysics {

    public static void apply(World world, ActiveBarrier barrier, double radius) {
        Vec3d center = barrier.getPosition();
        double padding = 30.0;
        Box searchBox = new Box(center.subtract(radius + padding, radius + padding, radius + padding),
                center.add(radius + padding, radius + padding, radius + padding));

        world.getOtherEntities(null, searchBox).forEach(entity -> {
            if (entity.isSpectator()) return;

            Vec3d currPos = entity.getPos();
            double currDist = currPos.distanceTo(center);

            Vec3d prevPos = new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
            if (prevPos.equals(Vec3d.ZERO)) prevPos = currPos;
            double prevDist = prevPos.distanceTo(center);

            // Determine which side this entity belongs on based on where it was last tick
            boolean belongsInside = prevDist <= radius;
            boolean wrongSide = (belongsInside && currDist > radius)
                             || (!belongsInside && currDist <= radius);

            if (wrongSide) {
                Vec3d radialDir = currPos.subtract(center).normalize();

                // Snap back to the correct side with a firm offset
                double safeRadius = belongsInside ? radius - 0.5 : radius + 0.5;
                Vec3d snapPos = center.add(radialDir.multiply(safeRadius));

                Vec3d vel = entity.getVelocity();
                double radialComponent = vel.dotProduct(radialDir);
                entity.setVelocity(vel.subtract(radialDir.multiply(radialComponent)));
                entity.velocityModified = true;

                if (entity instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.networkHandler.requestTeleport(
                            snapPos.x, snapPos.y, snapPos.z,
                            serverPlayer.getYaw(), serverPlayer.getPitch()
                    );
                } else {
                    entity.setPosition(snapPos);
                }

                if (entity instanceof ProjectileEntity) {
                    entity.setVelocity(Vec3d.ZERO);
                }
            }
            // Soft repulsion zone: continuously push away from the wall when near it
            else if (Math.abs(currDist - radius) < 1.0) {
                Vec3d pushDir = currPos.subtract(center).normalize();
                double pushStrength = 0.4 * (1.0 - Math.abs(currDist - radius));

                if (currDist < radius) {
                    entity.setVelocity(entity.getVelocity().add(pushDir.multiply(-pushStrength)));
                } else {
                    entity.setVelocity(entity.getVelocity().add(pushDir.multiply(pushStrength)));
                }
                entity.velocityModified = true;
            }
        });
    }
}
