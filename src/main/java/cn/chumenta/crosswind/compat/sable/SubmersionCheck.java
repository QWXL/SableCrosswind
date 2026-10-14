package cn.chumenta.crosswind.compat.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SubmersionCheck {
    private SubmersionCheck() {
    }

    public static boolean entitySubmerged(final Entity entity) {
        try {
            final Level level = entity.level();

            if (entity.isEyeInFluid(FluidTags.WATER)) {
                return true;
            }

            return isWaterAt(level, entity.getX(), entity.getEyeY(), entity.getZ());
        } catch (final Throwable t) {
            return false;
        }
    }

    public static boolean subLevelSubmerged(final Level level, final Object pose,
                                            final Vector3dc plotCom, final Vector3d dest) {
        try {
            if (!(pose instanceof final dev.ryanhcode.sable.companion.math.Pose3dc p)) {
                return false;
            }

            p.transformPosition(plotCom, dest);
            return isWaterAt(level, dest.x, dest.y, dest.z);
        } catch (final Throwable t) {
            return false;
        }
    }

    public static boolean isWaterAt(final Level level, final double x, final double y, final double z) {
        if (level == null) {
            return false;
        }
        final BlockPos pos = BlockPos.containing(x, y, z);

        if (!level.isLoaded(pos)) {
            return false;
        }
        final FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER);
    }
}
