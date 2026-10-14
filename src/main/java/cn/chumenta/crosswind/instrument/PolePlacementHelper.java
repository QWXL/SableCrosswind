package cn.chumenta.crosswind.instrument;

import com.simibubi.create.foundation.placement.PoleHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Predicate;

public final class PolePlacementHelper extends PoleHelper<Direction.Axis> {
    public static final int HELPER_ID = PlacementHelpers.register(new PolePlacementHelper());

    private PolePlacementHelper() {
        super(WindInstruments.InstrumentBlock::hasPole,
                state -> Direction.Axis.Y,
                WindInstruments.InstrumentBlock.AXIS);
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.getItem() instanceof final BlockItem bi
                && bi.getBlock() instanceof final WindInstruments.InstrumentBlock ib
                && ib.poleDirectionMaskPublic() != 0;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return WindInstruments.InstrumentBlock::hasPole;
    }

    @Override
    public PlacementOffset getOffset(final Player player, final Level world, final BlockState state,
                                     final BlockPos pos, final BlockHitResult ray) {
        if (!(state.getBlock() instanceof final WindInstruments.InstrumentBlock ib)) {
            return PlacementOffset.fail();
        }
        final int mask = ib.poleDirectionMaskPublic();
        if (mask == 0) {
            return PlacementOffset.fail();
        }

        for (final Direction dir : net.createmod.catnip.placement.IPlacementHelper
                .orderedByDistance(pos, ray.getLocation(), d -> d.getAxis() == Direction.Axis.Y)) {
            final int step = dir == Direction.UP ? 1 : -1;
            final int targetY = PolePlacement.targetY(pos.getY(), step,
                    y -> WindInstruments.InstrumentBlock.hasPoleAt(
                            world, new BlockPos(pos.getX(), y, pos.getZ())),
                    mask);
            if (targetY == PolePlacement.NO_PLACEMENT) {
                continue;
            }
            return PlacementOffset.success(new BlockPos(pos.getX(), targetY, pos.getZ()));
        }
        return PlacementOffset.fail();
    }
}
