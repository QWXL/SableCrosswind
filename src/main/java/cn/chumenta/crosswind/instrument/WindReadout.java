package cn.chumenta.crosswind.instrument;

import cn.chumenta.crosswind.wind.WindMath;
import cn.chumenta.crosswind.wind.WindSample;

public record WindReadout(
        float toDegrees,
        float fromDegrees,
        float speedMps,
        float gustMps,
        float cupSpeedMps,
        float temperatureC,
        float precipitation,

        boolean climateAvailable
) {
    public static final WindReadout NONE =
            new WindReadout(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

    public static WindReadout of(final WindSample sample, final float cupSpeedMps) {
        return new WindReadout(
                (float) sample.toDegrees(),
                (float) sample.fromDegrees(),
                (float) sample.speed(),
                (float) sample.gustComponent(),
                cupSpeedMps,
                (float) sample.temperatureC(),
                (float) sample.precipitation(),
                sample.climateAvailable());
    }

    public record Fields(boolean fromDirection, boolean toDirection, boolean speed, boolean gust,
                         boolean temperature, boolean precipitation) {
        public static final Fields DIRECTIONS_ONLY =
                new Fields(true, true, false, false, false, false);

        public static final Fields SPEED_ONLY =
                new Fields(false, false, true, false, false, false);

        public static final Fields ALL =
                new Fields(true, true, true, true, true, true);

        public boolean any() {
            return this.fromDirection || this.toDirection || this.speed || this.gust
                    || this.temperature || this.precipitation;
        }
    }

    public double speedKnots() {
        return this.speedMps * WindSample.KNOTS_PER_MPS;
    }

    public double cupSpeedKnots() {
        return this.cupSpeedMps * WindSample.KNOTS_PER_MPS;
    }

    public double peakSpeedMps() {
        return cn.chumenta.crosswind.wind.GustMath.peakSpeed(this.speedMps, this.gustMps);
    }

    public boolean directionAvailable() {
        return this.speedMps >= WindSample.DIRECTION_FLOOR_MPS;
    }

    public double peakSpeedKnots() {
        return this.peakSpeedMps() * WindSample.KNOTS_PER_MPS;
    }

    public double gustKnots() {
        return this.gustMps * WindSample.KNOTS_PER_MPS;
    }

    public boolean differsFrom(final WindReadout other, final double angleToleranceDeg,
                               final double speedToleranceMps, final double tempToleranceC,
                               final double precipTolerance) {
        if (Math.abs(WindMath.deltaDegrees(this.toDegrees, other.toDegrees)) > angleToleranceDeg) {
            return true;
        }
        if (Math.abs(this.speedMps - other.speedMps) > speedToleranceMps) {
            return true;
        }
        if (Math.abs(this.gustMps - other.gustMps) > speedToleranceMps) {
            return true;
        }
        if (Math.abs(this.temperatureC - other.temperatureC) > tempToleranceC) {
            return true;
        }
        return Math.abs(this.precipitation - other.precipitation) > precipTolerance;
    }
}
