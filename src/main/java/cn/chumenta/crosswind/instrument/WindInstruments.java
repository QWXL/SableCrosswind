package cn.chumenta.crosswind.instrument;

import cn.chumenta.crosswind.registry.CrosswindRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public final class WindInstruments {
    private WindInstruments() {
    }

    public static class WindVaneBlockEntity extends AbstractWindInstrumentBlockEntity {
        public WindVaneBlockEntity(final BlockPos pos, final BlockState state) {
            super(CrosswindRegistry.WIND_VANE_BE.get(), pos, state);
        }

        @Override
        protected net.minecraft.network.chat.Component goggleLabel() {
            return net.minecraft.network.chat.Component.translatable("block.crosswind.wind_vane");
        }

        @Override
        public WindReadout.Fields provides() {
            return WindReadout.Fields.DIRECTIONS_ONLY;
        }
    }

    public static class AnemometerBlockEntity extends AbstractWindInstrumentBlockEntity {
        public AnemometerBlockEntity(final BlockPos pos, final BlockState state) {
            super(CrosswindRegistry.ANEMOMETER_BE.get(), pos, state);
        }

        @Override
        protected net.minecraft.network.chat.Component goggleLabel() {
            return net.minecraft.network.chat.Component.translatable("block.crosswind.anemometer");
        }

        @Override
        public WindReadout.Fields provides() {
            return WindReadout.Fields.SPEED_ONLY;
        }

        @Override
        protected void onServerTick(final Level level, final BlockPos pos, final BlockState state) {
            if (this.isRemoved()) {
                return;
            }
            final double limit = this.breakSpeedMps();

            if (limit > 0.0D && this.readout().cupSpeedMps() >= limit) {
                if (level instanceof final ServerLevel serverLevel) {
                    final double[] world = samplePositionFor(level, pos);
                    cn.chumenta.crosswind.advancement.CrosswindTriggers.onAnemometerDestroyed(
                            serverLevel, world[0], world[1], world[2]);
                }

                level.destroyBlock(pos, true);
            }
        }
    }

    public static class WeatherStationBlockEntity extends AbstractWindInstrumentBlockEntity {
        public WeatherStationBlockEntity(final BlockPos pos, final BlockState state) {
            super(CrosswindRegistry.WEATHER_STATION_BE.get(), pos, state);
        }

        @Override
        protected net.minecraft.network.chat.Component goggleLabel() {
            return net.minecraft.network.chat.Component.translatable("block.crosswind.weather_station");
        }

        @Override
        public WindReadout.Fields provides() {
            return WindReadout.Fields.ALL;
        }

        @Override
        protected double readoutWindowSeconds() {
            return 1.0D;
        }

        @Override
        protected double breakSpeedMps() {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static class RodBlock extends InstrumentBlock {
        public RodBlock(final Properties props, final net.minecraft.world.phys.shapes.VoxelShape shape) {
            super(props, shape, false);
        }

        @Override
        protected int poleDirectionMask() {
            return 3;
        }
    }

    public static class PoleInstrumentBlock extends InstrumentBlock {
        public PoleInstrumentBlock(final Properties props,
                                   final net.minecraft.world.phys.shapes.VoxelShape shape) {
            this(props, shape, false);
        }

        public PoleInstrumentBlock(final Properties props,
                                   final net.minecraft.world.phys.shapes.VoxelShape shape,
                                   final boolean needsSupport) {
            super(props, shape, needsSupport);
        }

        @Override
        protected int poleDirectionMask() {
            return 2;
        }
    }

    public static class InstrumentBlock extends Block implements EntityBlock {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty POLE_ABOVE =
                net.minecraft.world.level.block.state.properties.BooleanProperty.create("pole_above");

        public static final net.minecraft.world.level.block.state.properties.BooleanProperty POLE_BELOW =
                net.minecraft.world.level.block.state.properties.BooleanProperty.create("pole_below");

        public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction.Axis> AXIS =
                net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;

        protected int poleDirectionMask() {
            return 0;
        }

        protected boolean poleConnects() {
            return this.poleDirectionMask() != 0;
        }

        public final int poleDirectionMaskPublic() {
            return this.poleDirectionMask();
        }

        private boolean tracksAbove() {
            return (this.poleDirectionMask() & 1) != 0;
        }

        private boolean tracksBelow() {
            return (this.poleDirectionMask() & 2) != 0;
        }

        private final net.minecraft.world.phys.shapes.VoxelShape shape;

        private final boolean needsSupport;

        public InstrumentBlock(final Properties props,
                               final net.minecraft.world.phys.shapes.VoxelShape shape,
                               final boolean needsSupport) {
            super(props);
            this.shape = shape;
            this.needsSupport = needsSupport;
        }

        @Override
        protected void createBlockStateDefinition(
                final net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);

            if (this.tracksAbove()) {
                builder.add(POLE_ABOVE);
            }
            if (this.tracksBelow()) {
                builder.add(POLE_BELOW);
            }

            if (this.poleConnects()) {
                builder.add(AXIS);
            }
        }

        public static boolean hasPole(final BlockState state) {
            return state.getBlock() instanceof final InstrumentBlock ib && ib.poleConnects();
        }

        @Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(
                final net.minecraft.world.item.ItemStack stack,
                final BlockState state,
                final Level level,
                final BlockPos pos,
                final net.minecraft.world.entity.player.Player player,
                final net.minecraft.world.InteractionHand hand,
                final net.minecraft.world.phys.BlockHitResult hitResult) {
            if (player.isShiftKeyDown() || !player.mayBuild()) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            final net.createmod.catnip.placement.IPlacementHelper helper =
                    net.createmod.catnip.placement.PlacementHelpers.get(PolePlacementHelper.HELPER_ID);
            if (!helper.matchesItem(stack)) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (net.minecraft.world.item.BlockItem) stack.getItem(),
                            player, hand, hitResult);
        }

        public static boolean hasPoleAt(final BlockGetter level, final BlockPos pos) {
            return hasPole(level.getBlockState(pos));
        }

        private BlockState refreshPoleState(final BlockState state, final BlockGetter level,
                                            final BlockPos pos) {
            if (!this.poleConnects()) {
                return state;
            }
            BlockState out = state;
            if (this.tracksAbove()) {
                out = out.setValue(POLE_ABOVE, hasPoleAt(level, pos.above()));
            }
            if (this.tracksBelow()) {
                out = out.setValue(POLE_BELOW, hasPoleAt(level, pos.below()));
            }
            if (this.poleConnects()) {
                out = out.setValue(AXIS, Direction.Axis.Y);
            }
            return out;
        }

        @Override
        public void onPlace(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState oldState, final boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (!level.isClientSide() && this.poleConnects()) {
                level.setBlock(pos, refreshPoleState(state, level, pos), 3);
                refreshNeighbour(level, pos.above());
                refreshNeighbour(level, pos.below());
            }
        }

        private static void refreshNeighbour(final Level level, final BlockPos pos) {
            final BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof final InstrumentBlock ib && ib.poleConnects()) {
                final BlockState ns = ib.refreshPoleState(s, level, pos);
                if (ns != s) {
                    level.setBlock(pos, ns, 3);
                }
            }
        }

        @Override
        public net.minecraft.world.phys.shapes.VoxelShape getShape(
                final BlockState state, final BlockGetter level, final BlockPos pos,
                final CollisionContext context) {
            return this.shape;
        }

        @Override
        public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
            return !this.needsSupport || InstrumentShapes.hasSupportBelow(level, pos);
        }

        @Override
        public BlockState updateShape(final BlockState state, final Direction direction,
                                      final BlockState neighborState, final LevelAccessor level,
                                      final BlockPos pos, final BlockPos neighborPos) {
            if (this.needsSupport && direction == Direction.DOWN
                    && !state.canSurvive(level, pos)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }

            if (this.poleConnects() && direction.getAxis().isVertical()) {
                BlockState ns = state;
                if (this.tracksAbove()) {
                    ns = ns.setValue(POLE_ABOVE, hasPoleAt(level, pos.above()));
                }
                if (this.tracksBelow()) {
                    ns = ns.setValue(POLE_BELOW, hasPoleAt(level, pos.below()));
                }
                if (ns != state) {
                    if (level instanceof final Level lvl) {
                        refreshNeighbour(lvl, pos.above());
                        refreshNeighbour(lvl, pos.below());
                    }
                    return ns;
                }
            }
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            final BlockEntityType<?> type = state.getBlockHolder()
                    .unwrapKey()
                    .map(k -> net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE
                            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    k.location().getNamespace(), k.location().getPath())))
                    .orElse(null);
            if (type == null) {
                return null;
            }
            if (type == CrosswindRegistry.WIND_VANE_BE.get()) {
                return new WindVaneBlockEntity(pos, state);
            }
            if (type == CrosswindRegistry.ANEMOMETER_BE.get()) {
                return new AnemometerBlockEntity(pos, state);
            }
            if (type == CrosswindRegistry.WEATHER_STATION_BE.get()) {
                return new WeatherStationBlockEntity(pos, state);
            }
            return null;
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                     final BlockEntityType<T> type) {
            if (level.isClientSide()) {
                return null;
            }
            return (lvl, pos, st, be) -> {
                if (be instanceof final AbstractWindInstrumentBlockEntity instrument) {
                    AbstractWindInstrumentBlockEntity.serverTick(lvl, pos, st, instrument);
                }
            };
        }
    }
}
