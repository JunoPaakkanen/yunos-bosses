package com.yuno.yunosbosses.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DomainFloorVoidBlock extends DomainFloorBlock {

    public DomainFloorVoidBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world.isClient) {
            world.addParticleClient(
                    ParticleTypes.REVERSE_PORTAL,
                    entity.getX() + (world.random.nextDouble() - 0.5) * 0.4,
                    entity.getY() + 0.05,
                    entity.getZ() + (world.random.nextDouble() - 0.5) * 0.4,
                    (world.random.nextDouble() - 0.5) * 0.02,
                    0.05 + world.random.nextDouble() * 0.05,
                    (world.random.nextDouble() - 0.5) * 0.02
            );
        }
    }
}
