package cn.chumenta.crosswind.compat.sable;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class ForceMath {
    private ForceMath() {
    }

    public static void force(final Vector3dc normal, final Vector3dc localVelo,
                             final double forceCoefficient,
                             final double parallelDrag, final double directionlessDrag,
                             final double lift,
                             final Vector3d out, final Vector3d dragScratch, final Vector3d tmpScratch) {
        out.set(0.0D, 0.0D, 0.0D);

        if (parallelDrag > 0.0D) {
            final double strength = normal.dot(localVelo) * parallelDrag * forceCoefficient;
            normal.mul(strength, dragScratch);
            out.add(dragScratch);
        } else {
            dragScratch.set(0.0D, 0.0D, 0.0D);
        }

        if (directionlessDrag > 0.0D) {
            localVelo.mul(directionlessDrag * forceCoefficient, tmpScratch);
            out.add(tmpScratch);
        }

        if (lift > 0.0D) {
            localVelo.sub(dragScratch, tmpScratch);
            final double strength = tmpScratch.length() * lift * forceCoefficient;
            normal.mul(strength, tmpScratch);
            out.add(tmpScratch);
        }
    }

    public static Vector3d windCorrection(final Vector3dc normal, final Vector3dc localVelo,
                                          final Vector3dc windLocalNull,
                                          final double forceCoefficient,
                                          final double parallelDrag, final double directionlessDrag,
                                          final double lift,
                                          final Vector3d out,
                                          final Vector3d baseForce, final Vector3d windForce,
                                          final Vector3d dragA, final Vector3d dragB,
                                          final Vector3d tmpA, final Vector3d tmpB,
                                          final Vector3d veloWithWind) {
        if (windLocalNull == null
                || (windLocalNull.x() == 0.0D && windLocalNull.y() == 0.0D
                    && windLocalNull.z() == 0.0D)) {
            return out.set(0.0D, 0.0D, 0.0D);
        }

        force(normal, localVelo, forceCoefficient,
                parallelDrag, directionlessDrag, lift, baseForce, dragA, tmpA);

        localVelo.sub(windLocalNull, veloWithWind);
        force(normal, veloWithWind, forceCoefficient,
                parallelDrag, directionlessDrag, lift, windForce, dragB, tmpB);

        return windForce.sub(baseForce, out);
    }
}
