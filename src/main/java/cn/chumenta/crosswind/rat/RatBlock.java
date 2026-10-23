package cn.chumenta.crosswind.rat;

import cn.chumenta.crosswind.registry.CrosswindRegistry;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RatBlock extends HorizontalKineticBlock implements IBE<RatBlockEntity> {
    public static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 4.0D, 11.0D, 16.0D, 10.0D);

    public RatBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public Class<RatBlockEntity> getBlockEntityClass() {
        return RatBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RatBlockEntity> getBlockEntityType() {
        return CrosswindRegistry.RAT_BE.get();
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state,
                                   final Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockState above = level.getBlockState(pos.above());
        return !above.isAir() && above.getFluidState().isEmpty();
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction direction,
                                  final BlockState neighborState, final LevelAccessor level,
                                  final BlockPos pos, final BlockPos neighborPos) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
