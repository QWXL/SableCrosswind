package cn.chumenta.crosswind.compat.sable;

public final class WindForceMath {
    public static org.joml.Vector3d driveVelocity(final double windX, final double windY,
                                                  final double windZ,
                                                  final double bodyX, final double bodyY,
                                                  final double bodyZ,
                                                  final org.joml.Vector3d dest) {
        final double windSpeed = Math.sqrt(windX * windX + windY * windY + windZ * windZ);
        if (windSpeed < 1.0E-9D) {
            return dest.set(0.0D, 0.0D, 0.0D);
        }
        final double ux = windX / windSpeed;
        final double uy = windY / windSpeed;
        final double uz = windZ / windSpeed;

        final double along = bodyX * ux + bodyY * uy + bodyZ * uz;
        return dest.set(windX - along * ux, windY - along * uy, windZ - along * uz);
    }

    private WindForceMath() {
    }

    public static double smoothMin(final double x, final double limit) {
        if (limit <= 0.0D) {
            return x;
        }
        if (x <= 0.0D) {
            return 0.0D;
        }
        final double u = x / limit;
        if (u <= 0.5D) {
            return x;
        }
        if (u < 1.0D) {
            final double t = 2.0D - 2.0D * u;
            return limit * (0.25D + 0.75D * (1.0D - t * t));
        }

        return limit * (1.5D - 0.5D * Math.exp(-3.0D * (u - 1.0D)));
    }
}
