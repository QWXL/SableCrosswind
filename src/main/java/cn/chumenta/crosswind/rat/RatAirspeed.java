package cn.chumenta.crosswind.rat;

public final class RatAirspeed {
    public static final double MIN_AIRSPEED_MPS = 10.0D;

    public static final double FULL_AIRSPEED_MPS = 150.0D;

    public static final float MAX_RPM = 256.0F;

    public static final float STRESS_PER_RPM = 4.0F;

    public static final int WINDOW_TICKS = 20;

    private RatAirspeed() {
    }

    public static float rpmFor(final double meanAxialAirspeedMps) {
        final double magnitude = Math.abs(meanAxialAirspeedMps);
        if (magnitude <= MIN_AIRSPEED_MPS) {
            return 0.0F;
        }
        final double ratio = Math.min(1.0D,
                (magnitude - MIN_AIRSPEED_MPS) / (FULL_AIRSPEED_MPS - MIN_AIRSPEED_MPS));
        final float rpm = (float) Math.ceil(MAX_RPM * ratio);
        return meanAxialAirspeedMps < 0.0D ? -rpm : rpm;
    }

    public static float fanRpmFor(final double axialAirspeedMps) {
        final float magnitude = (float) (MAX_RPM * Math.abs(axialAirspeedMps) / FULL_AIRSPEED_MPS);
        return axialAirspeedMps < 0.0D ? -magnitude : magnitude;
    }

    public static final class Window {
        private final double[] samples;

        private int cursor;

        private int filled;

        private double sum;

        public Window(final int windowTicks) {
            this.samples = new double[Math.max(1, windowTicks)];
        }

        public double push(final double value) {
            if (!Double.isFinite(value)) {
                return this.average();
            }

            if (this.filled == this.samples.length) {
                this.sum -= this.samples[this.cursor];
            } else {
                this.filled++;
            }
            this.samples[this.cursor] = value;
            this.sum += value;
            this.cursor = (this.cursor + 1) % this.samples.length;

            return this.average();
        }

        public double average() {
            return this.filled == 0 ? 0.0D : this.sum / this.filled;
        }

        public void clear() {
            java.util.Arrays.fill(this.samples, 0.0D);
            this.cursor = 0;
            this.filled = 0;
            this.sum = 0.0D;
        }
    }
}
