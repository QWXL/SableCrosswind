package cn.chumenta.crosswind.instrument;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.CrosswindConfig;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractWindInstrumentBlockEntity extends BlockEntity
        implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    protected WindReadout readout = WindReadout.NONE;

    private WindReadout lastSynced = null;

    private long lastSyncTick = Long.MIN_VALUE;

    private cn.chumenta.crosswind.instrument.CupWindow cupWindow;

    protected AbstractWindInstrumentBlockEntity(final BlockEntityType<?> type,
                                                final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public static <T extends AbstractWindInstrumentBlockEntity> void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final T be) {
        if (level.isClientSide()) {
            return;
        }
        be.tickServer((ServerLevel) level, pos);
    }

    protected void tickServer(final ServerLevel level, final BlockPos pos) {
        final WindSettings settings = WindSettings.fromConfig();
        if (!settings.enabled()) {
            if (this.readout != WindReadout.NONE) {
                this.readout = WindReadout.NONE;
                this.markDirtyAndSync(level, pos);
            }

            this.onServerTick(level, pos, this.getBlockState());
            return;
        }

        final double cx;
        final double cy;
        final double cz;
        final double[] worldPos = blockWorldPosition(level, pos);
        cx = worldPos[0];
        cy = worldPos[1];
        cz = worldPos[2];
        final WindSample sample = Crosswind.windService().sample(level, cx, cy, cz, settings);

        final long tick = level.getGameTime();

        final float cupSpeed = (float) this.cupWindow().push(sample.speed());
        final WindReadout next = WindReadout.of(sample, cupSpeed);
        this.readout = next;

        final long interval = Math.max(1, CrosswindConfig.INSTRUMENT_SYNC_INTERVAL.get());
        final boolean due = this.lastSynced == null || tick - this.lastSyncTick >= interval;
        final boolean changed = this.lastSynced == null || next.differsFrom(this.lastSynced,
                CrosswindConfig.INSTRUMENT_ANGLE_TOLERANCE.get(),
                CrosswindConfig.INSTRUMENT_SPEED_TOLERANCE.get(),
                CrosswindConfig.INSTRUMENT_TEMP_TOLERANCE.get(),
                CrosswindConfig.INSTRUMENT_PRECIP_TOLERANCE.get());

        if (due || changed) {
            this.lastSyncTick = tick;
            this.lastSynced = next;
            this.sendReadout(level, pos, next);

            cn.chumenta.crosswind.advancement.CrosswindTriggers.onReadoutSynced(
                    level, cx, cy, cz, next, this.provides());
        }

        this.onServerTick(level, pos, this.getBlockState());
    }

    public static double[] samplePositionFor(final Level level, final BlockPos pos) {
        return blockWorldPosition(level, pos);
    }

    private static double[] blockWorldPosition(final Level level, final BlockPos pos) {
        final double cx = pos.getX() + 0.5D;
        final double cy = pos.getY() + 0.5D;
        final double cz = pos.getZ() + 0.5D;
        try {
            final var containing = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, pos);
            if (containing != null && !containing.isRemoved()) {
                final org.joml.Vector3d dest = new org.joml.Vector3d();
                containing.logicalPose().transformPosition(
                        new org.joml.Vector3d(cx, cy, cz), dest);
                return new double[]{dest.x, dest.y, dest.z};
            }
        } catch (final Throwable ignored) {
        }
        return new double[]{cx, cy, cz};
    }

    private cn.chumenta.crosswind.instrument.CupWindow cupWindow() {
        if (this.cupWindow == null) {
            final double seconds = Math.max(0.05D, this.readoutWindowSeconds());
            this.cupWindow = new cn.chumenta.crosswind.instrument.CupWindow(
                    Math.max(1, (int) Math.round(seconds * 20.0D)));
        }
        return this.cupWindow;
    }

    @Override
    public void setRemoved() {
        if (this.cupWindow != null) {
            this.cupWindow.clear();
        }

        this.vaneAngle.reset();
        this.cupAngle.reset();
        super.setRemoved();
    }

    protected double readoutWindowSeconds() {
        return cn.chumenta.crosswind.CrosswindConfig.INSTRUMENT_CUP_WINDOW_SECONDS.get();
    }

    protected double breakSpeedMps() {
        return cn.chumenta.crosswind.CrosswindConfig.INSTRUMENT_BREAK_SPEED_ANDESITE.get();
    }

    protected void onServerTick(final Level level, final BlockPos pos, final BlockState state) {
    }

    protected void sendReadout(final ServerLevel level, final BlockPos pos, final WindReadout value) {
        cn.chumenta.crosswind.network.PacketDistributorHelper.sendInstrumentReadout(level, pos, value);
    }

    private void markDirtyAndSync(final ServerLevel level, final BlockPos pos) {
        this.setChanged();
        this.sendReadout(level, pos, this.readout);
    }

    @org.jetbrains.annotations.Nullable
    public String lastReport() {
        return this.lastReport;
    }

    protected String lastReport;

    @Override
    public boolean addToGoggleTooltip(final java.util.List<net.minecraft.network.chat.Component> tooltip,
                                      final boolean isPlayerSneaking) {
        return InstrumentGoggleInfo.addTo(tooltip, this.readout, this.goggleLabel(), this.provides());
    }

    protected abstract net.minecraft.network.chat.Component goggleLabel();

    public abstract WindReadout.Fields provides();

    public static final class RenderAngle {
        private float angle;
        private boolean init;

        private long lastTick = Long.MIN_VALUE;

        public float spinBy(final float degreesPerSecond, final float partialTick,
                            final long gameTime) {
            if (!this.init) {
                this.init = true;
                this.lastTick = gameTime;
            }
            final long elapsed = Math.max(0L, gameTime - this.lastTick);
            this.lastTick = gameTime;

            this.angle += degreesPerSecond * (float) (elapsed * 0.05D);
            final float view = this.angle + degreesPerSecond * (float) (partialTick * 0.05D);
            return (float) cn.chumenta.crosswind.wind.WindMath.wrapDegrees(view);
        }

        public float advanceTowards(final float targetDegrees, final float maxStepPerTick,
                                    final float partialTick) {
            if (!this.init) {
                this.init = true;
                this.angle = targetDegrees;
            }
            this.angle = WindInstrumentAngles.approachForRender(
                    this.angle, targetDegrees, maxStepPerTick, partialTick);
            return this.angle;
        }

        public void reset() {
            this.init = false;
            this.lastTick = Long.MIN_VALUE;
        }
    }

    private final RenderAngle vaneAngle = new RenderAngle();

    private final RenderAngle cupAngle = new RenderAngle();

    public RenderAngle vaneAngle() {
        return this.vaneAngle;
    }

    public RenderAngle cupAngle() {
        return this.cupAngle;
    }

    public WindReadout readout() {
        return this.readout;
    }

    public void setReadoutFromServer(final WindReadout value) {
        this.readout = value;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putFloat("ToDeg", this.readout.toDegrees());
        tag.putFloat("FromDeg", this.readout.fromDegrees());
        tag.putFloat("Speed", this.readout.speedMps());
        tag.putFloat("Gust", this.readout.gustMps());

        tag.putFloat("CupSpeed", this.readout.cupSpeedMps());
        tag.putFloat("Temp", this.readout.temperatureC());
        tag.putFloat("Precip", this.readout.precipitation());
        tag.putBoolean("ClimateOk", this.readout.climateAvailable());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readout = new WindReadout(
                tag.getFloat("ToDeg"), tag.getFloat("FromDeg"),
                tag.getFloat("Speed"), tag.getFloat("Gust"),

                tag.getFloat("CupSpeed"),
                tag.getFloat("Temp"), tag.getFloat("Precip"),

                tag.getBoolean("ClimateOk"));
    }
}
