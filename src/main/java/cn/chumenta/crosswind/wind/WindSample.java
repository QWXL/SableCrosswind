package cn.chumenta.crosswind.wind;

import net.minecraft.world.phys.Vec3;

public record WindSample(
        Vec3 velocity,
        double gust,
        double temperatureC,
        double precipitation,
        double pressure,
        boolean terrainApplied,

        boolean climateAvailable
) {
    public static final double KNOTS_PER_MPS = 1.9438444924406046D;

    public static final WindSample CALM = new WindSample(Vec3.ZERO, 0.0D, 0.0D, 0.0D, 0.0D, false, false);

    public double fromDegrees() {
        return WindMath.wrapDegrees(this.toDegrees() + 180.0D);
    }

    public double toDegrees() {
        final double x = this.velocity.x;
        final double z = this.velocity.z;
        if (x == 0.0D && z == 0.0D) {
            return 0.0D;
        }

        return WindMath.wrapDegrees(Math.toDegrees(Math.atan2(x, -z)));
    }

    public double speed() {
        return this.velocity.horizontalDistance();
    }

    public double speedKnots() {
        return this.speed() * KNOTS_PER_MPS;
    }

    public double gustComponent() {
        return this.gust;
    }

    public double peakSpeed() {
        return GustMath.peakSpeed(this.speed(), this.gust);
    }

    public double gustKnots() {
        return this.gust * KNOTS_PER_MPS;
    }

    public boolean isCalm() {
        return this.speed() < 1.0E-6D;
    }

    public static final double DIRECTION_FLOOR_MPS = 0.1D;

    public boolean hasDirection() {
        return this.speed() >= DIRECTION_FLOOR_MPS;
    }
}
