package cn.chumenta.crosswind.instrument;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class InstrumentShapes {
    private InstrumentShapes() {
    }

    public static final VoxelShape ROD = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    public static final VoxelShape WIND_VANE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 14.0D, 10.0D);

    public static final VoxelShape ANEMOMETER = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 14.0D, 10.0D);

    public static final VoxelShape WEATHER_STATION =
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 19.0D, 10.0D);

    public static boolean hasSupportBelow(final BlockGetter level, final BlockPos pos) {
        final BlockPos below = pos.relative(Direction.DOWN);
        final BlockState state = level.getBlockState(below);

        return !state.isAir() && state.getFluidState().isEmpty();
    }
}
