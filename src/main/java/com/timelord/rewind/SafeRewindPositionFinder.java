package com.timelord.rewind;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class SafeRewindPositionFinder {
    private SafeRewindPositionFinder() {}

    public static Optional<Vec3d> find(ServerWorld world, PlayerEntity player, Vec3d preferred) {
        if (isSafe(world, player, preferred, false))
            return Optional.of(preferred);

        for (int vertical = -2; vertical <= 3; vertical++) {
            for (int radius = 0; radius <= 3; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius)
                            continue;

                        Vec3d candidate = preferred.add(x, vertical, z);
                        if (isSafe(world, player, candidate, true))
                            return Optional.of(candidate);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isSafe(ServerWorld world, PlayerEntity player, Vec3d position, boolean requireGround) {
        BlockPos feet = BlockPos.ofFloored(position);

        if (!world.isChunkLoaded(feet) || !world.getWorldBorder().contains(feet))
            return false;

        if (feet.getY() < world.getBottomY() || feet.getY() + 2 >= world.getTopY())
            return false;

        Box box = player.getBoundingBox().offset(position.subtract(player.getPos()));
        if (!world.isSpaceEmpty(player, box))
            return false;

        if (requireGround && !world.getBlockState(feet.down()).isSolidBlock(world, feet.down()))
            return false;

        for (BlockPos checked : BlockPos.iterate(feet.down(), feet.up(2))) {
            BlockState state = world.getBlockState(checked);

            if (world.getFluidState(checked).isIn(FluidTags.LAVA)
                    || state.isIn(BlockTags.FIRE)
                    || state.isOf(Blocks.CACTUS)
                    || state.isOf(Blocks.MAGMA_BLOCK)
                    || state.isOf(Blocks.SWEET_BERRY_BUSH)
                    || state.isOf(Blocks.POWDER_SNOW))
                return false;
        }

        return true;
    }
}
