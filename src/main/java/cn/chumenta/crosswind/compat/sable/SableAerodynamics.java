package cn.chumenta.crosswind.compat.sable;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableAerodynamics {
    private SableAerodynamics() {
    }

    public static Vector3d providerForce(final Vector3dc normal, final Vector3dc blockCenter,
                                         final Pose3dc pose, final net.minecraft.world.level.Level level,
                                         final Vector3dc localVelo, final double pressure,
                                         final double timeStep,
                                         final double parallelDrag, final double directionlessDrag,
                                         final double lift,
                                         final Vector3d outForce, final Vector3d tmpDrag,
                                         final Vector3d tmpVec) {
        final double pdt = pressure * timeStep;

        outForce.set(0.0D, 0.0D, 0.0D);

        if (parallelDrag > 0.0D) {
            final double dragStrength = normal.dot(localVelo) * parallelDrag * pdt;
            normal.mul(dragStrength, tmpDrag);
            outForce.add(tmpDrag);
        } else {
            tmpDrag.set(0.0D, 0.0D, 0.0D);
        }

        if (directionlessDrag > 0.0D) {
            final double dragStrength = directionlessDrag * pdt;
            localVelo.mul(dragStrength, tmpVec);
            outForce.add(tmpVec);
        }

        if (lift > 0.0D) {
            localVelo.sub(tmpDrag, tmpVec);
            final double liftStrength = tmpVec.length() * lift * pdt;
            normal.mul(liftStrength, tmpVec);
            outForce.add(tmpVec);
        }

        return outForce;
    }

    public static Vector3d localAirflowVelocity(final Vector3dc blockCenter, final Pose3dc pose,
                                                final Vector3dc linearVelocity,
                                                final Vector3dc angularVelocity,
                                                final Vector3d outVelo, final Vector3d tmp) {
        pose.transformPosition(blockCenter, tmp).sub(pose.position());

        angularVelocity.cross(tmp, outVelo);
        outVelo.add(linearVelocity);

        pose.transformNormalInverse(outVelo);
        return outVelo;
    }

    public static Vector3d airflowWithWind(final Vector3dc localVelo, final Vector3dc windLocal,
                                           final Vector3d dest) {
        return localVelo.sub(windLocal, dest);
    }

    public static Vector3d forceForProvider(final BlockSubLevelLiftProvider provider,
                                            final BlockSubLevelLiftProvider.LiftProviderContext ctx,
                                            final Pose3dc pose,
                                            final net.minecraft.world.level.Level level,
                                            final Vector3dc linearVelocity,
                                            final Vector3dc angularVelocity,
                                            final Vector3dc windLocalOrNull,
                                            final double timeStep,
                                            final SableAerodynamics.Scratch scratch) {
        scratch.normal.set(ctx.dir().x, ctx.dir().y, ctx.dir().z);
        scratch.blockCenter.set(ctx.pos().getX() + 0.5D, ctx.pos().getY() + 0.5D,
                ctx.pos().getZ() + 0.5D);

        localAirflowVelocity(scratch.blockCenter, pose, linearVelocity, angularVelocity,
                scratch.velo, scratch.tmpVec);

        if (windLocalOrNull != null) {
            airflowWithWind(scratch.velo, windLocalOrNull, scratch.velo);
        }

        pose.transformPosition(scratch.blockCenter, scratch.tmpVec);
        final double pressure = dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData
                .getAirPressure(level, scratch.tmpVec);

        return providerForce(scratch.normal, scratch.blockCenter, pose, level, scratch.velo,
                pressure, timeStep,
                provider.sable$getParallelDragScalar(),
                provider.sable$getDirectionlessDragScalar(),
                provider.sable$getLiftScalar(),
                scratch.force, scratch.drag, scratch.tmpVec2);
    }

    public static final class Scratch {
        public final Vector3d normal = new Vector3d();
        public final Vector3d blockCenter = new Vector3d();
        public final Vector3d velo = new Vector3d();
        public final Vector3d windLocal = new Vector3d();
        public final Vector3d veloWithWind = new Vector3d();
        public final Vector3d force = new Vector3d();
        public final Vector3d forceWind = new Vector3d();
        public final Vector3d drag = new Vector3d();
        public final Vector3d drag2 = new Vector3d();
        public final Vector3d tmpVec = new Vector3d();
        public final Vector3d tmpVec2 = new Vector3d();
        public final Vector3d delta = new Vector3d();
    }
}
