package com.yuno.yunosbosses.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class DomainFloorBlock extends Block {

    public DomainFloorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        // Particles must only be spawned on the client side
        if (world.isClient) {

            // Only spawn particles if the entity is actually moving, so they don't endlessly splash while standing still
            if (entity.getVelocity().horizontalLengthSquared() > 0.001) {
                Random random = world.getRandom();

                // Add a random chance so it doesn't spawn 20 particles every single second (looks more natural)
                if (random.nextInt(3) == 0) { // 33% chance per tick while moving

                    // Randomize the particle position slightly around the entity's feet
                    double x = entity.getX() + (random.nextDouble() - 0.5) * entity.getWidth();
                    double y = entity.getY(); // Right at foot level
                    double z = entity.getZ() + (random.nextDouble() - 0.5) * entity.getWidth();

                    // You can change ParticleTypes.SPLASH to ParticleTypes.FISHING if you want smaller water droplets
                    world.addParticle(ParticleTypes.SPLASH, x, y, z, 0.0, 0.0, 0.0);
                }
            }
        }

        super.onSteppedOn(world, pos, state, entity);
    }
}
