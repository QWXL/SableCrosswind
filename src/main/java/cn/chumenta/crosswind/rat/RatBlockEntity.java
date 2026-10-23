package cn.chumenta.crosswind.rat;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;

public class RatBlockEntity extends GeneratingKineticBlockEntity {
    private static final int AIRSPEED_SYNC_INTERVAL = 10;

    private final RatAirspeed.Window airspeedWindow = new RatAirspeed.Window(RatAirspeed.WINDOW_TICKS);

    private final Vector3d localCenter = new Vector3d();
    private final Vector3d centerWorld = new Vector3d();
    private final Vector3d localNormal = new Vector3d();
    private final Vector3d worldNormal = new Vector3d();
    private final Vector3d structureVelocity = new Vector3d();

    private final cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle
            fanAngle = new cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle();

    private final cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle
            shaftAngle = new cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle();

    private float generatedSpeed;

    private double airspeedMps;

    private long lastSyncTick = Long.MIN_VALUE;

    public RatBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public static RatBlockEntity create(final BlockPos pos, final BlockState state) {
        return new RatBlockEntity(
                cn.chumenta.crosswind.registry.CrosswindRegistry.RAT_BE.get(), pos, state);
    }

    @Override
    public float getGeneratedSpeed() {
        return this.generatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        this.lastCapacityProvided = RatAirspeed.STRESS_PER_RPM;
        return RatAirspeed.STRESS_PER_RPM;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        final ServerLevel serverLevel = (ServerLevel) this.level;
        this.airspeedMps = this.airspeedWindow.push(this.sampleAxialAirspeed(serverLevel));

        final float target = RatAirspeed.rpmFor(this.airspeedMps);
        if (target != this.generatedSpeed) {
            this.generatedSpeed = target;
            this.updateGeneratedRotation();
        }

        final long gameTime = serverLevel.getGameTime();
        if (this.lastSyncTick == Long.MIN_VALUE
                || gameTime - this.lastSyncTick >= AIRSPEED_SYNC_INTERVAL) {
            this.lastSyncTick = gameTime;
            this.sendData();
        }
    }

    private double sampleAxialAirspeed(final ServerLevel level) {
        final BlockPos pos = this.worldPosition;
        final double centerX = pos.getX() + 0.5D;
        final double centerY = pos.getY() + 0.5D;
        final double centerZ = pos.getZ() + 0.5D;
        this.localCenter.set(centerX, centerY, centerZ);

        final Direction facing = this.getBlockState().getValue(RatBlock.HORIZONTAL_FACING);
        this.localNormal.set(facing.getStepX(), 0.0D, facing.getStepZ());

        this.centerWorld.set(this.localCenter);
        this.worldNormal.set(this.localNormal);
        this.structureVelocity.set(0.0D, 0.0D, 0.0D);

        try {
            final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
            if (subLevel != null && !subLevel.isRemoved()) {
                final Pose3dc pose = subLevel.logicalPose();
                pose.transformPosition(this.localCenter, this.centerWorld);
                pose.transformNormal(this.localNormal, this.worldNormal).normalize();
                Sable.HELPER.getVelocity(level, subLevel, this.localCenter, this.structureVelocity);
            }
        } catch (final Throwable ignored) {
            this.structureVelocity.set(0.0D, 0.0D, 0.0D);
        }

        final WindSample wind = Crosswind.windService().sample(level,
                this.centerWorld.x, this.centerWorld.y, this.centerWorld.z,
                WindSettings.fromConfig());

        return (this.structureVelocity.x - wind.velocity().x) * this.worldNormal.x
                + (this.structureVelocity.y - wind.velocity().y) * this.worldNormal.y
                + (this.structureVelocity.z - wind.velocity().z) * this.worldNormal.z;
    }

    public double airspeedMps() {
        return this.airspeedMps;
    }

    public cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle fanAngle() {
        return this.fanAngle;
    }

    public cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity.RenderAngle shaftAngle() {
        return this.shaftAngle;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        tooltip.add(Component.literal("     ")
                .append(Component.translatable("block.crosswind.rat"))
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(line("gui.crosswind.goggle.airspeed",
                String.format(Locale.ROOT, "%.1f m/s", this.airspeedMps)));
        tooltip.add(line("gui.crosswind.goggle.rpm",
                String.format(Locale.ROOT, "%.0f RPM", (double) this.generatedSpeed)));

        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    private static MutableComponent line(final String key, final String value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(0.5D);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries,
                         final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("GeneratedSpeed", this.generatedSpeed);
        tag.putDouble("Airspeed", this.airspeedMps);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries,
                        final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.generatedSpeed = tag.getFloat("GeneratedSpeed");
        this.airspeedMps = tag.getDouble("Airspeed");
    }

    @Override
    public void remove() {
        this.airspeedWindow.clear();
        this.fanAngle.reset();
        this.shaftAngle.reset();
        super.remove();
    }
}
