package cn.chumenta.crosswind.compat.sable;

import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SubLevelWindForces {
    public static final double GRAVITY_MPS2 = 11.0D;

    public static final double MASS_REFERENCE_KPG = 1.0D;

    static double smoothMin(final double x, final double limit) {
        return WindForceMath.smoothMin(x, limit);
    }

    private SubLevelWindForces() {
    }

    static final class Scratch {
        final SableAerodynamics.Scratch aero = new SableAerodynamics.Scratch();
        final Vector3d linearVelocity = new Vector3d();
        final Vector3d angularVelocity = new Vector3d();
        final Vector3d windWorld = new Vector3d();
        final Vector3d windLocal = new Vector3d();
        final Vector3d delta = new Vector3d();
        final Vector3d lever = new Vector3d();
        final Vector3d torque = new Vector3d();
        final Vector3d centerOfMass = new Vector3d();
        final Vector3d size = new Vector3d();

        final Vector3d relVelo = new Vector3d();

        final Vector3d tmpRelVelo = new Vector3d();
        final Vector3d boxForce = new Vector3d();

        double massForClamp;

        boolean approachClamped;

        double observedRelSpeed;

        double diagMass;

        double diagBodySpeed;

        double diagRawForce;

        double diagArea;

        double diagSailForce;

        int diagSailCount;

        double diagForceX;
        double diagForceY;
        double diagForceZ;

        final Vector3d diagPos = new Vector3d();
        final Vector3d diagVel = new Vector3d();
        final Vector3d diagAngVel = new Vector3d();

        final Vector3d diagRawForceVec = new Vector3d();

        boolean diagAccelClamped;

        double diagPressureScale;

        double diagCoarseForce;

        final Vector3d diagSubmittedLocal = new Vector3d();

        final Vector3d diagRawForceVec2 = new Vector3d();
        final Vector3d diagRawTorque = new Vector3d();

        final Vector3d submitLocalForce = new Vector3d();
        final Vector3d submitLocalTorque = new Vector3d();

        final Vector3d diagTickImpulse = new Vector3d();

        final Vector3d diagTickStartVel = new Vector3d();

        double diagTickStartSpeed;

        final Vector3d tmpDbg = new Vector3d();

        final Vector3d diagTickForce = new Vector3d();

        int diagTickSamples;
    }

    public static double apply(final ServerLevel level, final ServerSubLevel subLevel,
                               final RigidBodyHandle handle, final double timeStep,
                               final WindSample wind, final WindSettings settings,
                               final ForceGroup group, final Scratch scratch) {
        if (wind.isCalm()) {
            return 0.0D;
        }

        final var massData = subLevel.getMassTracker();
        final Vector3dc com = massData.getCenterOfMass();
        if (com == null || massData.isInvalid()) {
            return 0.0D;
        }
        scratch.centerOfMass.set(com);

        scratch.approachClamped = false;
        scratch.observedRelSpeed = 0.0D;
        scratch.diagSailForce = 0.0D;
        scratch.diagSailCount = 0;
        scratch.diagCoarseForce = 0.0D;

        final Pose3dc pose = subLevel.logicalPose();
        handle.getLinearVelocity(scratch.linearVelocity);
        handle.getAngularVelocity(scratch.angularVelocity);

        final Vector3d totalForce = scratch.delta;
        final Vector3d totalTorque = scratch.torque;
        totalForce.set(0.0D, 0.0D, 0.0D);
        totalTorque.set(0.0D, 0.0D, 0.0D);

        final double pressure = dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData
                .getAirPressure(level, pose.position());
        final double pressureScale = cn.chumenta.crosswind.wind.PressureMath.clampedForceScale(
                pressure, settings.pressureReference(), settings.pressureExponent(),
                settings.pressureMinFactor(), settings.pressureMaxFactor());

        final double densityScale = pressureScale * cn.chumenta.crosswind.wind.DensityMath
                .temperatureRatio(wind.temperatureC());

        final int providers = accumulateProviders(subLevel, pose, wind, pressure, densityScale,
                scratch, totalForce, totalTorque);

        scratch.massForClamp = massData.getMass();
        final boolean usedBoxDrag = settings.sableCoarseWind()
                && applyCoarseDrag(subLevel, pose, densityScale, wind, settings, scratch, totalForce,
                        timeStep, scratch.massForClamp);

        scratch.diagCoarseForce = usedBoxDrag ? scratch.boxForce.length() : 0.0D;
        if (!usedBoxDrag && providers == 0) {
            return 0.0D;
        }

        scratch.diagRawForce = totalForce.length();
        scratch.diagRawForceVec.set(totalForce);
        scratch.diagMass = massData.getMass();
        scratch.diagBodySpeed = scratch.linearVelocity.length();
        scratch.diagPos.set(pose.position());
        scratch.diagVel.set(scratch.linearVelocity);
        scratch.diagAngVel.set(scratch.angularVelocity);

        scratch.diagPressureScale = densityScale;
        if (!settings.sableCoarseWind()) {
            scratch.diagArea = 0.0D;
        }

        final double massExp = settings.sableMassExponent();
        final double forceMass = massData.getMass();
        if (massExp > 0.0D && forceMass > 0.0D) {
            final double massFactor = Math.pow(MASS_REFERENCE_KPG / forceMass, massExp);
            totalForce.mul(massFactor);
            totalTorque.mul(massFactor);
        }

        final double multiplier = settings.sableForceMultiplier();
        if (multiplier != 1.0D) {
            totalForce.mul(multiplier);
            totalTorque.mul(multiplier);
        }

        boolean clamped = false;
        final double maxAccelG = settings.sableMaxAccelerationG();
        if (maxAccelG > 0.0D) {
            final double accelMass = massData.getMass();
            if (accelMass > 0.0D) {
                final double mag = totalForce.length();
                final double softMag = smoothMin(mag, accelMass * maxAccelG * GRAVITY_MPS2);
                if (softMag < mag) {
                    final double scale = softMag / mag;
                    totalForce.mul(scale);
                    totalTorque.mul(scale);
                    clamped = true;
                }
            }
        }
        scratch.diagAccelClamped = clamped;

        final double cap = settings.sableMaxForcePerSubstep();
        final double forceMag = totalForce.length();
        double truncation = 1.0D;
        if (cap > 0.0D && forceMag > cap) {
            truncation = cap / forceMag;
            totalForce.mul(truncation);
            totalTorque.mul(truncation);
        }

        totalForce.mul(timeStep);
        totalTorque.mul(timeStep);

        if (scratch.diagTickSamples == 0) {
            scratch.diagTickStartVel.set(scratch.linearVelocity);
            scratch.diagTickStartSpeed = scratch.linearVelocity.length();
        }
        scratch.diagTickForce.add(totalForce);
        scratch.diagTickSamples++;

        final QueuedForceGroup queued = subLevel.getOrCreateQueuedForceGroup(group);

        final Vector3d worldForce = scratch.diagRawForceVec2;
        worldForce.set(totalForce);
        final Vector3d worldTorque = scratch.diagRawTorque;
        worldTorque.set(totalTorque);

        final Vector3d localForce = scratch.submitLocalForce;
        final Vector3d localTorque = scratch.submitLocalTorque;
        pose.transformNormalInverse(worldForce, localForce);
        pose.transformNormalInverse(worldTorque, localTorque);

        queued.getForceTotal().applyLinearAndAngularImpulse(localForce, localTorque);

        queued.recordPointForce(scratch.centerOfMass, localForce);

        scratch.diagTickImpulse.add(totalForce);

        scratch.diagForceX = totalForce.x;
        scratch.diagForceY = totalForce.y;
        scratch.diagForceZ = totalForce.z;

        return (truncation < 1.0D || clamped) ? -forceMag : forceMag;
    }

    public static double accelerationG(final double forceMag, final double mass) {
        if (mass <= 0.0D) {
            return 0.0D;
        }
        return forceMag / mass / GRAVITY_MPS2;
    }

    private static int accumulateProviders(final ServerSubLevel subLevel, final Pose3dc pose,
                                           final WindSample wind,
                                           final double pressure,
                                           final double pressureScale,
                                           final Scratch scratch,
                                           final Vector3d totalForce, final Vector3d totalTorque) {
        final var plot = subLevel.getPlot();
        if (plot == null) {
            return 0;
        }
        final var liftProviders = plot.getLiftProviders();
        if (liftProviders == null || liftProviders.isEmpty()) {
            return 0;
        }

        scratch.windWorld.set(wind.velocity().x, wind.velocity().y, wind.velocity().z);
        pose.transformNormalInverse(scratch.windWorld, scratch.windLocal);

        final double coefficient = pressure * pressureScale;
        int count = 0;

        for (final BlockSubLevelLiftProvider.LiftProviderContext ctx : liftProviders) {
            if (!(ctx.state().getBlock() instanceof final BlockSubLevelLiftProvider provider)) {
                continue;
            }

            scratch.aero.normal.set(ctx.dir().x, ctx.dir().y, ctx.dir().z);
            scratch.aero.blockCenter.set(ctx.pos().getX() + 0.5D, ctx.pos().getY() + 0.5D,
                    ctx.pos().getZ() + 0.5D);

            SableAerodynamics.localAirflowVelocity(scratch.aero.blockCenter, pose,
                    scratch.linearVelocity, scratch.angularVelocity,
                    scratch.aero.velo, scratch.aero.tmpVec);
            final Vector3d extra = ForceMath.windCorrection(
                    scratch.aero.normal, scratch.aero.velo, scratch.windLocal,
                    coefficient,
                    provider.sable$getParallelDragScalar(),
                    provider.sable$getDirectionlessDragScalar(),
                    provider.sable$getLiftScalar(),
                    scratch.aero.delta,
                    scratch.aero.force, scratch.aero.forceWind,
                    scratch.aero.drag, scratch.aero.drag2,
                    scratch.aero.tmpVec, scratch.aero.tmpVec2,
                    scratch.aero.veloWithWind);

            extra.negate();

            if (extra.lengthSquared() < 1.0E-18D) {
                continue;
            }

            scratch.aero.blockCenter.sub(scratch.centerOfMass, scratch.lever);
            scratch.lever.cross(extra, scratch.aero.tmpVec2);

            totalForce.add(extra);
            totalTorque.sub(scratch.aero.tmpVec2);
            scratch.diagSailForce += extra.length();
            scratch.diagSailCount++;
            count++;
        }
        return count;
    }

    private static boolean applyCoarseDrag(final ServerSubLevel subLevel, final Pose3dc pose,
                                           final double pressureScale, final WindSample wind,
                                           final WindSettings settings, final Scratch scratch,
                                           final Vector3d totalForce,
                                           final double timeStep, final double mass) {
        final BoundingBox3dc box = subLevel.boundingBox();
        if (box == null) {
            return false;
        }
        box.size(scratch.size);
        if (scratch.size.x <= 0.0D || scratch.size.y <= 0.0D || scratch.size.z <= 0.0D) {
            return false;
        }

        final Vector3d windVec = scratch.relVelo;
        windVec.set(wind.velocity().x, wind.velocity().y, wind.velocity().z);
        final double windSpeed = windVec.length();
        if (windSpeed < 1.0E-3D) {
            return false;
        }
        scratch.observedRelSpeed = windSpeed;

        final double unitX = windVec.x / windSpeed;
        final double unitY = windVec.y / windSpeed;
        final double unitZ = windVec.z / windSpeed;
        final double alongWind = scratch.linearVelocity.x * unitX
                + scratch.linearVelocity.y * unitY
                + scratch.linearVelocity.z * unitZ;

        final Vector3d drive = scratch.tmpRelVelo;
        drive.set(windVec.x - alongWind * unitX,
                windVec.y - alongWind * unitY,
                windVec.z - alongWind * unitZ);
        final double driveSpeed = drive.length();
        if (driveSpeed < 1.0E-3D) {
            scratch.diagArea = 0.0D;
            return false;
        }

        final double nx = Math.abs(unitX);
        final double ny = Math.abs(unitY);
        final double nz = Math.abs(unitZ);
        final double area = nx * scratch.size.y * scratch.size.z
                + ny * scratch.size.x * scratch.size.z
                + nz * scratch.size.x * scratch.size.y;
        if (area <= 1.0E-9D) {
            return false;
        }
        scratch.diagArea = area;

        final double density = settings.airDensityReference() * pressureScale;
        final double k = 0.5D * density * settings.windDragCoefficient() * area * driveSpeed;
        scratch.boxForce.set(drive).mul(k);

        final double beta = settings.sableForceExponent();
        if (beta != 1.0D && driveSpeed > 1.0E-6D) {
            scratch.boxForce.mul(Math.pow(driveSpeed, beta - 1.0D));
        }

        totalForce.add(scratch.boxForce);
        return true;
    }
}
